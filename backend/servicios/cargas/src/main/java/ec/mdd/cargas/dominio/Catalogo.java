package ec.mdd.cargas.dominio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Un catálogo del diccionario de datos, como las 21 gestiones autorizadas o los cargos: los valores
 * que admite una columna (hoja «Catálogos» de la ERS).
 *
 * @param codigo cómo lo nombran las columnas (por ejemplo, {@code GESTIONES_AUTORIZADAS})
 * @param nombre su nombre, en español
 * @param valores sus valores, al menos uno y sin códigos repetidos
 */
public record Catalogo(String codigo, String nombre, List<ValorDeCatalogo> valores) {

    /** Valida el catálogo y copia sus valores. */
    public Catalogo {
        codigo = Textos.obligatorio(codigo, "El código del catálogo");
        nombre = Textos.obligatorio(nombre, "El nombre del catálogo " + codigo);
        if (valores == null || valores.isEmpty()) {
            throw new IllegalArgumentException("El catálogo " + codigo + " no tiene valores");
        }
        Set<String> vistos = new HashSet<>();
        for (ValorDeCatalogo valor : valores) {
            if (!vistos.add(valor.codigo())) {
                throw new IllegalArgumentException(
                        "El catálogo " + codigo + " repite el valor " + valor.codigo());
            }
        }
        valores = List.copyOf(valores);
    }
}
