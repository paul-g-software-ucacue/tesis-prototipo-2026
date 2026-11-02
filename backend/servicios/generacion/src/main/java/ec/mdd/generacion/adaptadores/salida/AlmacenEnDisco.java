package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.puertos.salida.AlmacenDeArchivos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Los archivos en una carpeta del volumen (D-88). La clave es un UUID que genera el almacén, y un
 * archivo a medias nunca tiene clave: se escribe aparte y se mueve de una vez. La carpeta se crea
 * al guardar: arrancar el proceso no toca el disco.
 */
public final class AlmacenEnDisco implements AlmacenDeArchivos {

    private final Path raiz;

    /** Recibe la carpeta raíz, que puede no existir todavía. */
    public AlmacenEnDisco(Path raiz) {
        this.raiz = raiz;
    }

    @Override
    public String guardar(byte[] contenido) {
        String clave = UUID.randomUUID().toString();
        try {
            Files.createDirectories(raiz);
            Path temporal = Files.createTempFile(raiz, "subida-", ".tmp");
            try {
                Files.write(temporal, contenido);
                Files.move(temporal, raiz.resolve(clave), StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo guardar el archivo", error);
        }
        return clave;
    }

    @Override
    public byte[] leer(String clave) {
        try {
            return Files.readAllBytes(raiz.resolve(UUID.fromString(clave).toString()));
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer el archivo", error);
        }
    }
}
