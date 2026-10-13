package ec.mdd.plataforma.errores.nucleo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

/**
 * Convierte una excepción en texto para el registro, con los secretos ocultos (principio XIII,
 * RNF-SEG-06).
 *
 * <p>Conserva la clase, el mensaje, la traza y las causas, que hacen falta para diagnosticar, pero
 * reemplaza por {@value #OCULTO} los JWT, lo que sigue a {@code Bearer} y los valores de {@code
 * clave}, {@code password} y {@code contraseña}. No puede reconocer datos personales como nombres o
 * cédulas: por eso las excepciones propias nunca los llevan en su mensaje.
 */
public final class SaneadorDeTrazas {

    /** Texto que reemplaza a cada secreto. */
    public static final String OCULTO = "[oculto]";

    private static final Pattern JWT =
            Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern BEARER = Pattern.compile("(?i)(Bearer\\s+)\\S+");
    private static final Pattern CLAVE =
            Pattern.compile("(?iu)\\b(clave|password|contraseña)\\s*[=:]\\s*[^\\s,;&\"']+");

    private SaneadorDeTrazas() {}

    /** La traza completa de la excepción y sus causas, saneada. */
    public static String trazaSaneada(Throwable excepcion) {
        StringWriter texto = new StringWriter();
        excepcion.printStackTrace(new PrintWriter(texto));
        return sanear(texto.toString());
    }

    /** El texto con los secretos reemplazados por {@value #OCULTO}. */
    public static String sanear(String texto) {
        String saneado = JWT.matcher(texto).replaceAll(OCULTO);
        saneado = BEARER.matcher(saneado).replaceAll("$1" + OCULTO);
        return CLAVE.matcher(saneado).replaceAll("$1=" + OCULTO);
    }
}
