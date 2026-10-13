package ec.mdd.firma.spike;

import java.time.Instant;

/**
 * Resultado de validar una firma del PDF (data-model.md, «Resultado de la validación»).
 *
 * @param indice número de la firma en el orden del documento, desde 1
 * @param firmante CN del certificado del firmante, o {@code null} si no se pudo leer
 * @param momento momento de la firma: {@code signingTime} del CMS o, si falta, {@code /M}
 * @param byteRange si el ByteRange cubre el archivo hasta el final de su revisión, salvo /Contents
 * @param integridad si el contenido del ByteRange coincide con lo firmado y la firma se verifica
 * @param cadena si hay un camino PKIX del certificado hasta la raíz de confianza
 * @param vigencia si el certificado del firmante era válido en el momento de la firma
 */
public record ResultadoFirma(
        int indice,
        String firmante,
        Instant momento,
        Comprobacion byteRange,
        Comprobacion integridad,
        Comprobacion cadena,
        Comprobacion vigencia) {

    /**
     * Indica si la firma pasa las cuatro comprobaciones.
     *
     * @return {@code true} si todas son válidas
     */
    public boolean esValida() {
        return byteRange.valida() && integridad.valida() && cadena.valida() && vigencia.valida();
    }

    /**
     * Resumen de una línea sin el CN, para el modo local de D-43 (el CN real no se imprime).
     *
     * @return el resumen sin datos personales
     */
    public String resumenSinFirmante() {
        return "firma "
                + indice
                + " · momento "
                + momento
                + " · ByteRange "
                + byteRange
                + " · integridad "
                + integridad
                + " · cadena "
                + cadena
                + " · vigencia "
                + vigencia;
    }

    /**
     * Resultado de una comprobación.
     *
     * @param valida si la comprobación pasa
     * @param motivo por qué no pasa; vacío si pasa
     */
    public record Comprobacion(boolean valida, String motivo) {

        /** Comprobación que pasa. */
        static Comprobacion ok() {
            return new Comprobacion(true, "");
        }

        /** Comprobación que no pasa, con su motivo. */
        static Comprobacion falla(String motivo) {
            return new Comprobacion(false, motivo);
        }

        @Override
        public String toString() {
            return valida ? "válida" : "no válida (" + motivo + ")";
        }
    }
}
