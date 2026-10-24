package ec.mdd.cargas.dominio;

/**
 * Una regla común a todas las cargas, como el separador, las fechas o las listas (hoja
 * «Convenciones» de la ERS).
 *
 * @param tema de qué trata (por ejemplo, «Fechas»)
 * @param regla qué dice (por ejemplo, «aaaa-mm-dd (ISO 8601).»)
 */
public record Convencion(String tema, String regla) {

    /** Valida que los dos textos tengan valor y no empiecen como una fórmula. */
    public Convencion {
        tema = Textos.obligatorio(tema, "El tema de la convención");
        regla = Textos.obligatorio(regla, "La regla de la convención " + tema);
    }
}
