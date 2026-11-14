package ec.mdd.firma.dominio;

import java.util.Objects;

/**
 * Un archivo de la cadena del documento firmable: dónde está en el almacén y su SHA-256, que es
 * también su nombre (D-101).
 *
 * @param nombre relativo al almacén: {@code documentos/<sha256>.pdf}
 * @param sha256 el SHA-256 registrado
 */
public record ArchivoDelDocumento(String nombre, String sha256) {

    /** Exige los datos. */
    public ArchivoDelDocumento {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(sha256, "sha256");
    }
}
