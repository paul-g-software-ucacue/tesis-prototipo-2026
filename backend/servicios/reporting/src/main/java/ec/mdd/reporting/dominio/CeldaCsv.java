package ec.mdd.reporting.dominio;

/**
 * Una celda de CSV que una hoja de cálculo no puede interpretar como fórmula (RNF-SEG-04). La usan
 * el CSV de los reportes (D-115) y el CSV para SIGA (D-119), para que la regla sea una sola.
 */
public final class CeldaCsv {

    private CeldaCsv() {}

    /**
     * @param valor el texto de la celda, sin comillas
     * @return el mismo texto con un apóstrofo delante si empieza con {@code =}, {@code +}, {@code
     *     -}, {@code @}, una tabulación o un retorno de carro
     */
    public static String neutralizada(String valor) {
        return comoFormula(valor) ? "'" + valor : valor;
    }

    private static boolean comoFormula(String valor) {
        if (valor.isEmpty()) {
            return false;
        }
        char primero = valor.charAt(0);
        return primero == '='
                || primero == '+'
                || primero == '-'
                || primero == '@'
                || primero == '\t'
                || primero == '\r';
    }
}
