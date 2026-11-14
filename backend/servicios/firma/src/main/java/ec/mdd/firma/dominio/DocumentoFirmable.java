package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El documento firmable registrado: el PDF/A de una versión congelada (F13-H02, FR-009, FR-010). No
 * cambia (principio VII); en F14 recibirá su archivo vigente y sus firmas.
 *
 * @param id su identificador
 * @param periodo el periodo
 * @param version la versión congelada
 * @param tipo en el prototipo, CONSOLIDADO
 * @param hashContenido el hash de contenido de la versión, el del pie (D-100)
 * @param sha256Original el SHA-256 del archivo generado, también su nombre
 * @param archivo dónde está, relativo al almacén: {@code documentos/<sha256>.pdf}
 * @param tamano su tamaño en bytes
 * @param generadoPor {@code sistema} o quien pidió el reintento
 * @param generadoEn cuándo
 */
public record DocumentoFirmable(
        UUID id,
        String periodo,
        int version,
        String tipo,
        String hashContenido,
        String sha256Original,
        String archivo,
        int tamano,
        String generadoPor,
        Instant generadoEn) {

    /** El tipo del documento firmable del prototipo (RF-58). */
    public static final String CONSOLIDADO = "CONSOLIDADO";

    /** Exige los datos. */
    public DocumentoFirmable {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(hashContenido, "hashContenido");
        Objects.requireNonNull(sha256Original, "sha256Original");
        Objects.requireNonNull(archivo, "archivo");
        Objects.requireNonNull(generadoPor, "generadoPor");
        Objects.requireNonNull(generadoEn, "generadoEn");
        if (tamano <= 0) {
            throw new IllegalArgumentException("Un documento tiene al menos un byte");
        }
    }
}
