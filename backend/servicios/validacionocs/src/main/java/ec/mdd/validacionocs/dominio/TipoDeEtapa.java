package ec.mdd.validacionocs.dominio;

/** Las etapas del flujo, en su orden (RF-34, ADR-007). */
public enum TipoDeEtapa {
    /** La propuesta de cada carrera. */
    BORRADOR_CARRERA(true),
    /** La planificación de investigación y vinculación. */
    BORRADOR_UNIDADES(false),
    /** La consolidación institucional. */
    EN_CONSOLIDACION(false),
    /** La revisión del Vicerrectorado. */
    EN_REVISION_VRA(false),
    /** El documento congelado, en firma. */
    CONGELADO_EN_FIRMA(true),
    /** La resolución del OCS. */
    EN_OCS(true),
    /** La remisión a la instancia zonal. */
    REMITIDO_ZONAL(false),
    /** Aprobado, antes de publicarse. */
    APROBADO(true),
    /** Publicado: el final del flujo. */
    PUBLICADO(true);

    private final boolean esqueleto;

    TipoDeEtapa(boolean esqueleto) {
        this.esqueleto = esqueleto;
    }

    /**
     * Si el consolidado todavía admite cambios en esta etapa: consolidar, justificar una blanda y
     * adjuntar el informe (#215, D-99). Hasta EN_CONSOLIDACION inclusive, en el orden del flujo;
     * desde EN_REVISION_VRA, lo enviado a revisión no cambia.
     */
    public boolean admiteCambiosEnElConsolidado() {
        return compareTo(EN_CONSOLIDACION) <= 0;
    }

    /** Si es del esqueleto fijo, que ninguna configuración quita ni desactiva. */
    public boolean esEsqueleto() {
        return esqueleto;
    }
}
