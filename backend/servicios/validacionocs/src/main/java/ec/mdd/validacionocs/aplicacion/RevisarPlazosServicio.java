package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.PlazosDeEtapas;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.VencimientoDePlazo;
import ec.mdd.validacionocs.puertos.entrada.RevisarPlazos;
import ec.mdd.validacionocs.puertos.salida.AvisoDePlazoVencido;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioPlazos;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La revisión diaria de plazos (F12-H03, research §3, D-114). Para cada proceso en curso:
 *
 * <ol>
 *   <li>calcula el vencimiento de la etapa actual, con los plazos de la configuración que regía al
 *       crear el distributivo (la copia del proceso no los guarda) y el reloj;
 *   <li>si venció, pregunta a Académico si el periodo está cerrado; un periodo cerrado no avisa. Si
 *       Académico no responde (por ejemplo, sin identidad no hay token de proceso), sigue como con
 *       el periodo abierto, para que el aviso llegue cuando el proceso vuelva;
 *   <li>con el proceso bloqueado, vuelve a calcular: si la etapa o la llegada cambiaron entre
 *       medio, no avisa una etapa que ya se dejó;
 *   <li>registra el vencimiento, uno por llegada a la etapa;
 *   <li>solo si lo registró, en la misma transacción, pide el aviso (D-108) y deja {@code
 *       PLAZO_VENCIDO} en la bitácora.
 * </ol>
 *
 * <p>Así la revisión es idempotente aunque corra al arrancar y por el cron el mismo día, o dos a la
 * vez. Un fallo con un periodo se registra y no detiene a los demás. El plazo vencido solo avisa:
 * no cambia ningún estado (FR-003).
 */
public final class RevisarPlazosServicio implements RevisarPlazos {

    private static final Logger LOG = LoggerFactory.getLogger(RevisarPlazosServicio.class);

    private final RepositorioProceso procesos;
    private final EtapasAlCrearse etapas;
    private final DatosDelPeriodo datos;
    private final RepositorioPlazos plazos;
    private final AvisoDePlazoVencido aviso;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * @param procesos los procesos de aprobación
     * @param etapas la configuración que regía al crear cada distributivo, con sus plazos
     * @param datos el estado del periodo, de Académico
     * @param plazos los vencimientos registrados
     * @param aviso el aviso a Notificaciones
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de cada vencimiento
     * @param reloj el momento de la revisión
     */
    public RevisarPlazosServicio(
            RepositorioProceso procesos,
            EtapasAlCrearse etapas,
            DatosDelPeriodo datos,
            RepositorioPlazos plazos,
            AvisoDePlazoVencido aviso,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.procesos = procesos;
        this.etapas = etapas;
        this.datos = datos;
        this.plazos = plazos;
        this.aviso = aviso;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public int revisar() {
        Instant ahora = reloj.instant();
        int nuevos = 0;
        for (String periodo : procesos.enCurso()) {
            try {
                if (revisar(periodo, ahora)) {
                    nuevos++;
                }
            } catch (RuntimeException e) {
                LOG.warn(
                        "No se revisó el plazo de {}; queda para la próxima revisión: {}",
                        periodo,
                        e.getMessage());
            }
        }
        return nuevos;
    }

    private boolean revisar(String periodo, Instant ahora) {
        Optional<ProcesoAprobacion> proceso = procesos.de(periodo);
        if (proceso.isEmpty()) {
            return false;
        }
        Instant creadoEn = proceso.get().distributivoCreadoEn();
        PlazosDeEtapas deLaEtapa = etapas.plazos(periodo, creadoEn);
        Optional<VencimientoDePlazo> vencido = proceso.get().vencimiento(deLaEtapa, ahora);
        if (vencido.isEmpty() || cerradoOSinPeriodo(periodo)) {
            return false;
        }
        return transaccion.ejecutar(
                () -> {
                    Optional<ProcesoAprobacion> bloqueado =
                            procesos.bloquear(periodo)
                                    .filter(una -> una.distributivoCreadoEn().equals(creadoEn));
                    Optional<VencimientoDePlazo> todavia =
                            bloqueado
                                    .flatMap(una -> una.vencimiento(deLaEtapa, ahora))
                                    .filter(una -> una.plazo().equals(vencido.get().plazo()));
                    if (todavia.isEmpty() || !plazos.registrar(todavia.get())) {
                        return false;
                    }
                    aviso.avisar(bloqueado.get(), todavia.get());
                    registro.registrar(cambio(todavia.get()));
                    return true;
                });
    }

    // Sin identidad no hay token de proceso para preguntar a Académico. La revisión no espera al
    // día siguiente (escenario 8, SC-004): sigue como con el periodo abierto, y el aviso queda en
    // el registro de publicaciones hasta que identidad vuelva. Un periodo cerrado con el
    // distributivo sin publicar es raro; avisar de más es mejor que no avisar.
    private boolean cerradoOSinPeriodo(String periodo) {
        try {
            return datos.estado(periodo).map(EstadoDelPeriodo::cerrado).orElse(true);
        } catch (RuntimeException e) {
            LOG.warn(
                    "Académico no respondió por {}; el plazo se revisa como con el periodo abierto: {}",
                    periodo,
                    e.getMessage());
            return false;
        }
    }

    private static CambioDeFlujo cambio(VencimientoDePlazo vencimiento) {
        return new CambioDeFlujo(
                vencimiento.detectadoEn(),
                ProcesoAprobacion.SISTEMA,
                CambioDeFlujo.Accion.PLAZO_VENCIDO,
                Map.of(
                        "periodo",
                        vencimiento.periodo(),
                        "etapa",
                        vencimiento.plazo().etapa().name()),
                Map.of(),
                Map.of(
                        "fechaLimite",
                        vencimiento.plazo().fechaLimite().toString(),
                        "diasDeAtraso",
                        vencimiento.diasDeAtraso()));
    }
}
