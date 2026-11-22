package ec.mdd.reporting.dominio;

import java.util.List;

/**
 * Lo que el lector entendió de un archivo de mapeo, sin validar (research §7). Lo que no entendió
 * (una clave desconocida, un nodo del tipo equivocado) ya viene como problema.
 *
 * @param columnas las columnas, en el orden del archivo
 * @param problemas los problemas de estructura que encontró el lector
 */
public record BorradorDeLayout(
        List<BorradorDeColumna> columnas, List<ProblemaDelLayout> problemas) {

    /** Copia las listas. */
    public BorradorDeLayout {
        columnas = List.copyOf(columnas);
        problemas = List.copyOf(problemas);
    }
}
