package ec.mdd.cargas.dominio;

/**
 * Si una carga es necesaria para crear el distributivo (ERS §8.3, glosario «Carga obligatoria»).
 */
public enum Obligatoriedad {
    /** Sin ella no se crea el distributivo: las cargas 1 a 8. */
    OBLIGATORIA,
    /** La 9, proyectos y prácticas: obligatoria salvo que se confirme «sin proyectos» (RF-18). */
    OBLIGATORIA_SALVO_SIN_PROYECTOS,
    /** La 10, matriculados por carrera: si falta, el tipo de IST se fija a mano (RF-20). */
    RECOMENDADA
}
