package ec.mdd.plataforma.cargas;

/** Qué pasa con un registro de la vista previa si se confirma (glosario «Vista previa»). */
public enum TipoDeRegistro {
    /** No existe en el dueño: se registra. */
    NUEVO,
    /** Existe con otros valores: se corrige. */
    CAMBIA,
    /** Existe en el dueño y no viene en el archivo: se informa, pero no se aplica. */
    BAJA,
    /** Existe con los mismos valores: no se toca. */
    IGUAL
}
