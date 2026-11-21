package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.ReporteIndividual;

/**
 * El PDF/A-2b del reporte individual, sin campos de formulario ni contenido activo y con la versión
 * y su hash en el pie de cada página (RF-48, aclaración del 2026-11-21, D-115).
 */
public interface GeneradorDePdf {

    /**
     * @param reporte el reporte individual
     * @return los bytes del PDF
     */
    byte[] generar(ReporteIndividual reporte);
}
