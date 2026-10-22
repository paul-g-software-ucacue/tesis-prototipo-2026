package ec.mdd.roles.dominio;

/**
 * El estado de un periodo académico según la copia que Roles recibe de Académico (F05-H03, research
 * §1). Los estados del periodo solo avanzan, en el orden en que se declaran (RF-13), así que de dos
 * estados vale el más avanzado, sin importar en qué orden llegaron los eventos.
 */
public enum EstadoConocido {
    /** Recién registrado. */
    PLANIFICACION,
    /** En aprobación. */
    EN_APROBACION,
    /** En curso. */
    VIGENTE,
    /** El último estado: su Responsable de consolidación ya no tiene los permisos del rol. */
    CERRADO;

    /** El más avanzado de los dos. */
    public EstadoConocido masAvanzado(EstadoConocido otro) {
        return otro.ordinal() > ordinal() ? otro : this;
    }

    /** Si el periodo está cerrado. */
    public boolean cerrado() {
        return this == CERRADO;
    }
}
