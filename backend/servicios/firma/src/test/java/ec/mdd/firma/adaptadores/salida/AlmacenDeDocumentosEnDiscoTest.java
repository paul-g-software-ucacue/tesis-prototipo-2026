package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Los PDF del documento firmable fuera de la base, nombrados por su hash (F13-H02, FR-009). */
class AlmacenDeDocumentosEnDiscoTest {

    private static final byte[] PDF = "%PDF-1.7 documento".getBytes(StandardCharsets.US_ASCII);
    private static final String SHA = "a".repeat(64);

    @TempDir Path raiz;

    @Test
    void dadoUnPdf_cuandoSeGuarda_entoncesQuedaEnDocumentosConElNombreDeSuHash() throws Exception {
        String archivo = new AlmacenDeDocumentosEnDisco(raiz).guardar(PDF, SHA);

        assertThat(archivo).isEqualTo("documentos/" + SHA + ".pdf");
        assertThat(Files.readAllBytes(raiz.resolve(archivo))).isEqualTo(PDF);
        try (var restos = Files.list(raiz.resolve("documentos"))) {
            assertThat(restos).as("sin temporales").hasSize(1);
        }
    }

    @Test
    void dadoUnArchivoYaGuardado_cuandoSeGuardaOtraVez_entoncesNoLoEscribe() throws Exception {
        AlmacenDeDocumentosEnDisco almacen = new AlmacenDeDocumentosEnDisco(raiz);
        String archivo = almacen.guardar(PDF, SHA);
        Path guardado = raiz.resolve(archivo);
        FileTime antes = FileTime.from(Instant.parse("2026-01-01T00:00:00Z"));
        Files.setLastModifiedTime(guardado, antes);

        assertThat(almacen.guardar(PDF, SHA)).isEqualTo(archivo);
        assertThat(Files.getLastModifiedTime(guardado)).isEqualTo(antes);
    }

    @Test
    void dadoUnArchivoConEseNombre_cuandoSeGuarda_entoncesNuncaLoSobrescribe() throws Exception {
        Path documentos = Files.createDirectories(raiz.resolve("documentos"));
        Path existente = documentos.resolve(SHA + ".pdf");
        Files.write(existente, "otro contenido".getBytes(StandardCharsets.US_ASCII));

        new AlmacenDeDocumentosEnDisco(raiz).guardar(PDF, SHA);

        assertThat(Files.readString(existente)).isEqualTo("otro contenido");
    }

    @Test
    void dadaUnaCarpetaSinPermisoDeEscritura_cuandoSeGuarda_entoncesUncheckedIOException()
            throws Exception {
        Path documentos = Files.createDirectories(raiz.resolve("documentos"));
        Files.setPosixFilePermissions(documentos, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            assertThatThrownBy(() -> new AlmacenDeDocumentosEnDisco(raiz).guardar(PDF, SHA))
                    .isInstanceOf(UncheckedIOException.class);
        } finally {
            Files.setPosixFilePermissions(documentos, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    void dadoUnHashQueNoEsHexadecimal_cuandoSeGuarda_entoncesSeRechazaSinTocarElDisco() {
        assertThatThrownBy(() -> new AlmacenDeDocumentosEnDisco(raiz).guardar(PDF, "../otro"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(raiz.resolve("documentos")).doesNotExist();
    }
}
