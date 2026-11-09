package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.DevolucionDeParcial;
import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.ParcialesPendientes;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.AvanzarEtapa;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.DevolverParciales;
import ec.mdd.validacionocs.puertos.entrada.EnviarARevision;
import ec.mdd.validacionocs.puertos.entrada.EnviarParcial;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.DistributivoActual;
import ec.mdd.validacionocs.puertos.salida.Distributivo.ParcialEnviadoEnGeneracion;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El flujo del distributivo (F12-H02): consultarlo, enviar el parcial de una carrera y el avance
 * genérico; desde F10-H03, enviar el consolidado a revisión, sin notificar (D-97); desde F12-H05,
 * devolver parciales desde EN_CONSOLIDACION, sin notificar (D-98). Las reglas son de {@link
 * ProcesoAprobacion}; este servicio coordina con Generación y con la bitácora.
 *
 * <p>Enviar son dos transacciones, una por servicio (principio V, research §2): primero Generación
 * marca el parcial y calcula su hash, en la suya; después se registra la transición y, si era el
 * último, el paso de etapa, en la de Validación OCS, con su entrada en la bitácora (D-95). Si la
 * segunda falla, reenviar la completa con el mismo hash: Generación es idempotente. Antes de tocar
 * Generación, {@link ProcesoAprobacion#validarEnvio} rechaza lo que no cabe (FR-013).
 */
public final class FlujoDelDistributivoServicio
        implements ConsultarFlujo, EnviarParcial, AvanzarEtapa, EnviarARevision, DevolverParciales {

    private final Distributivo distributivo;
    private final RepositorioProceso procesos;
    private final EtapasAlCrearse etapas;
    private final DatosDelPeriodo datos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos, las etapas al crearse y el reloj. */
    public FlujoDelDistributivoServicio(
            Distributivo distributivo,
            RepositorioProceso procesos,
            EtapasAlCrearse etapas,
            DatosDelPeriodo datos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.distributivo = distributivo;
        this.procesos = procesos;
        this.etapas = etapas;
        this.datos = datos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public FlujoDelDistributivo flujo(String periodo, Solicitante quien) {
        DistributivoActual actual = actual(periodo);
        ProcesoAprobacion proceso = transaccion.ejecutar(() -> proceso(periodo, actual, false));
        return vista(proceso, actual, quien);
    }

    @Override
    public FlujoDelDistributivo enviar(String periodo, String carrera, Solicitante quien) {
        exigirAbierto(periodo);
        DistributivoActual antes = actual(periodo);
        transaccion.ejecutar(() -> proceso(periodo, antes, false)).validarEnvio(carrera, quien);
        ParcialEnviadoEnGeneracion marcado = distributivo.marcarEnviado(periodo, carrera);
        String hashDelDistributivo = distributivo.hash(periodo);
        DistributivoActual despues = actual(periodo);
        Instant ahora = reloj.instant();
        ProcesoAprobacion proceso =
                transaccion.ejecutar(
                        () -> {
                            ProcesoAprobacion bloqueado = proceso(periodo, despues, true);
                            List<Transicion> nuevas =
                                    bloqueado.enviarParcial(
                                            carrera,
                                            quien,
                                            marcado.version(),
                                            marcado.hash(),
                                            hashDelDistributivo,
                                            ahora);
                            procesos.guardar(bloqueado, nuevas);
                            nuevas.forEach(
                                    una ->
                                            registro.registrar(
                                                    cambio(periodo, una, Optional.empty())));
                            return bloqueado;
                        });
        return vista(proceso, despues, quien);
    }

    @Override
    public FlujoDelDistributivo avanzar(String periodo, Solicitante quien) {
        exigirAbierto(periodo);
        DistributivoActual actual = actual(periodo);
        String hashDelDistributivo = distributivo.hash(periodo);
        Instant ahora = reloj.instant();
        ProcesoAprobacion proceso =
                transaccion.ejecutar(
                        () -> {
                            ProcesoAprobacion bloqueado = proceso(periodo, actual, true);
                            List<Transicion> nuevas =
                                    bloqueado.avanzar(
                                            quien, actual.version(), hashDelDistributivo, ahora);
                            procesos.guardar(bloqueado, nuevas);
                            nuevas.forEach(
                                    una ->
                                            registro.registrar(
                                                    cambio(periodo, una, Optional.empty())));
                            return bloqueado;
                        });
        return vista(proceso, actual, quien);
    }

    // F10-H03, research §2: primero lo que no necesita a Generación; después las condiciones, fuera
    // de una transacción porque el panel llama a Datos (D-83), y en cada envío (FR-008); al final,
    // con el proceso bloqueado, el agregado vuelve a comprobar y registra.
    @Override
    public FlujoDelDistributivo enviarARevision(String periodo, Solicitante quien) {
        exigirAbierto(periodo);
        DistributivoActual actual = actual(periodo);
        transaccion.ejecutar(() -> proceso(periodo, actual, false)).validarRevision(quien);
        // F12-H05, research §2: una devolución que Generación marcó y Aprobación no registró deja
        // el parcial editable; no pasa a revisión hasta que se complete.
        if (actual.enviados().containsValue(false)) {
            throw new ParcialesPendientes();
        }
        CondicionesDeRevision condiciones = distributivo.condicionesDeRevision(periodo);
        String hashDelDistributivo = distributivo.hash(periodo);
        ProcesoAprobacion proceso =
                transaccion.ejecutar(
                        () -> {
                            ProcesoAprobacion bloqueado = proceso(periodo, actual, true);
                            // #215, research §2: el momento, ya con el bloqueo. Si el envío esperó
                            // a una operación de Generación sobre el consolidado, queda después.
                            Instant ahora = reloj.instant();
                            List<Transicion> nuevas =
                                    bloqueado.enviarARevision(
                                            quien,
                                            condiciones,
                                            actual.version(),
                                            hashDelDistributivo,
                                            ahora);
                            procesos.guardar(bloqueado, nuevas);
                            nuevas.forEach(
                                    una ->
                                            registro.registrar(
                                                    cambio(periodo, una, condiciones.informe())));
                            return bloqueado;
                        });
        return vista(proceso, actual, quien);
    }

    // F12-H05, research §2: como enviar un parcial. Primero el dominio sin cambiar nada; después
    // Generación marca los parciales, todos o ninguno, en su transacción; al final, con el proceso
    // bloqueado, el agregado vuelve a comprobar y registra. Si la última falla, repetir la
    // devolución la completa: Generación es idempotente.
    @Override
    public FlujoDelDistributivo devolver(
            String periodo, List<DevolucionDeParcial> devoluciones, Solicitante quien) {
        exigirAbierto(periodo);
        DistributivoActual antes = actual(periodo);
        transaccion
                .ejecutar(() -> proceso(periodo, antes, false))
                .validarDevolucion(quien, devoluciones);
        int version =
                distributivo.marcarDevueltos(
                        periodo, devoluciones.stream().map(DevolucionDeParcial::carrera).toList());
        String hashDelDistributivo = distributivo.hash(periodo);
        DistributivoActual despues = actual(periodo);
        Instant ahora = reloj.instant();
        ProcesoAprobacion proceso =
                transaccion.ejecutar(
                        () -> {
                            ProcesoAprobacion bloqueado = proceso(periodo, despues, true);
                            List<Transicion> nuevas =
                                    bloqueado.devolver(
                                            quien,
                                            devoluciones,
                                            version,
                                            hashDelDistributivo,
                                            ahora);
                            procesos.guardar(bloqueado, nuevas);
                            nuevas.forEach(
                                    una ->
                                            registro.registrar(
                                                    cambio(periodo, una, Optional.empty())));
                            return bloqueado;
                        });
        return vista(proceso, despues, quien);
    }

    private DistributivoActual actual(String periodo) {
        return distributivo.actual(periodo).orElseThrow(SinDistributivo::new);
    }

    private void exigirAbierto(String periodo) {
        EstadoDelPeriodo estado = datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new);
        if (estado.cerrado()) {
            throw new PeriodoCerrado();
        }
    }

    // El proceso del distributivo actual: lo crea la primera vez, o si el guardado es de un
    // distributivo anterior (research §5); y suma los parciales que Generación agregó después.
    private ProcesoAprobacion proceso(String periodo, DistributivoActual actual, boolean bloquear) {
        Optional<ProcesoAprobacion> guardado =
                bloquear ? procesos.bloquear(periodo) : procesos.de(periodo);
        if (guardado.filter(uno -> uno.distributivoCreadoEn().equals(actual.creadoEn()))
                .isEmpty()) {
            ProcesoAprobacion nuevo =
                    ProcesoAprobacion.nuevo(
                            periodo,
                            etapas.de(periodo, actual.creadoEn()),
                            actual.creadoEn(),
                            actual.enviados().keySet());
            procesos.crear(nuevo, reloj.instant());
            return bloquear ? procesos.bloquear(periodo).orElseThrow() : nuevo;
        }
        ProcesoAprobacion proceso = guardado.get();
        if (!proceso.parciales().keySet().containsAll(actual.enviados().keySet())) {
            proceso.sumarParciales(actual.enviados().keySet());
            procesos.guardar(proceso, List.of());
        }
        return proceso;
    }

    // Una entrada por transición (D-95). Un paso desde EN_CONSOLIDACION solo viene del envío a
    // revisión, porque el avance genérico rechaza esa etapa: lleva su propia acción y la huella del
    // informe (F10-H03, research §5).
    private static CambioDeFlujo cambio(
            String periodo, Transicion transicion, Optional<String> informe) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", periodo);
        transicion.carrera().ifPresent(carrera -> detalle.put("carrera", carrera));
        detalle.put("version", transicion.version());
        detalle.put("hash", transicion.hash());
        informe.ifPresent(huella -> detalle.put("informe", huella));
        if (transicion.tipo() == TipoDeTransicion.ENVIO_PARCIAL) {
            return new CambioDeFlujo(
                    transicion.momento(),
                    transicion.quien(),
                    CambioDeFlujo.Accion.PARCIAL_ENVIADO,
                    detalle,
                    Map.of("estado", transicion.desde().name(), "parcial", "PENDIENTE"),
                    Map.of("estado", transicion.hacia().name(), "parcial", "ENVIADO"));
        }
        if (transicion.tipo() == TipoDeTransicion.DEVOLUCION) {
            Map<String, Object> devueltos = new LinkedHashMap<>();
            transicion
                    .observaciones()
                    .forEach((carrera, observacion) -> devueltos.put(carrera, observacion.texto()));
            detalle.put("devueltos", devueltos);
            return new CambioDeFlujo(
                    transicion.momento(),
                    transicion.quien(),
                    CambioDeFlujo.Accion.DISTRIBUTIVO_DEVUELTO,
                    detalle,
                    Map.of("estado", transicion.desde().name()),
                    Map.of("estado", transicion.hacia().name()));
        }
        return new CambioDeFlujo(
                transicion.momento(),
                transicion.quien(),
                transicion.desde() == TipoDeEtapa.EN_CONSOLIDACION
                        ? CambioDeFlujo.Accion.CONSOLIDADO_ENVIADO_A_REVISION
                        : CambioDeFlujo.Accion.ETAPA_AVANZADA,
                detalle,
                Map.of("estado", transicion.desde().name()),
                Map.of("estado", transicion.hacia().name()));
    }

    private static FlujoDelDistributivo vista(
            ProcesoAprobacion proceso, DistributivoActual actual, Solicitante quien) {
        List<FlujoDelDistributivo.Parcial> parciales =
                proceso.parciales().entrySet().stream()
                        .map(
                                parcial ->
                                        parcial(
                                                proceso,
                                                actual,
                                                parcial.getKey(),
                                                parcial.getValue()))
                        .toList();
        return new FlujoDelDistributivo(
                proceso.periodo(),
                proceso.estado(),
                proceso.nombreDeLaEtapa(),
                actual.version(),
                parciales,
                proceso.acciones(quien),
                proceso.transiciones());
    }

    // Enviado: con su transición registrada. Pendiente de confirmar: Generación lo marcó y la
    // transición todavía no se registró (research §2).
    private static FlujoDelDistributivo.Parcial parcial(
            ProcesoAprobacion proceso,
            DistributivoActual actual,
            String carrera,
            EstadoDeEnvio estado) {
        boolean enviado = estado == EstadoDeEnvio.ENVIADO;
        Optional<Transicion> envio = envioDe(proceso, carrera);
        return new FlujoDelDistributivo.Parcial(
                carrera,
                enviado,
                !enviado && actual.enviados().getOrDefault(carrera, false),
                envio.map(Transicion::quien),
                envio.map(Transicion::momento),
                estado == EstadoDeEnvio.DEVUELTO
                        ? devolucionDe(proceso, carrera)
                        : Optional.empty());
    }

    // La última devolución que incluye la carrera, con su observación (FR-015).
    private static Optional<FlujoDelDistributivo.Devolucion> devolucionDe(
            ProcesoAprobacion proceso, String carrera) {
        return proceso.transiciones().stream()
                .filter(una -> una.tipo() == TipoDeTransicion.DEVOLUCION)
                .filter(una -> una.observaciones().containsKey(carrera))
                .reduce((primera, ultima) -> ultima)
                .map(
                        una ->
                                new FlujoDelDistributivo.Devolucion(
                                        una.quien(),
                                        una.momento(),
                                        una.observaciones().get(carrera).texto()));
    }

    private static Optional<Transicion> envioDe(ProcesoAprobacion proceso, String carrera) {
        return proceso.transiciones().stream()
                .filter(una -> una.tipo() == TipoDeTransicion.ENVIO_PARCIAL)
                .filter(una -> una.carrera().filter(carrera::equals).isPresent())
                .reduce((primera, ultima) -> ultima);
    }
}
