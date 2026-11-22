package ec.mdd.reporting.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Una columna tal como la escribió el archivo de mapeo, sin validar: cada valor es el texto
 * escrito, o {@code null} si falta.
 *
 * @param nombre el encabezado
 * @param campo la clave del campo
 * @param traduccion código → valor, en el orden del archivo; {@code null} si no tiene
 */
public record BorradorDeColumna(String nombre, String campo, Map<String, String> traduccion) {

    /** Copia la traducción conservando su orden. */
    public BorradorDeColumna {
        traduccion =
                traduccion == null
                        ? null
                        : Collections.unmodifiableMap(new LinkedHashMap<>(traduccion));
    }
}
