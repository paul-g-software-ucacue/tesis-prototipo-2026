package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.ContenidoDelDocumento;

/** Genera el PDF/A-2b del documento firmable (F13-H02, D-101). */
@FunctionalInterface
public interface GeneradorDePdf {

    /**
     * @param contenido lo que se imprime
     * @return los bytes del PDF/A-2b, con los campos de firma vacíos
     */
    byte[] generar(ContenidoDelDocumento contenido);
}
