package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.reporting.adaptadores.entrada.api.CampoDelLayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.ColumnaDelLayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.ExportacionesSiga;
import ec.mdd.reporting.adaptadores.entrada.api.LayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.LayoutsSiga;
import ec.mdd.reporting.adaptadores.entrada.api.SigaApi;
import ec.mdd.reporting.adaptadores.entrada.api.TraduccionDeCodigo;
import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.CsvExportado;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.puertos.entrada.ConsultarExportacionesSiga;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El CSV para SIGA y el layout de exportación (F19-H01, RF-52, D-119). Las cuatro operaciones son
 * solo de la Secretaría general académica ({@code PUBLICACION:APROBACION}, aclaración del
 * 2026-11-22). {@code no-store}: el CSV lleva datos personales.
 */
@RestController
class SigaControlador implements SigaApi {

    /** El encabezado con el SHA-256 del CSV entregado (F19-H02, D-120). */
    static final String SUMA_VERIFICACION = "X-Suma-Verificacion";

    private static final String PERMISO = "hasAuthority('PERMISO_PUBLICACION:APROBACION')";
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final MediaType YAML = new MediaType("text", "yaml", StandardCharsets.UTF_8);

    /** 256 KB: lo que admite el archivo de mapeo. */
    static final long TAMANO_MAXIMO = 262_144;

    private final GenerarCsvSiga generar;
    private final GestionarLayoutSiga layouts;
    private final ConsultarExportacionesSiga exportaciones;

    SigaControlador(
            GenerarCsvSiga generar,
            GestionarLayoutSiga layouts,
            ConsultarExportacionesSiga exportaciones) {
        this.generar = generar;
        this.layouts = layouts;
        this.exportaciones = exportaciones;
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<Resource> generarCsvSiga(String periodo) {
        CsvExportado exportado = generar.generar(periodo, ReportesControlador.quien());
        ArchivoSiga archivo = exportado.archivo();
        return ResponseEntity.ok()
                .contentType(CSV)
                // F19-H02 (D-120): la suma de los bytes del cuerpo, la del registro.
                .header(SUMA_VERIFICACION, exportado.exportacion().suma().valor())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(archivo.nombre())
                                .build()
                                .toString())
                .cacheControl(CacheControl.noStore())
                .body(new ByteArrayResource(archivo.contenido()));
    }

    // F19-H02 (D-120, FR-018): las exportaciones del periodo, de la más reciente a la más antigua.
    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<ExportacionesSiga> consultarExportacionesSiga(String periodo) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        new ExportacionesSiga(
                                periodo,
                                exportaciones.delPeriodo(periodo).stream()
                                        .map(SigaControlador::exportacion)
                                        .toList()));
    }

    private static ec.mdd.reporting.adaptadores.entrada.api.ExportacionSiga exportacion(
            ExportacionSiga exportacion) {
        return new ec.mdd.reporting.adaptadores.entrada.api.ExportacionSiga(
                exportacion.id(),
                exportacion.version(),
                exportacion.layout(),
                exportacion.nombre(),
                exportacion.filas(),
                exportacion.suma().valor(),
                exportacion.usuario(),
                exportacion.momento().atOffset(ZoneOffset.UTC));
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<LayoutsSiga> consultarLayoutsSiga() {
        GestionarLayoutSiga.LayoutsSiga consultados = layouts.consultar();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        new LayoutsSiga(
                                respuesta(consultados.vigente()),
                                consultados.anteriores().stream()
                                        .map(SigaControlador::respuesta)
                                        .toList()));
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<String> descargarPlantillaLayoutSiga() {
        return ResponseEntity.ok()
                .contentType(YAML)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("layout-siga-plantilla.yaml")
                                .build()
                                .toString())
                .body(layouts.plantilla());
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<LayoutSiga> cargarLayoutSiga(MultipartFile archivo) {
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ArchivoDemasiadoGrande(archivo.getSize());
        }
        try {
            return ResponseEntity.ok(
                    respuesta(layouts.cargar(archivo.getBytes(), ReportesControlador.quien())));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static LayoutSiga respuesta(LayoutExportacion layout) {
        LayoutSiga respuesta =
                new LayoutSiga()
                        .numero(layout.numero())
                        .porDefecto(layout.esPorDefecto())
                        .sha256(layout.sha256())
                        .columnas(
                                layout.columnas().stream()
                                        .map(SigaControlador::respuesta)
                                        .toList());
        layout.cargadoPor().ifPresent(respuesta::cargadoPor);
        layout.cargadoEn()
                .ifPresent(momento -> respuesta.cargadoEn(momento.atOffset(ZoneOffset.UTC)));
        return respuesta;
    }

    private static ColumnaDelLayoutSiga respuesta(ColumnaDelLayout columna) {
        ColumnaDelLayoutSiga respuesta =
                new ColumnaDelLayoutSiga(
                        columna.nombre(), CampoDelLayoutSiga.fromValue(columna.campo().clave()));
        if (!columna.traduccion().isEmpty()) {
            respuesta.traduccion(
                    columna.traduccion().entrySet().stream()
                            .map(par -> new TraduccionDeCodigo(par.getKey(), par.getValue()))
                            .toList());
        }
        return respuesta;
    }
}
