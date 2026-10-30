package ec.mdd.cargas.dominio;

/** Si una carga tiene datos (glosario «Estado de la carga», F01-H05). */
public enum EstadoDeLaCarga {
    /** Su servicio dueño no tiene ningún registro vigente de ella. */
    PENDIENTE,
    /** Su servicio dueño tiene al menos un registro vigente, entrado por archivo o por pantalla. */
    CARGADA,
    /** Solo la carga de proyectos: sin datos, pero con algún periodo confirmado «sin proyectos». */
    SIN_PROYECTOS
}
