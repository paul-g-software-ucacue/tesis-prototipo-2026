package ec.mdd.reporting.dominio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Los docentes de los datos de prueba de la ERS ({@code cargas/datos_prueba/06_docentes.csv},
 * ficticios; principio VIII), para las pruebas de Reporting (F20-H02, D-117).
 *
 * <p>{@link #repetidos(int)} los repite hasta el volumen que pide SC-007, con una identificación
 * derivada en el mismo rango ficticio ({@code 010999xxxx}, desde {@code 0109995000}, que ningún
 * docente original usa) y un sufijo en el nombre. Las horas son deterministas, con hasta dos
 * decimales.
 */
public final class DocentesDePrueba {

    private static final String ARCHIVO = "/reporting/datos-de-prueba/06_docentes.csv";
    // Antes de TODOS: leer() la usa durante la inicialización de la clase.
    private static final BigDecimal CUARTO = new BigDecimal("0.25");
    private static final List<DocentesDeLaVersion.Docente> TODOS = leer();

    private DocentesDePrueba() {}

    /** Los 47 docentes del archivo, en su orden, con horas de ejemplo. */
    public static List<DocentesDeLaVersion.Docente> todos() {
        return TODOS;
    }

    /**
     * @param indice la posición en el archivo
     * @return ese docente
     */
    public static DocentesDeLaVersion.Docente docente(int indice) {
        return TODOS.get(indice);
    }

    /**
     * @param cantidad cuántos docentes
     * @return los del archivo repetidos hasta esa cantidad, cada uno con su identificación
     */
    public static List<DocentesDeLaVersion.Docente> repetidos(int cantidad) {
        List<DocentesDeLaVersion.Docente> docentes = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            DocentesDeLaVersion.Docente base = TODOS.get(i % TODOS.size());
            int vuelta = i / TODOS.size();
            docentes.add(
                    vuelta == 0
                            ? base
                            : new DocentesDeLaVersion.Docente(
                                    "010999" + String.format("%04d", 5000 + i),
                                    base.nombre() + " " + (vuelta + 1),
                                    base.dedicacion().orElse(null),
                                    horas(i)));
        }
        return docentes;
    }

    private static List<DocentesDeLaVersion.Docente> leer() {
        try (InputStream entrada = DocentesDePrueba.class.getResourceAsStream(ARCHIVO);
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String> encabezado = Arrays.asList(lector.readLine().split(",", -1));
            int identificacion = encabezado.indexOf("numero_identificacion");
            int dedicacion = encabezado.indexOf("dedicacion");
            int estado = encabezado.indexOf("estado");
            List<DocentesDeLaVersion.Docente> docentes = new ArrayList<>();
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] celdas = linea.split(",", -1);
                // Un docente en licencia al congelar queda sin dedicación («sin dato»).
                Dedicacion suya =
                        "ACTIVO".equals(celdas[estado])
                                ? Dedicacion.valueOf(celdas[dedicacion])
                                : null;
                docentes.add(
                        new DocentesDeLaVersion.Docente(
                                celdas[identificacion],
                                String.join(
                                                " ",
                                                celdas[encabezado.indexOf("primer_apellido")],
                                                celdas[encabezado.indexOf("segundo_apellido")],
                                                celdas[encabezado.indexOf("primer_nombre")],
                                                celdas[encabezado.indexOf("segundo_nombre")])
                                        .strip(),
                                suya,
                                horas(docentes.size())));
            }
            return List.copyOf(docentes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HorasPorComponente horas(int i) {
        return new HorasPorComponente(
                BigDecimal.valueOf(6 + (i % 7) * 2L).add(i % 2 == 0 ? BigDecimal.ZERO : CUARTO),
                new BigDecimal("2.50"),
                i % 3 == 0 ? new BigDecimal("3") : BigDecimal.ZERO,
                i % 5 == 0 ? new BigDecimal("2") : BigDecimal.ZERO,
                i % 11 == 0 ? new BigDecimal("10") : BigDecimal.ZERO);
    }
}
