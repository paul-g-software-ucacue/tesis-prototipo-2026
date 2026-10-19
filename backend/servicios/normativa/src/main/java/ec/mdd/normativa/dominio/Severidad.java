package ec.mdd.normativa.dominio;

/** Qué pasa cuando una asignación no cumple una regla (glosario §7.3, RF-28, MD-08). */
public enum Severidad {
    /** Impide guardar o avanzar. */
    DURA,
    /** Exige una justificación. */
    BLANDA
}
