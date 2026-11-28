package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.CsvExportado;

/**
 * Genera el CSV para SIGA de la versión publicada vigente con el layout de exportación vigente
 * (RF-52, F19-H01, D-119). Cada CSV queda en la bitácora antes de entregarse (FR-021).
 */
public interface GenerarCsvSiga {

    /**
     * @param periodo el código del periodo
     * @param quien quién lo genera
     * @return el archivo y su exportación, ya guardada con su entrada en la bitácora (F19-H02)
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.ExportacionRechazada si la versión no tiene todas sus firmas
     *     o tiene inconsistencias abiertas (F19-H02, D-120)
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado,
     *     un docente viene repetido o las filas no suman lo del reporte general
     */
    CsvExportado generar(String periodo, String quien);
}
