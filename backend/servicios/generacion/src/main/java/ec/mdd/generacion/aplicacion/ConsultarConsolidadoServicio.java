package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.UUID;

/**
 * Calcula el consolidado al abrirlo, sin guardar nada (RF-32). Las inconsistencias salen del panel:
 * no se evalúan dos veces de dos formas. Sin distributivo no hay nada que mostrar, y eso no es un
 * error.
 */
public final class ConsultarConsolidadoServicio implements ConsultarConsolidado {

    private final ConsultarPanel panel;
    private final DatosParaSugerencia datos;
    private final RepositorioDistributivos distributivos;
    private final RepositorioConsolidacion consolidacion;

    /** Recibe el panel y los puertos de salida. */
    public ConsultarConsolidadoServicio(
            ConsultarPanel panel,
            DatosParaSugerencia datos,
            RepositorioDistributivos distributivos,
            RepositorioConsolidacion consolidacion) {
        this.panel = panel;
        this.datos = datos;
        this.distributivos = distributivos;
        this.consolidacion = consolidacion;
    }

    @Override
    public ConsolidadoDelPeriodo consolidado(String periodo) {
        PanelDeInconsistencias actual = panel.panel(periodo);
        if (!actual.creado()) {
            return ConsolidadoDelPeriodo.sinDistributivo(periodo, actual.modificable());
        }
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(SinDistributivo::new);
        UUID id = version.id();
        return ConsolidadoDelPeriodo.armar(
                actual,
                version,
                datos.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new),
                consolidacion.ultimaConsolidacion(id),
                consolidacion.ultimoCambioDeAsignaciones(periodo),
                consolidacion.ultimoInforme(id));
    }
}
