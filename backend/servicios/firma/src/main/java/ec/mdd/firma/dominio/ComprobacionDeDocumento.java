package ec.mdd.firma.dominio;

import java.util.Arrays;

/**
 * La primera comprobación de la validación básica, la única que no necesita leer el PDF (D-23,
 * D-103): el archivo subido empieza con todos los bytes del vigente.
 *
 * <p>Una firma incremental solo agrega al final, así que este prefijo también garantiza que las
 * firmas anteriores y el contenido no cambiaron (comprobaciones 2 y 8 del diseño de firmas §6). Un
 * archivo igual al vigente pasa esta comprobación: no tener firma nueva lo rechaza la siguiente,
 * que lo dice con su propio código.
 */
public final class ComprobacionDeDocumento {

    private ComprobacionDeDocumento() {}

    /**
     * Si el archivo subido es el vigente, con o sin algo agregado al final.
     *
     * @param vigente los bytes del archivo vigente
     * @param subido los bytes del archivo subido
     * @return aprobada, o rechazada con su motivo
     */
    public static ResultadoDeComprobacion mismoDocumento(byte[] vigente, byte[] subido) {
        boolean prefijo =
                subido.length >= vigente.length
                        && Arrays.equals(subido, 0, vigente.length, vigente, 0, vigente.length);
        return prefijo
                ? ResultadoDeComprobacion.aprobada(Comprobacion.MISMO_DOCUMENTO)
                : ResultadoDeComprobacion.rechazada(
                        Comprobacion.MISMO_DOCUMENTO,
                        "El archivo no empieza con el PDF vigente del documento: no es la versión"
                                + " vigente.");
    }
}
