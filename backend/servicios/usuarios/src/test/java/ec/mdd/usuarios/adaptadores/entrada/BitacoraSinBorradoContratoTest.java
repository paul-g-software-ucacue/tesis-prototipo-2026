package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Ninguna operación de la API modifica ni borra la bitácora (F17-H01, FR-009): se recorren todos
 * los contratos de {@code contratos/}. La única escritura que la nombra es {@code recibirEvento},
 * que solo agrega entradas.
 */
class BitacoraSinBorradoContratoTest {

    private static final Set<String> MODIFICAN = Set.of("put", "patch", "delete");
    private static final Set<String> ESCRIBEN = Set.of("post", "put", "patch", "delete");

    /** Una operación de un contrato. */
    private record Operacion(String servicio, String metodo, String ruta, String texto) {

        boolean nombraLaBitacora() {
            return ruta.toLowerCase(Locale.ROOT).contains("bitacora") || texto.contains("bitacora");
        }

        @Override
        public String toString() {
            return servicio + " " + metodo.toUpperCase(Locale.ROOT) + " " + ruta;
        }
    }

    @Test
    void todosLosContratos_cuandoSeRecorren_entoncesNingunaOperacionModificaLaBitacora() {
        List<Operacion> operaciones = operaciones();

        assertThat(operaciones).as("se leyeron los contratos").hasSizeGreaterThan(20);
        assertThat(operaciones)
                .filteredOn(Operacion::nombraLaBitacora)
                .filteredOn(operacion -> MODIFICAN.contains(operacion.metodo()))
                .isEmpty();
    }

    @Test
    void todosLosContratos_cuandoSeRecorren_entoncesLaUnicaEscrituraDeLaBitacoraEsRecibirEvento() {
        assertThat(operaciones())
                .filteredOn(Operacion::nombraLaBitacora)
                .filteredOn(operacion -> ESCRIBEN.contains(operacion.metodo()))
                .extracting(Operacion::toString)
                .containsExactly("usuarios POST /interno/usuarios/eventos");
    }

    private static List<Operacion> operaciones() {
        List<Operacion> operaciones = new ArrayList<>();
        try (Stream<Path> carpetas = Files.list(contratos())) {
            for (Path carpeta : carpetas.sorted().toList()) {
                Path contrato = carpeta.resolve("openapi.yaml");
                if (Files.isRegularFile(contrato)) {
                    operaciones.addAll(deContrato(carpeta.getFileName().toString(), contrato));
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("No se pudieron leer los contratos", error);
        }
        return operaciones;
    }

    private static List<Operacion> deContrato(String servicio, Path contrato) throws IOException {
        Map<String, Object> raiz;
        try (Reader lector = Files.newBufferedReader(contrato)) {
            raiz = new Yaml().load(lector);
        }
        List<Operacion> operaciones = new ArrayList<>();
        if (raiz.get("paths") instanceof Map<?, ?> rutas) {
            rutas.forEach(
                    (ruta, metodos) -> {
                        if (metodos instanceof Map<?, ?> porMetodo) {
                            porMetodo.forEach(
                                    (metodo, operacion) ->
                                            operaciones.add(
                                                    new Operacion(
                                                            servicio,
                                                            String.valueOf(metodo),
                                                            String.valueOf(ruta),
                                                            texto(operacion))));
                        }
                    });
        }
        return operaciones;
    }

    // Sin tildes ni mayúsculas, para que «Bitácora» y «EntradaBitacora» cuenten igual.
    private static String texto(Object operacion) {
        return Normalizer.normalize(String.valueOf(operacion), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static Path contratos() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null) {
            Path candidata = actual.resolve("contratos");
            if (Files.isDirectory(candidata.resolve("usuarios"))) {
                return candidata;
            }
            actual = actual.getParent();
        }
        throw new IllegalStateException("No se encontró la carpeta contratos/");
    }
}
