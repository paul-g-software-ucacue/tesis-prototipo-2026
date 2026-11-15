package ec.mdd.validacionocs.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.dominio.ArchivoDeResolucion;
import ec.mdd.validacionocs.puertos.entrada.ResolucionIlegible;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El PDF de la resolución en el volumen, por su huella (F15-H01, research §4). */
class AlmacenDeResolucionesEnDiscoTest {

    private static final byte[] PDF =
            "%PDF-1.7\nresolucion\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final String HUELLA = ArchivoDeResolucion.sha256(PDF);

    @TempDir Path raiz;

    @Test
    void dadoUnPdf_cuandoSeGuardaYSeLee_entoncesQuedaPorSuHuellaYVuelveIgual() throws Exception {
        AlmacenDeResolucionesEnDisco almacen = new AlmacenDeResolucionesEnDisco(raiz);

        almacen.guardar(HUELLA, PDF);

        Path archivo = raiz.resolve("resoluciones/" + HUELLA + ".pdf");
        assertThat(archivo).hasBinaryContent(PDF);
        assertThat(almacen.leer(HUELLA)).isEqualTo(PDF);
        // Otra vez el mismo: no se reescribe, y no quedan temporales.
        java.nio.file.attribute.FileTime antes = Files.getLastModifiedTime(archivo);
        almacen.guardar(HUELLA, PDF);
        assertThat(Files.getLastModifiedTime(archivo)).isEqualTo(antes);
        try (var archivos = Files.list(raiz.resolve("resoluciones"))) {
            assertThat(archivos).hasSize(1);
        }
    }

    @Test
    void dadoUnArchivoQueFaltaODistintoASuHuella_cuandoSeLee_entoncesEsIlegible() throws Exception {
        AlmacenDeResolucionesEnDisco almacen = new AlmacenDeResolucionesEnDisco(raiz);
        assertThatThrownBy(() -> almacen.leer(HUELLA)).isInstanceOf(ResolucionIlegible.class);

        Files.createDirectories(raiz.resolve("resoluciones"));
        Files.write(raiz.resolve("resoluciones/" + HUELLA + ".pdf"), new byte[] {1, 2, 3});
        assertThatThrownBy(() -> almacen.leer(HUELLA)).isInstanceOf(ResolucionIlegible.class);
    }

    @Test
    void dadaUnaHuellaMalFormada_cuandoSeGuardaOSeLee_entoncesSeRechazaSinSalirDeLaCarpeta() {
        AlmacenDeResolucionesEnDisco almacen = new AlmacenDeResolucionesEnDisco(raiz);

        assertThatThrownBy(() -> almacen.guardar("../otro", PDF))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacen.leer("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
