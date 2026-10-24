package ec.mdd.cargas.adaptadores.salida;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los 10 CSV de los datos de prueba de la ERS ({@code cargas/datos_prueba/}, sintéticos,
 * RNF-SEG-06), copiados a los recursos de prueba, con su número de registros.
 */
public final class DatosDePrueba {

    /** Cada archivo con su número de registros. */
    public static final Map<String, Integer> REGISTROS = new LinkedHashMap<>();

    static {
        REGISTROS.put("01_estructura_organica.csv", 24);
        REGISTROS.put("02_carreras.csv", 7);
        REGISTROS.put("03_ciclos_academicos.csv", 2);
        REGISTROS.put("04_asignaturas_malla.csv", 300);
        REGISTROS.put("05_oferta_periodo.csv", 165);
        REGISTROS.put("06_docentes.csv", 47);
        REGISTROS.put("07_politica_carga_horaria.csv", 22);
        REGISTROS.put("08_cargos_autoridades.csv", 21);
        REGISTROS.put("09_proyectos_practicas.csv", 11);
        REGISTROS.put("10_matriculados_carrera.csv", 7);
    }

    private DatosDePrueba() {}

    /** El contenido del archivo, tal como está. */
    public static byte[] contenido(String archivo) {
        try (InputStream entrada =
                DatosDePrueba.class.getResourceAsStream("/cargas/datos-de-prueba/" + archivo)) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
