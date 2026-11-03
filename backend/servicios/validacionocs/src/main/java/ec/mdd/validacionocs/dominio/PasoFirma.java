package ec.mdd.validacionocs.dominio;

/**
 * Un paso del árbol de firmas (RF-58).
 *
 * @param orden su lugar, desde 1
 * @param etiqueta Elaborado, Revisado o Aprobado
 * @param rol quién firma
 * @param etapa en qué etapa
 * @param documento sobre qué documento
 */
public record PasoFirma(
        int orden, String etiqueta, String rol, TipoDeEtapa etapa, DocumentoDeFirma documento) {}
