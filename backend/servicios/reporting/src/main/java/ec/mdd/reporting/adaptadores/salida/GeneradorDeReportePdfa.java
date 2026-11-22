package ec.mdd.reporting.adaptadores.salida;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.FilaDeActividad;
import ec.mdd.reporting.dominio.FilaDeAsignatura;
import ec.mdd.reporting.dominio.FilaDelDocente;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.puertos.salida.GeneradorDePdf;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * El PDF/A-2b de los reportes individual (F20-H01, D-115) y general (F20-H02, D-117) con
 * openhtmltopdf y PDFBox, por el mismo camino del documento firmable (D-101): la fuente DejaVu Sans
 * incrustada y el perfil sRGB. Cada reporte arma su XHTML; el renderizado, la página y el pie son
 * comunes.
 *
 * <p>«No editable» es PDF/A-2b sin campos de formulario, sin anotaciones ni JavaScript, y sin
 * cifrar: PDF/A no admite cifrado, y la integridad la da el hash de contenido de la versión en el
 * pie de cada página (aclaración del 2026-11-21). Los encabezados de las tablas se repiten en cada
 * página.
 *
 * <p>El general va en A4 horizontal, porque su tabla tiene nueve columnas. Su fila «Total
 * institucional» es la última del cuerpo de la tabla y no un {@code tfoot}: openhtmltopdf repite el
 * {@code tfoot} en cada página, y los totales deben aparecer una sola vez, al final. Se distingue
 * con el fondo, como el total del individual: solo la variante regular de la fuente está
 * incrustada, y una negrita sin su fuente no sería PDF/A.
 */
public final class GeneradorDeReportePdfa implements GeneradorDePdf {

    private static final String FUENTE = "DejaVu Sans";
    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter MOMENTO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ECUADOR);

    private static final String ESTILO =
            """
            @page {
              size: TAMANO;
              margin: 18mm 15mm 24mm 15mm;
              @bottom-left { content: element(pie); }
              @bottom-right {
                content: "Página " counter(page) " de " counter(pages);
                font-family: 'DejaVu Sans'; font-size: 6.5pt; color: #333;
              }
            }
            * { font-family: 'DejaVu Sans'; font-weight: normal; font-style: normal; }
            body { font-size: 9pt; color: #000; margin: 0; padding: 0; }
            #pie { position: running(pie); font-size: 6.5pt; color: #333; }
            h1 { font-size: 14pt; margin: 0 0 2mm 0; }
            h2 { font-size: 11pt; margin: 6mm 0 2mm 0; }
            p { margin: 0 0 1.5mm 0; }
            table {
              width: 100%; border-collapse: collapse; margin-bottom: 2mm;
              table-layout: fixed; -fs-table-paginate: paginate;
            }
            thead { display: table-header-group; }
            th, td {
              border: 0.5pt solid #555; padding: 1mm 1.5mm; text-align: left;
              word-wrap: break-word;
            }
            th { background: #e6e6e6; }
            td.n, th.n { text-align: right; white-space: nowrap; }
            tr { page-break-inside: avoid; }
            tr.total td, tr.total th { background: #f2f2f2; }
            .totales { width: 60%; }
            th.c { white-space: normal; font-size: 7.5pt; }
            """;

    private final byte[] fuente;
    private final byte[] perfilDeColor;

    /** Lee la fuente de los recursos y el perfil sRGB de la JVM. */
    public GeneradorDeReportePdfa() {
        this.fuente = recurso("/fuentes/DejaVuSans.ttf");
        this.perfilDeColor = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
    }

    @Override
    public byte[] generar(ReporteIndividual reporte) {
        return renderizar(xhtml(reporte));
    }

    @Override
    public byte[] generar(ReporteGeneral reporte) {
        return renderizar(xhtml(reporte));
    }

    private byte[] renderizar(String xhtml) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder constructor = new PdfRendererBuilder();
            constructor.useFastMode();
            constructor.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_B);
            constructor.useColorProfile(perfilDeColor);
            constructor.useFont(() -> new ByteArrayInputStream(fuente), FUENTE);
            constructor.withHtmlContent(xhtml, null);
            constructor.toStream(salida);
            constructor.run();
            return salida.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo generar el PDF/A del reporte", error);
        }
    }

    /** El XHTML del reporte, con cada texto escapado. */
    static String xhtml(ReporteIndividual reporte) {
        Publicacion publicacion = reporte.publicacion();
        DatosDelDocente docente = reporte.docente();
        StringBuilder html = new StringBuilder(16 * 1024);
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\"><head>")
                .append("<meta charset=\"UTF-8\"/><title>Reporte individual de distributivo · ")
                .append(escapar(docente.nombre()))
                .append(" · ")
                .append(escapar(publicacion.periodo()))
                .append("</title><style>")
                .append(ESTILO.replace("TAMANO", "A4"))
                .append("</style></head><body>");
        pie(html, publicacion);
        html.append("<h1>Reporte individual de distributivo</h1>")
                .append("<p>Periodo ")
                .append(escapar(publicacion.periodo()))
                .append(", versión publicada ")
                .append(publicacion.version())
                .append(", publicada el ")
                .append(MOMENTO.format(publicacion.publicadaEn()))
                .append(" por ")
                .append(escapar(publicacion.publicadaPor()))
                .append(".</p>")
                .append("<p>Hash de contenido: ")
                .append(escapar(publicacion.hash()))
                .append("</p>");
        html.append("<h2>")
                .append(escapar(docente.nombre()))
                .append("</h2><p>Identificación: ")
                .append(escapar(docente.identificacion()))
                .append(" · Dedicación: ")
                .append(escapar(docente.dedicacion().map(Dedicacion::etiqueta).orElse("sin dato")));
        docente.horasContratadas()
                .ifPresent(horas -> html.append(" · ").append(horas).append(" horas contratadas"));
        html.append("</p>");
        asignaturas(html, reporte);
        actividades(html, reporte);
        totales(html, reporte);
        html.append("<p>Generado el ")
                .append(MOMENTO.format(reporte.generadoEn()))
                .append(" por ")
                .append(escapar(reporte.generadoPor()))
                .append(".</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private static void asignaturas(StringBuilder html, ReporteIndividual reporte) {
        html.append("<h2>Asignaturas</h2>");
        if (reporte.asignaturas().isEmpty()) {
            html.append("<p>No tiene asignaturas en esta versión.</p>");
            return;
        }
        // Anchos fijos: un nombre largo se parte en líneas sin aplastar las demás columnas.
        html.append("<table><colgroup><col style=\"width: 18%\"/><col style=\"width: 19%\"/>")
                .append("<col style=\"width: 37%\"/><col style=\"width: 15%\"/>")
                .append("<col style=\"width: 11%\"/></colgroup>")
                .append("<thead><tr><th>Carrera</th><th>Código</th><th>Asignatura</th>")
                .append("<th>Paralelo</th><th class=\"n\">Horas</th></tr></thead><tbody>");
        for (FilaDeAsignatura una : reporte.asignaturas()) {
            html.append("<tr><td>")
                    .append(escapar(una.carrera()))
                    .append("</td><td>")
                    .append(escapar(una.codigo()))
                    .append("</td><td>")
                    .append(escapar(una.nombre()))
                    .append("</td><td>")
                    .append(escapar(una.paralelo()))
                    .append("</td><td class=\"n\">")
                    .append(horas(una.horas()))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private static void actividades(StringBuilder html, ReporteIndividual reporte) {
        html.append("<h2>Actividades</h2>");
        if (reporte.actividades().isEmpty()) {
            html.append("<p>No tiene actividades en esta versión.</p>");
            return;
        }
        html.append("<table><colgroup><col style=\"width: 26%\"/><col style=\"width: 36%\"/>")
                .append("<col style=\"width: 26%\"/><col style=\"width: 12%\"/></colgroup>")
                .append("<thead><tr><th>Componente</th><th>Actividad</th>")
                .append("<th>Unidad o proyecto</th><th class=\"n\">Horas</th></tr></thead><tbody>");
        for (FilaDeActividad una : reporte.actividades()) {
            html.append("<tr><td>")
                    .append(escapar(una.componente().etiqueta()))
                    .append("</td><td>")
                    .append(escapar(una.nombre()))
                    .append("</td><td>")
                    .append(escapar(una.unidad().orElse("—")))
                    .append("</td><td class=\"n\">")
                    .append(horas(una.horas()))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private static void totales(StringBuilder html, ReporteIndividual reporte) {
        html.append("<h2>Horas por componente</h2>")
                .append("<table class=\"totales\"><colgroup><col style=\"width: 75%\"/>")
                .append("<col style=\"width: 25%\"/></colgroup><thead><tr><th>Componente</th>")
                .append("<th class=\"n\">Horas</th></tr></thead><tbody>");
        for (Componente componente : Componente.values()) {
            html.append("<tr><td>")
                    .append(escapar(componente.etiqueta()))
                    .append("</td><td class=\"n\">")
                    .append(horas(reporte.horas().de(componente)))
                    .append("</td></tr>");
        }
        html.append("<tr class=\"total\"><th>Total</th><th class=\"n\">")
                .append(horas(reporte.horas().total()))
                .append("</th></tr></tbody></table>");
    }

    /** El XHTML del reporte general, con cada texto escapado. */
    static String xhtml(ReporteGeneral reporte) {
        Publicacion publicacion = reporte.publicacion();
        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\"><head>")
                .append("<meta charset=\"UTF-8\"/><title>Reporte general del distributivo · ")
                .append(escapar(publicacion.periodo()))
                .append("</title><style>")
                .append(ESTILO.replace("TAMANO", "A4 landscape"))
                .append("</style></head><body>");
        pie(html, publicacion);
        html.append("<h1>Reporte general del distributivo</h1>")
                .append("<p>Periodo ")
                .append(escapar(publicacion.periodo()))
                .append(", versión publicada ")
                .append(publicacion.version())
                .append(", publicada el ")
                .append(MOMENTO.format(publicacion.publicadaEn()))
                .append(" por ")
                .append(escapar(publicacion.publicadaPor()))
                .append(". ")
                .append(reporte.cantidadDeDocentes())
                .append(" docentes.</p>")
                .append("<p>Hash de contenido: ")
                .append(escapar(publicacion.hash()))
                .append("</p>");
        // Anchos fijos: un nombre largo se parte en líneas sin aplastar las columnas de horas.
        // Ni «Investigación» ni «Tiempo completo» deben partirse: los encabezados de horas van en
        // letra más chica y la dedicación tiene su ancho (visto en el PDF real del perfil).
        html.append("<table><colgroup><col style=\"width: 10%\"/><col style=\"width: 25%\"/>")
                .append("<col style=\"width: 12%\"/>");
        for (int i = 0; i < Componente.values().length + 1; i++) {
            html.append("<col style=\"width: 8.83%\"/>");
        }
        html.append("</colgroup><thead><tr><th>Identificación</th><th>Docente</th>")
                .append("<th>Dedicación</th>");
        for (Componente componente : Componente.values()) {
            html.append("<th class=\"n c\">")
                    .append(escapar(componente.etiqueta()))
                    .append("</th>");
        }
        html.append("<th class=\"n c\">Total</th></tr></thead><tbody>");
        for (FilaDelDocente una : reporte.docentes()) {
            html.append("<tr><td>")
                    .append(escapar(una.identificacion()))
                    .append("</td><td>")
                    .append(escapar(una.nombre()))
                    .append("</td><td>")
                    .append(escapar(una.dedicacion().map(Dedicacion::etiqueta).orElse("sin dato")))
                    .append("</td>");
            horasDeLaFila(html, una.horas(), "td");
            html.append("</tr>");
        }
        // La última fila del cuerpo, no un tfoot: aparece una sola vez (ver la clase).
        html.append("<tr class=\"total\"><th colspan=\"3\">Total institucional</th>");
        horasDeLaFila(html, reporte.totales(), "th");
        html.append("</tr></tbody></table>");
        html.append("<p>Generado el ")
                .append(MOMENTO.format(reporte.generadoEn()))
                .append(" por ")
                .append(escapar(reporte.generadoPor()))
                .append(".</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private static void horasDeLaFila(StringBuilder html, HorasPorComponente horas, String celda) {
        for (Componente componente : Componente.values()) {
            html.append('<')
                    .append(celda)
                    .append(" class=\"n\">")
                    .append(horas(horas.de(componente)))
                    .append("</")
                    .append(celda)
                    .append('>');
        }
        html.append('<')
                .append(celda)
                .append(" class=\"n\">")
                .append(horas(horas.total()))
                .append("</")
                .append(celda)
                .append('>');
    }

    // El periodo, la versión y su hash, en el pie de cada página; el número de página va aparte.
    private static void pie(StringBuilder html, Publicacion publicacion) {
        html.append("<div id=\"pie\">")
                .append(escapar(publicacion.periodo()))
                .append(" · versión ")
                .append(publicacion.version())
                .append(" · hash ")
                .append(escapar(publicacion.hash()))
                .append("</div>");
    }

    // Con dos decimales y coma decimal, como el CSV.
    private static String horas(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString().replace('.', ',');
    }

    /** Escapa un texto para XHTML. */
    static String escapar(String texto) {
        StringBuilder escapado = new StringBuilder(texto.length() + 16);
        for (char letra : texto.toCharArray()) {
            switch (letra) {
                case '&' -> escapado.append("&amp;");
                case '<' -> escapado.append("&lt;");
                case '>' -> escapado.append("&gt;");
                case '"' -> escapado.append("&quot;");
                case '\'' -> escapado.append("&#39;");
                default -> escapado.append(letra);
            }
        }
        return escapado.toString();
    }

    private static byte[] recurso(String ruta) {
        try (InputStream entrada =
                Objects.requireNonNull(
                        GeneradorDeReportePdfa.class.getResourceAsStream(ruta),
                        "falta el recurso " + ruta)) {
            return entrada.readAllBytes();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer " + ruta, error);
        }
    }
}
