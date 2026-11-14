package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * El archivo vigente de un documento firmable falta en el almacén o su SHA-256 no es el registrado
 * (F14-H01, FR-009). Responde {@code FIRMA_ARCHIVO_NO_DISPONIBLE}: no se entrega nada.
 */
public final class ArchivoNoDisponible extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Recibe la causa, que solo va al registro del incidente. */
    public ArchivoNoDisponible(String causa) {
        super(causa);
    }
}
