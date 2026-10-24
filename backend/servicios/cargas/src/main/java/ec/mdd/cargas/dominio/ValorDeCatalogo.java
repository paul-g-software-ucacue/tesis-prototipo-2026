package ec.mdd.cargas.dominio;

/**
 * Un valor permitido de un catálogo del diccionario de datos.
 *
 * @param codigo el código exacto, como se escribe en el archivo (por ejemplo, {@code G07})
 * @param descripcion qué significa, o vacío si la ERS no lo dice (como en los perfiles de carga)
 */
public record ValorDeCatalogo(String codigo, String descripcion) {

    /** Valida que el código tenga valor y que ningún texto empiece como una fórmula. */
    public ValorDeCatalogo {
        codigo = Textos.obligatorio(codigo, "El código del valor");
        descripcion = Textos.opcional(descripcion, "La descripción del valor " + codigo);
    }
}
