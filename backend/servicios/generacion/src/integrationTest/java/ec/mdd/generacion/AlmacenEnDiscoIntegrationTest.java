package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.adaptadores.salida.AlmacenEnDisco;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Los archivos en disco, con una carpeta real (FR-016, FR-017, D-88). */
class AlmacenEnDiscoIntegrationTest {

    private static final byte[] CONTENIDO = "%PDF-1.7 prueba".getBytes(StandardCharsets.US_ASCII);

    @TempDir private Path carpeta;

    @Test
    void dadoUnArchivo_cuandoSeGuarda_entoncesSeLeeIgual() {
        AlmacenEnDisco almacen = new AlmacenEnDisco(carpeta);

        assertThat(almacen.leer(almacen.guardar(CONTENIDO))).isEqualTo(CONTENIDO);
    }

    @Test
    void dadosDosArchivos_cuandoSeGuardan_entoncesTienenClavesDistintas() {
        AlmacenEnDisco almacen = new AlmacenEnDisco(carpeta);

        assertThat(almacen.guardar(CONTENIDO)).isNotEqualTo(almacen.guardar(CONTENIDO));
    }

    @Test
    void alGuardar_cuandoTermina_entoncesSoloQuedaElArchivoCompleto() throws IOException {
        String clave = new AlmacenEnDisco(carpeta).guardar(CONTENIDO);

        try (Stream<Path> archivos = Files.list(carpeta)) {
            assertThat(archivos.map(uno -> uno.getFileName().toString())).containsExactly(clave);
        }
    }

    @Test
    void unaRaizQueNoExiste_cuandoSeCrea_entoncesNoLaToca() {
        Path raiz = carpeta.resolve("todavia/no");

        new AlmacenEnDisco(raiz);

        assertThat(raiz).doesNotExist();
    }

    @Test
    void unaRaizQueNoExiste_cuandoSeGuarda_entoncesLaCrea() {
        AlmacenEnDisco almacen = new AlmacenEnDisco(carpeta.resolve("todavia/no"));

        assertThat(almacen.leer(almacen.guardar(CONTENIDO))).isEqualTo(CONTENIDO);
    }

    @Test
    void unaClaveQueSaleDeLaCarpeta_cuandoSeLee_entoncesLaRechaza() {
        AlmacenEnDisco almacen = new AlmacenEnDisco(carpeta);

        assertThatThrownBy(() -> almacen.leer("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaClaveQueNoExiste_cuandoSeLee_entoncesFalla() {
        AlmacenEnDisco almacen = new AlmacenEnDisco(carpeta);

        assertThatThrownBy(() -> almacen.leer(UUID.randomUUID().toString()))
                .isInstanceOf(UncheckedIOException.class);
    }
}
