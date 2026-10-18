package ec.mdd.normativa.dominio;

/**
 * Tipo de instituto según sus matriculados (RN-06): T1 de 1 a 300, T2 de 301 a 600, T3 de 601 a
 * 1200, T4 de 1201 a 1800 y T5 de 1801 a 3500. Los rangos son parámetros de la versión de
 * normativa; en el prototipo, el tipo de la institución es un parámetro fijo (Tipo 2), y su cálculo
 * por matriculados es F23.
 */
public enum TipoIst {
    /** Tipo 1. */
    T1,
    /** Tipo 2. */
    T2,
    /** Tipo 3. */
    T3,
    /** Tipo 4. */
    T4,
    /** Tipo 5. */
    T5
}
