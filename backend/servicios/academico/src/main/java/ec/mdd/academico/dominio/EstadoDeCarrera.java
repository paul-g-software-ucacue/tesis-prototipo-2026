package ec.mdd.academico.dominio;

/**
 * Estado de una carrera (carga 2): solo una vigente admite mallas y asignaturas nuevas, y oferta.
 */
public enum EstadoDeCarrera {
    /** Se oferta. */
    VIGENTE,
    /** Ya no se oferta. */
    NO_VIGENTE,
    /** En cierre: no admite oferta nueva. */
    EN_CIERRE
}
