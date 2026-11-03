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

    /** Si es del esqueleto fijo, que ninguna configuración quita ni desactiva. */
    public boolean esEsqueleto() {
        return esqueleto;
    }
}
