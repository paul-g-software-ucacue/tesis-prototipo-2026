package ec.mdd.firma.puertos.entrada;

import java.util.Objects;

/**
 * El archivo vigente de un documento firmable, verificado por su SHA-256 (F14-H01, FR-009).
 *
 * @param nombre el nombre con que se descarga: {@code distributivo-<periodo>-v<version>.pdf}
 * @param sha256 su SHA-256, el registrado
 * @param contenido sus bytes
 */
public record ArchivoVigente(String nombre, String sha256, byte[] contenido) {

    /** Exige los datos y copia el contenido. */
    public ArchivoVigente {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(sha256, "sha256");
        contenido = contenido.clone();
    }

    /** Una copia del contenido. */
    @Override
    public byte[] contenido() {
        return contenido.clone();
    }
}
