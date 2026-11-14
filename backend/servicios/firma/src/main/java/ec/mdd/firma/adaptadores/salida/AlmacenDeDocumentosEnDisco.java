package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Los PDF del documento firmable en la carpeta {@code documentos/} del volumen de D-88, nombrados
 * por su SHA-256 (F13-H02, D-101, RNF-POR-01). Nunca sobrescribe: se escribe un temporal y se
 * publica con un enlace duro, que falla si el nombre ya existe ({@code CREATE_NEW}). Un movimiento
 * atómico podría reemplazar el archivo existente. Si el nombre ya existe, su contenido es el mismo
 * por construcción y no se escribe otra vez. La carpeta se crea al guardar: arrancar el proceso no
 * toca el disco.
 */
public final class AlmacenDeDocumentosEnDisco implements AlmacenDeDocumentos {

    private static final String CARPETA = "documentos";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private final Path raiz;

    /** Recibe la carpeta raíz del volumen, que puede no existir todavía. */
    public AlmacenDeDocumentosEnDisco(Path raiz) {
        this.raiz = raiz;
    }

    @Override
    public String guardar(byte[] pdf, String sha256) {
        if (!HASH.matcher(sha256).matches()) {
            throw new IllegalArgumentException("El nombre es un SHA-256 en hexadecimal");
        }
        String nombre = CARPETA + "/" + sha256 + ".pdf";
        try {
            Path carpeta = Files.createDirectories(raiz.resolve(CARPETA));
            Path destino = carpeta.resolve(sha256 + ".pdf");
            if (Files.exists(destino)) {
                return nombre;
            }
            Path temporal = Files.createTempFile(carpeta, "documento-", ".tmp");
            try {
                Files.write(temporal, pdf);
                Files.createLink(destino, temporal);
            } catch (FileAlreadyExistsException otraLoGuardo) {
                // Otra generación lo publicó entre la comprobación y el enlace: es el mismo.
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo guardar el documento firmable", error);
        }
        return nombre;
    }
}
