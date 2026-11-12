package ec.mdd.firma.adaptadores.salida;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.ContenidoDelDocumento.Horas;
import ec.mdd.firma.dominio.Firmante;
import ec.mdd.firma.puertos.salida.GeneradorDePdf;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

/**
 * Genera el PDF/A-2b del documento firmable con OpenHTMLtoPDF sobre PDFBox, el camino que veraPDF
 * dio conforme en el spike B00-S01 (F13-H02, D-101):
 *
 * <ul>
 *   <li>el XHTML se arma en Java, con cada texto escapado: son tablas y un bloque fijo, sin motor
 *       de plantillas;
 *   <li>una sola fuente, DejaVu Sans, incrustada y sin negrita ni cursiva: una variante que no está
 *       incrustada rompería la conformidad;
 *   <li>el perfil sRGB de la JVM como intención de salida; el XMP de PDF/A lo pone OpenHTMLtoPDF;
 *   <li>el pie de cada página, con el periodo, la versión y el hash de contenido, es un elemento
 *       repetido de la página (CSS paginado);
 *   <li>el bloque de firmas va solo en la última página, con alturas fijas, y después PDFBox pone
 *       un campo de firma vacío con nombre sobre cada recuadro (OpenHTMLtoPDF no crea campos de
 *       firma).
 * </ul>
 *
 * <p>La conformidad se verifica en las pruebas con veraPDF, no aquí: veraPDF necesita JAXB 2.3 y no
 * entra al proceso (D-46).
 */
public final class GeneradorPdfa implements GeneradorDePdf {

    private static final String FUENTE = "DejaVu Sans";

    // La geometría del bloque de firmas, en milímetros, la misma del estilo: el margen superior y
    // el izquierdo de la página, el título, cada firmante y, dentro de él, dónde empieza su
    // recuadro.
    private static final float MARGEN_SUPERIOR = 18f;
    private static final float MARGEN_IZQUIERDO = 15f;
    private static final float TITULO = 12f;
    private static final float FIRMANTE = 60f;
    private static final float ANTES_DEL_RECUADRO = 3 * 6f + 7f;
    private static final float ANCHO_DEL_RECUADRO = 90f;
    private static final float ALTO_DEL_RECUADRO = 30f;
    private static final float PUNTOS_POR_MILIMETRO = 72f / 25.4f;

    private static final Map<String, String> ESTADOS =
            Map.of(
                    "ENVIADO", "Enviado",
                    "EN_ELABORACION", "En elaboración",
                    "DEVUELTO", "Devuelto");

    private static final Map<String, String> COMPONENTES =
            Map.of(
                    "CLASE", "Clase",
                    "OTRAS_DOCENCIA", "Otras actividades de docencia",
                    "INVESTIGACION", "Investigación",
                    "VINCULACION", "Vinculación",
                    "GESTION", "Gestión");

    private static final String ESTILO =
            """
            @page {
              size: A4;
              margin: 18mm 15mm 22mm 15mm;
              @bottom-center { content: element(pie); }
            }
            * { font-family: 'DejaVu Sans'; font-weight: normal; font-style: normal; }
            body { font-size: 8.5pt; color: #000; margin: 0; padding: 0; }
            #pie { position: running(pie); font-size: 6.5pt; text-align: center; color: #333; }
            h1 { font-size: 14pt; margin: 0 0 2mm 0; }
            h2 { font-size: 11pt; margin: 6mm 0 2mm 0; }
            h3 { font-size: 9.5pt; margin: 4mm 0 1mm 0; }
            p { margin: 0 0 2mm 0; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 2mm; }
            th, td { border: 0.5pt solid #555; padding: 1mm 1.5mm; text-align: left; }
            th { background: #e6e6e6; }
            td.n, th.n { text-align: right; }
            tr { page-break-inside: avoid; }
            .docente { page-break-inside: avoid; }
            .firmas { page-break-before: always; }
            .titulo-firmas { height: 12mm; margin: 0; padding: 0; overflow: hidden; }
            .firmante { height: 60mm; margin: 0; padding: 0; overflow: hidden; }
            .firmante p { height: 6mm; margin: 0; padding: 0; overflow: hidden; }
            .espacio { height: 7mm; margin: 0; padding: 0; }
            .recuadro {
              width: 90mm; height: 30mm; margin: 0; padding: 0; border: 0.5pt solid #555;
              box-sizing: border-box;
            }
            """;

    private final byte[] fuente;
    private final byte[] perfilDeColor;

    /** Lee la fuente de los recursos y el perfil sRGB de la JVM. */
    public GeneradorPdfa() {
        this.fuente = recurso("/fuentes/DejaVuSans.ttf");
        this.perfilDeColor = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
    }

    @Override
    public byte[] generar(ContenidoDelDocumento contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder constructor = new PdfRendererBuilder();
            constructor.useFastMode();
            constructor.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_B);
            constructor.useColorProfile(perfilDeColor);
            constructor.useFont(() -> new ByteArrayInputStream(fuente), FUENTE);
            constructor.withHtmlContent(xhtml(contenido), null);
            constructor.toStream(salida);
            constructor.run();
            return conCamposDeFirma(salida.toByteArray(), contenido);
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo generar el PDF/A", error);
        }
    }

    // Un campo de firma vacío por firmante, sobre su recuadro de la última página. Cada widget
    // lleva
    // el indicador Print y una apariencia normal vacía, que PDF/A-2b exige a los widgets visibles.
    // Se guarda en una sola pasada: es el documento original, sin actualización incremental.
    private static byte[] conCamposDeFirma(byte[] pdf, ContenidoDelDocumento contenido)
            throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDPage ultima = documento.getPage(documento.getNumberOfPages() - 1);
            float alto = ultima.getMediaBox().getHeight();
            PDAcroForm formulario = new PDAcroForm(documento);
            documento.getDocumentCatalog().setAcroForm(formulario);
            List<PDField> campos = new ArrayList<>();
            int fila = 0;
            for (Firmante firmante : contenido.firmantes()) {
                float arriba = MARGEN_SUPERIOR + TITULO + fila * FIRMANTE + ANTES_DEL_RECUADRO;
                PDRectangle caja =
                        new PDRectangle(
                                MARGEN_IZQUIERDO * PUNTOS_POR_MILIMETRO,
                                alto - (arriba + ALTO_DEL_RECUADRO) * PUNTOS_POR_MILIMETRO,
                                ANCHO_DEL_RECUADRO * PUNTOS_POR_MILIMETRO,
                                ALTO_DEL_RECUADRO * PUNTOS_POR_MILIMETRO);
                PDSignatureField campo = new PDSignatureField(formulario);
                campo.setPartialName(firmante.campo());
                PDAnnotationWidget widget = campo.getWidgets().getFirst();
                widget.setRectangle(caja);
                widget.setPage(ultima);
                widget.setPrinted(true);
                PDAppearanceStream vacia = new PDAppearanceStream(documento);
                vacia.setBBox(new PDRectangle(caja.getWidth(), caja.getHeight()));
                PDAppearanceDictionary apariencia = new PDAppearanceDictionary();
                apariencia.setNormalAppearance(vacia);
                widget.setAppearance(apariencia);
                ultima.getAnnotations().add(widget);
                campos.add(campo);
                fila++;
            }
            formulario.setFields(campos);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** El XHTML del documento, con cada texto escapado. */
    static String xhtml(ContenidoDelDocumento contenido) {
        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\"><head>")
                .append("<meta charset=\"UTF-8\"/>")
                .append("<title>Distributivo docente del periodo ")
                .append(escapar(contenido.periodo()))
                .append("</title><style>")
                .append(ESTILO)
                .append("</style></head><body>");
        html.append("<div id=\"pie\">")
                .append(escapar(contenido.periodo()))
                .append(" · versión ")
                .append(contenido.version())
                .append(" · hash ")
                .append(escapar(contenido.hashContenido()))
                .append("</div>");
        html.append("<h1>Distributivo docente del periodo ")
                .append(escapar(contenido.periodo()))
                .append("</h1><p>Versión ")
                .append(contenido.version())
                .append(" congelada para la firma.</p>");
        resumen(html, contenido);
        docentes(html, contenido);
        firmas(html, contenido);
        html.append("</body></html>");
        return html.toString();
    }

    private static void resumen(StringBuilder html, ContenidoDelDocumento contenido) {
        html.append("<h2>Resumen por carrera</h2><table><thead><tr>")
                .append(
                        "<th>Carrera</th><th>Estado</th><th class=\"n\">Paralelos con propuesta</th>");
        encabezadoDeHoras(html);
        html.append("</tr></thead><tbody>");
        for (ContenidoDelDocumento.Carrera carrera : contenido.carreras()) {
            html.append("<tr><td>")
                    .append(escapar(carrera.nombre()))
                    .append("</td><td>")
                    .append(escapar(ESTADOS.getOrDefault(carrera.estado(), carrera.estado())))
                    .append("</td><td class=\"n\">")
                    .append(carrera.propuestos())
                    .append(" de ")
                    .append(carrera.paralelos())
                    .append("</td>");
            celdasDeHoras(html, carrera.horas());
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<h2>Totales institucionales</h2><table><thead><tr>");
        encabezadoDeHoras(html);
        html.append("</tr></thead><tbody><tr>");
        celdasDeHoras(html, contenido.totales());
        html.append("</tr></tbody></table>");
    }

    private static void docentes(StringBuilder html, ContenidoDelDocumento contenido) {
        html.append("<h2>Detalle por docente</h2>");
        for (ContenidoDelDocumento.Docente docente : contenido.docentes()) {
            html.append("<div class=\"docente\"><h3>")
                    .append(escapar(docente.nombre()))
                    .append(" · ")
                    .append(escapar(docente.identificacion()))
                    .append("</h3>");
            if (!docente.asignaturas().isEmpty()) {
                html.append("<table><thead><tr><th>Asignatura</th><th>Paralelo</th>")
                        .append("<th>Carrera</th><th class=\"n\">Horas</th></tr></thead><tbody>");
                for (ContenidoDelDocumento.Asignatura asignatura : docente.asignaturas()) {
                    html.append("<tr><td>")
                            .append(escapar(asignatura.codigo()))
                            .append(" · ")
                            .append(escapar(asignatura.nombre()))
                            .append("</td><td>")
                            .append(escapar(asignatura.paralelo()))
                            .append("</td><td>")
                            .append(escapar(asignatura.carrera()))
                            .append("</td><td class=\"n\">")
                            .append(numero(asignatura.horas()))
                            .append("</td></tr>");
                }
                html.append("</tbody></table>");
            }
            if (!docente.actividades().isEmpty()) {
                html.append("<table><thead><tr><th>Actividad</th><th>Componente</th>")
                        .append("<th class=\"n\">Horas</th></tr></thead><tbody>");
                for (ContenidoDelDocumento.Actividad actividad : docente.actividades()) {
                    html.append("<tr><td>")
                            .append(escapar(actividad.nombre()))
                            .append("</td><td>")
                            .append(
                                    escapar(
                                            COMPONENTES.getOrDefault(
                                                    actividad.componente(),
                                                    actividad.componente())))
                            .append("</td><td class=\"n\">")
                            .append(numero(actividad.horas()))
                            .append("</td></tr>");
                }
                html.append("</tbody></table>");
            }
            html.append("<table><thead><tr>");
            encabezadoDeHoras(html);
            html.append("</tr></thead><tbody><tr>");
            celdasDeHoras(html, docente.horas());
            html.append("</tr></tbody></table></div>");
        }
    }

    // El bloque de firmas solo en la última página, con alturas fijas en milímetros: así cada
    // recuadro
    // cae en un rectángulo conocido, donde va su campo de firma (research §2).
    private static void firmas(StringBuilder html, ContenidoDelDocumento contenido) {
        html.append("<div class=\"firmas\"><div class=\"titulo-firmas\"><h2>Firmas</h2></div>");
        for (Firmante firmante : contenido.firmantes()) {
            html.append("<div class=\"firmante\"><p>")
                    .append(escapar(firmante.rotulo()))
                    .append("</p><p>")
                    .append(escapar(firmante.cargo()))
                    .append("</p><p>")
                    .append(escapar(firmante.nombre()))
                    .append(
                            "</p><div class=\"espacio\"></div><div class=\"recuadro\"></div></div>");
        }
        html.append("</div>");
    }

    private static void encabezadoDeHoras(StringBuilder html) {
        html.append("<th class=\"n\">Clase</th><th class=\"n\">Otras docencia</th>")
                .append("<th class=\"n\">Investigación</th><th class=\"n\">Vinculación</th>")
                .append("<th class=\"n\">Gestión</th><th class=\"n\">Total</th>");
    }

    private static void celdasDeHoras(StringBuilder html, Horas horas) {
        for (BigDecimal valor :
                new BigDecimal[] {
                    horas.clase(),
                    horas.otrasDocencia(),
                    horas.investigacion(),
                    horas.vinculacion(),
                    horas.gestion(),
                    horas.total()
                }) {
            html.append("<td class=\"n\">").append(numero(valor)).append("</td>");
        }
    }

    // Con coma decimal, como en la pantalla, y sin ceros de más.
    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
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
                        GeneradorPdfa.class.getResourceAsStream(ruta),
                        "falta el recurso " + ruta)) {
            return entrada.readAllBytes();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer " + ruta, error);
        }
    }
}
