package ec.mdd.cargas.dominio;

/** Las comprobaciones de texto que comparten los valores del diccionario de datos. */
final class Textos {

    private Textos() {}

    /**
     * El texto, que no puede estar en blanco ni empezar como una fórmula.
     *
     * @param valor el texto
     * @param donde qué es, para el mensaje (por ejemplo, «la columna codigo: tipo»)
     * @return el texto sin cambios
     */
    static String obligatorio(String valor, String donde) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(donde + " no puede estar en blanco");
        }
        return sinFormula(valor, donde);
    }

    /**
     * El texto, o una cadena vacía si no tiene valor; no puede empezar como una fórmula.
     *
     * @param valor el texto, o {@code null}
     * @param donde qué es, para el mensaje
     * @return el texto, o una cadena vacía
     */
    static String opcional(String valor, String donde) {
        return valor == null ? "" : sinFormula(valor, donde);
    }

    /**
     * Rechaza un texto que una hoja de cálculo tomaría como fórmula: el que empieza con {@code =},
     * {@code +}, {@code -}, {@code @}, un tabulador o un retorno de carro (principio IX).
     */
    private static String sinFormula(String valor, String donde) {
        if (!valor.isEmpty() && "=+-@\t\r".indexOf(valor.charAt(0)) >= 0) {
            throw new IllegalArgumentException(
                    donde + " empieza como una fórmula de hoja de cálculo: «" + valor + "»");
        }
        return valor;
    }
}
