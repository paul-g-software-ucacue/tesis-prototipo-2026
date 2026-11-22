package ec.mdd.reporting.dominio;

import java.util.List;

/**
 * El resultado de validar un archivo de mapeo: sus columnas o todos sus problemas.
 *
 * @param columnas las columnas válidas, en orden; vacía si hay problemas
 * @param problemas todos los problemas; vacía si es válido
 */
public record ValidacionDelLayout(
        List<ColumnaDelLayout> columnas, List<ProblemaDelLayout> problemas) {

    /** Copia las listas. */
    public ValidacionDelLayout {
        columnas = List.copyOf(columnas);
        problemas = List.copyOf(problemas);
    }

    /** Si no tiene problemas. */
    public boolean esValida() {
        return problemas.isEmpty();
    }
}
