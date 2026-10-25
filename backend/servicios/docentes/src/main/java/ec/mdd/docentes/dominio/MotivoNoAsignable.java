package ec.mdd.docentes.dominio;

/** Por qué un proyecto no es asignable en un periodo (glosario «Proyecto asignable»). */
public enum MotivoNoAsignable {
    /** Está finalizado o suspendido. */
    ESTADO,
    /** Su vigencia no se cruza con las fechas del periodo. */
    VIGENCIA
}
