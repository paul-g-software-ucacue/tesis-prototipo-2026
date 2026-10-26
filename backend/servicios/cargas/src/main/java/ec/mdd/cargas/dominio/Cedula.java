package ec.mdd.cargas.dominio;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La identificación de un docente en un archivo de carga (RF-08, research §5): la cédula, con 10
 * dígitos y su dígito verificador (módulo 10), o el pasaporte. La provincia y el tercer dígito los
 * revisa Docentes al confirmar la carga (aclaración 1).
 */
final class Cedula {

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("\\d{10}");
    private static final Pattern NUEVE_DIGITOS = Pattern.compile("\\d{9}");
    private static final Pattern PASAPORTE = Pattern.compile("[0-9A-Z]{1,13}");

    /** Sin el cero inicial: el criterio de RF-08. */
    static final String SIN_CERO = "Cédula inválida o sin cero inicial: le falta el 0 inicial.";

    /** Cualquier otra cédula inválida. */
    static final String INVALIDA =
            "Cédula inválida o sin cero inicial: debe tener 10 dígitos y un dígito verificador"
                    + " válido.";

    /** Un pasaporte con otros caracteres. */
    static final String PASAPORTE_INVALIDO =
            "Un pasaporte tiene de 1 a 13 letras mayúsculas o dígitos.";

    private Cedula() {}

    /** Si es una cédula de 10 dígitos con su dígito verificador. */
    static boolean valida(String numero) {
        if (!DIEZ_DIGITOS.matcher(numero).matches()) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int producto = (numero.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += producto > 9 ? producto - 9 : producto;
        }
        return (10 - suma % 10) % 10 == numero.charAt(9) - '0';
    }

    /**
     * La cédula con el cero inicial, si le falta y con él es válida.
     *
     * @param numero lo que vino en el archivo
     */
    static Optional<String> conCeroInicial(String numero) {
        if (NUEVE_DIGITOS.matcher(numero).matches() && valida("0" + numero)) {
            return Optional.of("0" + numero);
        }
        return Optional.empty();
    }

    /** Si es un pasaporte válido. */
    static boolean pasaporte(String numero) {
        return PASAPORTE.matcher(numero).matches();
    }
}
