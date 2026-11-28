package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ContenidoCongelado;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.InconsistenciasAlCongelar;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.GuardarContenidoCongelado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.salida.DatosParaElDocumento;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioContenidoCongelado;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guarda el contenido congelado de una versión (F20-H01, D-115). Las horas de clase de una versión
 * salen de la oferta de Académico, que se puede corregir después de publicar; guardarlas cuando se
 * congela la versión hace que lo publicado no cambie y coincida con el PDF firmado, que se calcula
 * con el mismo aviso (principio VII).
 *
 * <p>Lee la oferta, los nombres de las carreras y los docentes de Datos fuera de la transacción;
 * guarda la cabecera y las cargas en una sola. Si dos guardados llegan a la vez, el repositorio
 * deja el primero.
 */
public final class GuardarContenidoCongeladoServicio implements GuardarContenidoCongelado {

    private static final Logger LOG =
            LoggerFactory.getLogger(GuardarContenidoCongeladoServicio.class);

    private final RepositorioDistributivos distributivos;
    private final RepositorioContenidoCongelado repositorio;
    private final DatosParaSugerencia datos;
    private final DatosParaElDocumento carreras;
    private final ConsultarPanel panel;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * @param distributivos las versiones del distributivo
     * @param repositorio el contenido congelado
     * @param datos la oferta del periodo y los docentes activos
     * @param carreras los nombres de las carreras
     * @param panel las inconsistencias de la versión vigente (F19-H02, D-120)
     * @param transaccion la transacción del guardado
     * @param reloj el momento del guardado
     */
    public GuardarContenidoCongeladoServicio(
            RepositorioDistributivos distributivos,
            RepositorioContenidoCongelado repositorio,
            DatosParaSugerencia datos,
            DatosParaElDocumento carreras,
            ConsultarPanel panel,
            Transaccion transaccion,
            Clock reloj) {
        this.distributivos = distributivos;
        this.repositorio = repositorio;
        this.datos = datos;
        this.carreras = carreras;
        this.panel = panel;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void guardar(String periodo, int version) {
        if (guardarSiFalta(periodo, version, false)) {
            LOG.info("Contenido congelado guardado: periodo {}, versión {}", periodo, version);
        }
    }

    /**
     * Guarda el contenido si la versión está CONGELADA y no lo tiene.
     *
     * @param tardio si se calcula al pedirlo y no al congelar
     * @return si la versión tiene ahora contenido guardado: falso si no existe o no está congelada
     */
    boolean guardarSiFalta(String periodo, int numero, boolean tardio) {
        if (repositorio.hash(periodo, numero).isPresent()) {
            return true;
        }
        Optional<VersionDistributivo> leida = distributivos.version(periodo, numero);
        if (leida.isEmpty() || leida.get().estado() != EstadoDeVersion.CONGELADA) {
            LOG.info(
                    "Sin contenido congelado que guardar: periodo {}, versión {} {}",
                    periodo,
                    numero,
                    leida.map(una -> "en " + una.estado()).orElse("inexistente"));
            return false;
        }
        VersionDistributivo version = leida.get();
        Map<String, DocenteActivo> activos =
                datos.docentesActivos().stream()
                        .collect(
                                Collectors.toMap(
                                        DocenteActivo::identificacion,
                                        Function.identity(),
                                        (uno, otro) -> uno));
        ContenidoCongelado contenido =
                ContenidoCongelado.de(
                        periodo,
                        version,
                        datos.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new),
                        carreras.nombresDeCarreras(),
                        activos);
        Optional<InconsistenciasAlCongelar> inconsistencias =
                inconsistencias(periodo, numero, tardio);
        boolean guardado =
                transaccion.ejecutar(
                        () ->
                                repositorio.guardar(
                                        version.id(),
                                        contenido,
                                        inconsistencias,
                                        reloj.instant(),
                                        tardio));
        if (guardado && tardio) {
            LOG.warn(
                    "Contenido congelado calculado tarde, con la oferta de ahora: periodo {},"
                            + " versión {}",
                    periodo,
                    numero);
        }
        return true;
    }

    /**
     * Las inconsistencias abiertas de la versión, del panel, que siempre evalúa la vigente
     * (F19-H02, D-120). Solo cuentan si el panel es de esta misma versión: al congelar lo es; en un
     * guardado tardío, durante una reforma ya no, y no se adivinan.
     *
     * <p>Si el panel falla al congelar, se propaga: el reenvío de publicaciones repite el guardado
     * (ADR-018), porque el contenido no se puede guardar dos veces y sin el dato la versión no se
     * exporta. En un guardado tardío se guarda sin él, para no dejar sin reporte a quien lo pidió.
     */
    private Optional<InconsistenciasAlCongelar> inconsistencias(
            String periodo, int numero, boolean tardio) {
        PanelDeInconsistencias actual;
        try {
            actual = panel.panel(periodo);
        } catch (RuntimeException fallo) {
            if (!tardio) {
                throw fallo;
            }
            LOG.warn(
                    "Contenido congelado tardío sin inconsistencias: el panel no se pudo evaluar"
                            + " (periodo {}, versión {})",
                    periodo,
                    numero,
                    fallo);
            return Optional.empty();
        }
        if (!actual.creado() || actual.numero() != numero) {
            return Optional.empty();
        }
        return Optional.of(
                new InconsistenciasAlCongelar(
                        Math.toIntExact(actual.abiertas(Severidad.DURA)),
                        Math.toIntExact(actual.abiertas(Severidad.BLANDA))));
    }
}
