package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CalculadoraAfinidad;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.Inconsistencia;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.MotorReglas;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.AfinidadInsuficiente;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.ProponerDocente;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.ResultadoDePropuesta;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDePropuesta;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Propone y quita docentes. El puntaje se recalcula aquí: la justificación no depende de lo que
 * diga la pantalla. Una regla dura no se levanta con una justificación, y RN-19 sin justificar
 * responde con una advertencia, sin guardar (RF-28). Primero consulta y después, ya en la
 * transacción, bloquea la versión, evalúa y guarda (D-83): así dos coordinadores no se pasan de las
 * horas, y no hay una transacción abierta durante una llamada a otro proceso.
 */
public final class ProponerDocenteServicio implements ProponerDocente {

    private final DatosParaSugerencia datos;
    private final DatosParaAfinidad afinidad;
    private final DatosParaActividades actividades;
    private final RepositorioDistributivos distributivos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;
    private final CalculadoraAfinidad calculadora = new CalculadoraAfinidad();

    /** Recibe los puertos de salida y el reloj. */
    public ProponerDocenteServicio(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.datos = datos;
        this.afinidad = afinidad;
        this.actividades = actividades;
        this.distributivos = distributivos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ResultadoDePropuesta proponer(SolicitudDePropuesta solicitud) {
        String periodo = solicitud.periodo();
        AsignaturaParalelo uno =
                paraleloDeSuCarrera(
                        periodo, solicitud.asignatura(), solicitud.paralelo(), solicitud.carrera());
        if (uno.retirada()) {
            throw new ParaleloRetirado();
        }
        if (uno.periodoCerrado()) {
            throw new PeriodoCerrado();
        }
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(SinDistributivo::new);
        version.exigirEditable(uno.carrera());
        PesosDelPeriodo pesos = afinidad.pesos(periodo).orElseThrow(PeriodoSinAbrir::new);
        DocenteActivo docente =
                datos.docentesActivos().stream()
                        .filter(activo -> activo.identificacion().equals(solicitud.docente()))
                        .findFirst()
                        .orElseThrow(DocenteNoActivo::new);
        BigDecimal puntaje =
                calculadora.calcular(docente.formacion(), uno.asignatura(), pesos.pesos()).total();
        ReglasDeLaAsignacion reglas =
                new ReglasDeLaAsignacion(
                        docente,
                        actividades
                                .proyectosYCargos(docente.identificacion(), periodo)
                                .orElseThrow(DocenteNoActivo::new),
                        datos.ofertaDelPeriodo(periodo).orElse(List.of()),
                        actividades.reglas(periodo).orElseThrow(PeriodoSinAbrir::new));
        Instant ahora = reloj.instant();
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo actual =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(SinDistributivo::new);
                    actual.exigirEditable(uno.carrera());
                    // Primero las reglas duras: ahí un texto no sirve. Para contar las horas da
                    // igual la justificación, que se decide después.
                    CargaHoraria carga =
                            reglas.exigir(
                                    actual,
                                    actual.conPropuesta(
                                            propuesta(
                                                    uno, docente, sinDecidir(), solicitud, ahora)));
                    Optional<Inconsistencia> afinidad =
                            MotorReglas.evaluarAfinidad(
                                    puntaje,
                                    pesos.minimoAfinidad(),
                                    uno.troncoComun(),
                                    reglas.reglas());
                    if (afinidad.map(Inconsistencia::severidad).orElse(null) == Severidad.DURA) {
                        throw new AfinidadInsuficiente();
                    }
                    if (afinidad.isPresent() && sinTexto(solicitud.texto())) {
                        return ResultadoDePropuesta.conAdvertencia(afinidad.get());
                    }
                    Propuesta propuesta =
                            propuesta(
                                    uno,
                                    docente,
                                    Justificacion.para(
                                            puntaje,
                                            pesos.minimoAfinidad(),
                                            uno.troncoComun(),
                                            solicitud.texto()),
                                    solicitud,
                                    ahora);
                    if (actual.parcialDe(uno.carrera()).isEmpty()) {
                        distributivos.agregarParcial(
                                actual.id(), ParcialCarrera.nuevo(uno.carrera()));
                    }
                    Optional<Propuesta> anterior =
                            actual.propuestaDe(propuesta.asignatura(), propuesta.paralelo());
                    // La misma propuesta no cambia nada: no se guarda ni se registra (D-94).
                    if (anterior.filter(propuesta::mismosDatosQue).isPresent()) {
                        return ResultadoDePropuesta.guardada(
                                new PropuestaVista(anterior.get(), false, reglas.sinExceso(carga)));
                    }
                    distributivos.guardarPropuesta(actual.id(), propuesta);
                    registro.registrar(
                            cambioGuardado(periodo, propuesta, anterior, solicitud.quien(), ahora));
                    return ResultadoDePropuesta.guardada(
                            new PropuestaVista(propuesta, false, reglas.sinExceso(carga)));
                });
    }

    private static Justificacion sinDecidir() {
        return new Justificacion(TipoDeJustificacion.AFINIDAD, null);
    }

    private static boolean sinTexto(String texto) {
        return texto == null || texto.isBlank();
    }

    private static Propuesta propuesta(
            AsignaturaParalelo uno,
            DocenteActivo docente,
            Justificacion justificacion,
            SolicitudDePropuesta solicitud,
            Instant ahora) {
        return new Propuesta(
                uno.asignatura().codigo(),
                uno.paralelo(),
                uno.carrera(),
                docente.identificacion(),
                docente.nombre(),
                justificacion,
                ahora,
                solicitud.quien());
    }

    @Override
    public void quitar(
            String periodo,
            String asignatura,
            String paralelo,
            Optional<String> carrera,
            String quien) {
        AsignaturaParalelo uno = paraleloDeSuCarrera(periodo, asignatura, paralelo, carrera);
        if (uno.periodoCerrado()) {
            throw new PeriodoCerrado();
        }
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(PropuestaNoEncontrada::new);
        version.exigirEditable(uno.carrera());
        Propuesta anterior =
                version.propuestaDe(asignatura, paralelo).orElseThrow(PropuestaNoEncontrada::new);
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    if (!distributivos.quitarPropuesta(version.id(), asignatura, paralelo)) {
                        throw new PropuestaNoEncontrada();
                    }
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.PROPUESTA_QUITADA,
                                    Map.of(
                                            "periodo", periodo,
                                            "asignatura", asignatura,
                                            "paralelo", paralelo),
                                    valores(anterior),
                                    Map.of()));
                    return asignatura;
                });
    }

    private AsignaturaParalelo paraleloDeSuCarrera(
            String periodo, String asignatura, String paralelo, Optional<String> carrera) {
        AsignaturaParalelo uno =
                datos.paralelo(periodo, asignatura, paralelo)
                        .orElseThrow(ParaleloNoEncontrado::new);
        if (carrera.isPresent() && !carrera.get().equals(uno.carrera())) {
            throw new OtraCarrera();
        }
        return uno;
    }

    private static CambioDeDistributivo cambioGuardado(
            String periodo,
            Propuesta propuesta,
            Optional<Propuesta> anterior,
            String quien,
            Instant ahora) {
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("periodo", periodo);
        detalle.put("asignatura", propuesta.asignatura());
        detalle.put("paralelo", propuesta.paralelo());
        detalle.put("docente", propuesta.docente());
        detalle.put("justificacion", propuesta.justificacion().tipo().name());
        anterior.ifPresent(una -> detalle.put("docenteAnterior", una.docente()));
        // El texto no va a la bitácora: solo que cambió (D-94).
        if (anterior.filter(propuesta::otroTextoQue).isPresent()) {
            detalle.put("textoCambiado", true);
        }
        return new CambioDeDistributivo(
                ahora,
                quien,
                CambioDeDistributivo.Accion.PROPUESTA_GUARDADA,
                detalle,
                anterior.map(ProponerDocenteServicio::valores).orElse(Map.of()),
                valores(propuesta));
    }

    /** Lo que cambia de una propuesta: el docente y el tipo de justificación, nunca su texto. */
    private static Map<String, Object> valores(Propuesta propuesta) {
        return Map.of(
                "docente", propuesta.docente(),
                "justificacion", propuesta.justificacion().tipo().name());
    }
}
