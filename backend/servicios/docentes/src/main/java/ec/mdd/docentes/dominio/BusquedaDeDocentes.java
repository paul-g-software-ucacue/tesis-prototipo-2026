package ec.mdd.docentes.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Los criterios para buscar docentes (FR-007): un texto que se compara sin mayúsculas ni tildes con
 * los nombres y apellidos, o con el inicio de la identificación, y la carrera base y el estado
 * laboral. Un criterio vacío no filtra.
 *
 * @param texto parte del nombre o de los apellidos, o el inicio de la identificación; o {@code
 *     null}
 * @param carrera el código de la carrera base, o {@code null}
 * @param estado el estado laboral, o {@code null}
 */
public record BusquedaDeDocentes(String texto, String carrera, EstadoLaboral estado) {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    /** Deja el texto sin espacios en los extremos; vacío es {@code null}. */
    public BusquedaDeDocentes {
        texto = texto == null || texto.isBlank() ? null : texto.strip();
        carrera = carrera == null || carrera.isBlank() ? null : carrera.strip();
    }

    /** Sin criterios: todos los docentes. */
    public static BusquedaDeDocentes todos() {
        return new BusquedaDeDocentes(null, null, null);
    }

    /** Si el docente cumple todos los criterios. */
    public boolean aplicaA(Docente docente) {
        if (carrera != null && !carrera.equals(docente.datos().carreraBase())) {
            return false;
        }
        if (estado != null && estado != docente.estado()) {
            return false;
        }
        if (texto == null) {
            return true;
        }
        String buscado = normalizado(texto);
        return normalizado(docente.datos().nombreCompleto()).contains(buscado)
                || docente.identificacion().numero().startsWith(buscado);
    }

    // «Peraltá», «PERALTA» y «peralta» son lo mismo.
    private static String normalizado(String valor) {
        return MARCAS.matcher(Normalizer.normalize(valor, Normalizer.Form.NFD))
                .replaceAll("")
                .toUpperCase(Locale.ROOT);
    }
}
