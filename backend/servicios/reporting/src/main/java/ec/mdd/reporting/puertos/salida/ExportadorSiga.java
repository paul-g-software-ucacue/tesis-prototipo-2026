package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;

/**
 * El puerto «Exportación a SIGA» de la tabla del principio I (RF-52, ADR-002, D-119). El adaptador
 * del prototipo escribe el CSV con el layout de exportación; si SIGA llega a tener una API, sería
 * otro adaptador de este mismo puerto, sin tocar el dominio ni el caso de uso.
 */
public interface ExportadorSiga {

    /**
     * Escribe las filas ya comprobadas, con las columnas del layout en su orden. El resultado no
     * depende del momento ni de quién lo pide: las mismas filas con el mismo layout dan el mismo
     * archivo (FR-007).
     *
     * @param filas las filas de la versión publicada, en orden
     * @param layout el layout vigente al empezar
     * @return el archivo
     */
    ArchivoSiga exportar(FilasParaSiga filas, LayoutExportacion layout);
}
