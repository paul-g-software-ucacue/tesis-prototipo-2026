package ec.mdd.plataforma.cargas;

import java.util.Map;
import java.util.Objects;

/**
 * Una fila del archivo de una carga, sin errores (F01-H03), lista para que su dueño la traduzca.
 *
 * @param fila su número en el archivo, contando el encabezado como la 1
 * @param valores el valor de cada campo, sin los espacios de los extremos; vacío si no tiene
 */
public record FilaDelLote(int fila, Map<String, String> valores) {

    /** Valida el número y copia los valores. */
    public FilaDelLote {
        if (fila < 2) {
            throw new IllegalArgumentException("La fila 1 es el encabezado: " + fila);
        }
        valores = Map.copyOf(Objects.requireNonNull(valores, "valores"));
    }

    /** El valor de un campo; vacío si no lo tiene. */
    public String valor(String campo) {
        return valores.getOrDefault(campo, "");
    }
}
