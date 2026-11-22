package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * El CSV del reporte individual (RF-48, F20-H01, D-115) y del general (F20-H02, D-117): los mismos
 * datos de su PDF, en una tabla regular que se puede filtrar y concatenar.
 *
 * <ul>
 *   <li>UTF-8 con BOM, separador punto y coma y fin de línea CRLF: con la configuración regional de
 *       Ecuador (es-EC), una hoja de cálculo lo abre en columnas y lee bien las tildes.
 *   <li>Las horas con dos decimales y coma decimal ({@code 12,50}), sin comillas, como números.
 *   <li>Todo texto va entre comillas dobles, con las comillas de adentro duplicadas.
 *   <li>Un texto que empieza con {@code =}, {@code +}, {@code -}, {@code @}, tabulación o retorno
 *       de carro lleva un apóstrofo delante, para que la hoja de cálculo no lo interprete como
 *       fórmula (RNF-SEG-04, OWASP).
 *   <li>El individual es largo: una fila por asignatura y por actividad, y seis de totales (los
 *       cinco componentes, también los que suman 0, y el total), porque su unidad es la asignación.
 *   <li>El general es ancho: una fila por docente con una columna por componente y su total, y una
 *       fila de totales institucionales al final, porque su unidad es el docente. Así tiene la
 *       misma forma que su PDF y se compara directo con el CSV para SIGA (RF-52).
 *   <li>No lleva quién lo generó ni cuándo: dos CSV de la misma versión son idénticos.
 * </ul>
 */
public final class EscritorCsv {

    private static final String SEPARADOR = ";";
    private static final String FIN_DE_LINEA = "\r\n";
    private static final String BOM = "﻿";
    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter MOMENTO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ECUADOR);
    private static final List<String> ENCABEZADO =
            List.of(
                    "Periodo",
                    "Versión",
                    "Hash de contenido",
                    "Publicada el",
                    "Identificación",
                    "Docente",
                    "Dedicación",
                    "Tipo",
                    "Carrera o unidad",
                    "Código",
                    "Asignatura o actividad",
                    "Paralelo",
                    "Componente",
                    "Horas");

    private static final List<String> ENCABEZADO_GENERAL =
            List.of(
                    "Periodo",
                    "Versión",
                    "Hash de contenido",
                    "Publicada el",
                    "Tipo",
                    "Identificación",
                    "Docente",
                    "Dedicación",
                    Componente.CLASE.etiqueta(),
                    Componente.OTRAS_DOCENCIA.etiqueta(),
                    Componente.INVESTIGACION.etiqueta(),
                    Componente.VINCULACION.etiqueta(),
                    Componente.GESTION.etiqueta(),
                    "Total");

    private EscritorCsv() {}

    /**
     * @param reporte el reporte individual
     * @return los bytes del CSV, en UTF-8 con BOM
     */
    public static byte[] escribir(ReporteIndividual reporte) {
        StringBuilder csv = new StringBuilder(BOM);
        linea(csv, ENCABEZADO.stream().map(EscritorCsv::texto).toList());
        List<String> comun = comun(reporte);
        for (FilaDeAsignatura una : reporte.asignaturas()) {
            fila(
                    csv,
                    comun,
                    "ASIGNATURA",
                    una.carrera(),
                    una.codigo(),
                    una.nombre(),
                    una.paralelo(),
                    Componente.CLASE.etiqueta(),
                    una.horas());
        }
        for (FilaDeActividad una : reporte.actividades()) {
            fila(
                    csv,
                    comun,
                    "ACTIVIDAD",
                    una.unidad().orElse(""),
                    una.codigo(),
                    una.nombre(),
                    "",
                    una.componente().etiqueta(),
                    una.horas());
        }
        for (Componente componente : Componente.values()) {
            fila(
                    csv,
                    comun,
                    "TOTAL",
                    "",
                    "",
                    "",
                    "",
                    componente.etiqueta(),
                    reporte.horas().de(componente));
        }
        fila(csv, comun, "TOTAL", "", "", "", "", "Total", reporte.horas().total());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @param reporte el reporte general
     * @return los bytes del CSV, en UTF-8 con BOM
     */
    public static byte[] escribir(ReporteGeneral reporte) {
        StringBuilder csv = new StringBuilder(BOM);
        linea(csv, ENCABEZADO_GENERAL.stream().map(EscritorCsv::texto).toList());
        List<String> version = deLaVersion(reporte.publicacion());
        for (FilaDelDocente una : reporte.docentes()) {
            filaGeneral(
                    csv,
                    version,
                    "DOCENTE",
                    una.identificacion(),
                    una.nombre(),
                    dedicacion(una.dedicacion()),
                    una.horas());
        }
        filaGeneral(csv, version, "TOTAL", "", "Total institucional", "", reporte.totales());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Lo que se repite en cada fila: la versión y el docente, para que cada fila se entienda sola.
    private static List<String> comun(ReporteIndividual reporte) {
        DatosDelDocente docente = reporte.docente();
        List<String> comun = new ArrayList<>(deLaVersion(reporte.publicacion()));
        comun.add(texto(docente.identificacion()));
        comun.add(texto(docente.nombre()));
        comun.add(texto(dedicacion(docente.dedicacion())));
        return comun;
    }

    private static List<String> deLaVersion(Publicacion publicacion) {
        return List.of(
                texto(publicacion.periodo()),
                String.valueOf(publicacion.version()),
                texto(publicacion.hash()),
                texto(MOMENTO.format(publicacion.publicadaEn())));
    }

    private static String dedicacion(Optional<Dedicacion> dedicacion) {
        return dedicacion.map(Dedicacion::etiqueta).orElse("sin dato");
    }

    private static void filaGeneral(
            StringBuilder csv,
            List<String> version,
            String tipo,
            String identificacion,
            String docente,
            String dedicacion,
            HorasPorComponente horas) {
        List<String> celdas = new ArrayList<>(version);
        celdas.add(texto(tipo));
        celdas.add(texto(identificacion));
        celdas.add(texto(docente));
        celdas.add(texto(dedicacion));
        for (Componente componente : Componente.values()) {
            celdas.add(horas(horas.de(componente)));
        }
        celdas.add(horas(horas.total()));
        linea(csv, celdas);
    }

    private static void fila(
            StringBuilder csv,
            List<String> comun,
            String tipo,
            String unidad,
            String codigo,
            String nombre,
            String paralelo,
            String componente,
            BigDecimal horas) {
        List<String> celdas = new ArrayList<>(comun);
        celdas.add(texto(tipo));
        celdas.add(texto(unidad));
        celdas.add(texto(codigo));
        celdas.add(texto(nombre));
        celdas.add(texto(paralelo));
        celdas.add(texto(componente));
        celdas.add(horas(horas));
        linea(csv, celdas);
    }

    private static void linea(StringBuilder csv, List<String> celdas) {
        csv.append(String.join(SEPARADOR, celdas)).append(FIN_DE_LINEA);
    }

    /** Un texto entre comillas, neutralizado si empieza como una fórmula. */
    static String texto(String valor) {
        String seguro = comoFormula(valor) ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    private static boolean comoFormula(String valor) {
        if (valor.isEmpty()) {
            return false;
        }
        char primero = valor.charAt(0);
        return primero == '='
                || primero == '+'
                || primero == '-'
                || primero == '@'
                || primero == '\t'
                || primero == '\r';
    }

    /** Las horas con dos decimales y coma decimal. */
    static String horas(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString().replace('.', ',');
    }
}
