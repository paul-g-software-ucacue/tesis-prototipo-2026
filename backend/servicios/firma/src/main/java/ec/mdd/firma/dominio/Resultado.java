package ec.mdd.firma.dominio;

/** El resultado de una comprobación de la validación básica (D-103). */
public enum Resultado {

    /** La comprobación pasó. */
    APROBADA,

    /** La comprobación falló: el archivo se rechaza. */
    RECHAZADA,

    /** No se hizo, porque una comprobación anterior de la que depende no pasó. */
    NO_EVALUADA
}
