package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.ArchivoDeResolucion;
import ec.mdd.validacionocs.puertos.entrada.ResolucionIlegible;
import ec.mdd.validacionocs.puertos.salida.AlmacenDeResoluciones;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Los PDF de las resoluciones en {@code resoluciones/<sha256>.pdf} del volumen del proceso
 * (F15-H01, D-88, research §4), como Firma guarda sus documentos. El nombre por huella hace la
 * escritura idempotente: un reintento no duplica, y un archivo cuyo registro falló queda sin
 * referencia. Se escribe a un temporal y se enlaza, así que nadie lee un archivo a medias.
 */
public final class AlmacenDeResolucionesEnDisco implements AlmacenDeResoluciones {

    private static final String CARPETA = "resoluciones";
    private static final Pattern HUELLA = Pattern.compile("[0-9a-f]{64}");

    private final Path raiz;

    /** Recibe la carpeta raíz del volumen, que puede no existir todavía. */
    public AlmacenDeResolucionesEnDisco(Path raiz) {
        this.raiz = raiz;
    }

    @Override
    public void guardar(String sha256, byte[] contenido) {
        exigirHuella(sha256);
        try {
            Path carpeta = Files.createDirectories(raiz.resolve(CARPETA));
            Path destino = carpeta.resolve(sha256 + ".pdf");
            if (Files.exists(destino)) {
                return;
            }
            Path temporal = Files.createTempFile(carpeta, "resolucion-", ".tmp");
            try {
                Files.write(temporal, contenido);
                Files.createLink(destino, temporal);
            } catch (FileAlreadyExistsException otraLaGuardo) {
                // Otro registro la guardó entre la comprobación y el enlace: es la misma.
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo guardar la resolución", error);
        }
    }

    // Solo una huella: sin «..» ni otra carpeta, no sale de resoluciones/. Nunca entrega un archivo
    // que no coincide con su huella.
    @Override
    public byte[] leer(String sha256) {
        exigirHuella(sha256);
        byte[] contenido;
        try {
            contenido = Files.readAllBytes(raiz.resolve(CARPETA).resolve(sha256 + ".pdf"));
        } catch (NoSuchFileException falta) {
            throw new ResolucionIlegible();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer la resolución", error);
        }
        if (!ArchivoDeResolucion.sha256(contenido).equals(sha256)) {
            throw new ResolucionIlegible();
        }
        return contenido;
    }

    private static void exigirHuella(String sha256) {
        if (sha256 == null || !HUELLA.matcher(sha256).matches()) {
            throw new IllegalArgumentException("La huella es un SHA-256 en hexadecimal");
        }
    }
}
