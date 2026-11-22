package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.ArchivoSiga;

/**
 * Genera el CSV para SIGA de la versión publicada vigente con el layout de exportación vigente
 * (RF-52, F19-H01, D-119). Cada CSV queda en la bitácora antes de entregarse (FR-021).
 */
public interface GenerarCsvSiga {

    /**
     * @param periodo el código del periodo
     * @param quien quién lo genera
     * @return el archivo, ya registrado
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado,
     *     un docente viene repetido o las filas no suman lo del reporte general
     */
    ArchivoSiga generar(String periodo, String quien);
}
