package ec.mdd.validacionocs.dominio;

import java.util.List;

/**
 * Lo que dice un archivo, sin validar: cada valor es el texto escrito, o {@code null} si falta. Así
 * el dominio puede decir todo lo que está mal, no solo lo primero que encuentre (FR-015).
 *
 * @param etapas las etapas, en el orden del archivo
 * @param firmas las firmas, en el orden del archivo
 * @param problemas lo que el lector ya encontró, como una clave que no existe
 */
public record Borrador(
        List<EtapaEscrita> etapas, List<FirmaEscrita> firmas, List<String> problemas) {

    /** Copia las listas. */
    public Borrador {
        etapas = List.copyOf(etapas);
        firmas = List.copyOf(firmas);
        problemas = List.copyOf(problemas);
    }

    /**
     * Una etapa tal como se escribió.
     *
     * @param tipo su clave
     * @param activa el texto de {@code activa}
     * @param nombre su nombre
     * @param rol su rol responsable
     * @param plazoDias el texto de {@code plazo_dias}
     * @param admiteDevolucion el texto de {@code admite_devolucion}
     */
    public record EtapaEscrita(
            String tipo,
            String activa,
            String nombre,
            String rol,
            String plazoDias,
            String admiteDevolucion) {}

    /**
     * Una firma tal como se escribió.
     *
     * @param orden el texto de {@code orden}
     * @param etiqueta su etiqueta
     * @param rol quién firma
     * @param etapa en qué etapa
     * @param documento sobre qué documento
     */
    public record FirmaEscrita(
            String orden, String etiqueta, String rol, String etapa, String documento) {}
}
