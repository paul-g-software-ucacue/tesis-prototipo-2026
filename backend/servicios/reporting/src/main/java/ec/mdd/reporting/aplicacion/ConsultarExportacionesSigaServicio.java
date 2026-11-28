package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.puertos.entrada.ConsultarExportacionesSiga;
import ec.mdd.reporting.puertos.salida.ExportacionesSiga;
import java.util.List;

/**
 * Las exportaciones a SIGA de un periodo (F19-H02, D-120). Solo lee: un periodo sin publicación
 * tampoco tiene exportaciones, y eso no es un error.
 */
public final class ConsultarExportacionesSigaServicio implements ConsultarExportacionesSiga {

    private final ExportacionesSiga exportaciones;

    /**
     * @param exportaciones las exportaciones registradas
     */
    public ConsultarExportacionesSigaServicio(ExportacionesSiga exportaciones) {
        this.exportaciones = exportaciones;
    }

    @Override
    public List<ExportacionSiga> delPeriodo(String periodo) {
        return exportaciones.delPeriodo(periodo);
    }
}
