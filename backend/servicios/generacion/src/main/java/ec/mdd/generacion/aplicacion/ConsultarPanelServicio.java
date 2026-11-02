package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.DatosDelPeriodo;
import ec.mdd.generacion.dominio.MotorReglas;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.PeriodoListo;
import ec.mdd.generacion.dominio.ReglasDelPanel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaElPanel;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.RepositorioJustificaciones;
import java.util.List;
import java.util.Optional;

/**
 * Evalúa la versión vigente al abrir el panel, sin guardar nada (RF-29). Sin distributivo no hay
 * nada que evaluar, y eso no es un error. También se lee con el periodo cerrado.
 */
public final class ConsultarPanelServicio implements ConsultarPanel {

    private final DatosParaSugerencia datos;
    private final DatosParaElPanel normativa;
    private final RepositorioDistributivos distributivos;
    private final RepositorioJustificaciones justificaciones;

    /** Recibe los puertos de salida. */
    public ConsultarPanelServicio(
            DatosParaSugerencia datos,
            DatosParaElPanel normativa,
            RepositorioDistributivos distributivos,
            RepositorioJustificaciones justificaciones) {
        this.datos = datos;
        this.normativa = normativa;
        this.distributivos = distributivos;
        this.justificaciones = justificaciones;
    }

    @Override
    public PanelDeInconsistencias panel(String periodo) {
        PeriodoListo estado = datos.periodoListo(periodo).orElseThrow(PeriodoNoEncontrado::new);
        boolean modificable = !estado.cerrado();
        Optional<VersionDistributivo> vigente = distributivos.vigenteDe(periodo);
        if (vigente.isEmpty()) {
            return PanelDeInconsistencias.sinDistributivo(periodo, modificable);
        }
        VersionDistributivo version = vigente.orElseThrow();
        ReglasDelPanel reglas = normativa.reglasDelPanel(periodo).orElseThrow(PeriodoSinAbrir::new);
        List<AsignaturaParalelo> oferta =
                datos.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new);
        return PanelDeInconsistencias.armar(
                periodo,
                modificable,
                version,
                MotorReglas.evaluarVersion(
                        new DatosDelPeriodo(
                                periodo,
                                version,
                                oferta,
                                datos.docentesActivos(),
                                normativa.cargosVigentes()),
                        reglas),
                justificaciones.vigentes(version.id()),
                oferta);
    }
}
