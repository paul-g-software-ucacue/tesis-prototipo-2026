package ec.mdd.firma.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * La forma en que se comparan el CN de un certificado y el nombre impreso de un paso (D-23, D-104):
 * en mayúsculas, sin tildes ni diéresis, y con los espacios unificados. La «Ñ» se conserva: es una
 * letra, no una tilde. Lo demás (paréntesis, guiones) queda igual.
 */
public final class NombreDeFirmante {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private NombreDeFirmante() {}

    /**
     * El nombre normalizado.
     *
     * @param nombre el CN o el nombre impreso
     * @return en mayúsculas, sin marcas diacríticas salvo la de la Ñ, con un solo espacio entre
     *     palabras y sin espacios al inicio ni al final
     */
    public static String normalizar(String nombre) {
        String mayusculas = nombre.toUpperCase(Locale.ROOT).replace('Ñ', '\u0000');
        String sinMarcas =
                MARCAS.matcher(Normalizer.normalize(mayusculas, Normalizer.Form.NFD))
                        .replaceAll("")
                        .replace('\u0000', 'Ñ');
        return ESPACIOS.matcher(sinMarcas.strip()).replaceAll(" ");
    }
}
