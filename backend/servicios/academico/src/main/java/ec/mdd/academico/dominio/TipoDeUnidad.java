package ec.mdd.academico.dominio;

/**
 * El tipo de una unidad de la estructura orgánica (RF-12, carga 01). Una comisión es solo un dato
 * informativo, sin horas (C-12, glosario §7.3).
 */
public enum TipoDeUnidad {
    SEDE_MATRIZ,
    SEDE,
    EXTENSION,
    CAMPUS,
    GOBIERNO,
    UNIDAD_ACADEMICA,
    GESTION,
    COMISION;

    /** Si es la sede matriz, una sede, una extensión o un campus. */
    public boolean esSede() {
        return this == SEDE_MATRIZ || this == SEDE || this == EXTENSION || this == CAMPUS;
    }
}
