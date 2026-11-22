package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.reporting.adaptadores.entrada.api.CampoDelLayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.ColumnaDelLayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.LayoutSiga;
import ec.mdd.reporting.adaptadores.entrada.api.LayoutsSiga;
import ec.mdd.reporting.adaptadores.entrada.api.SigaApi;
import ec.mdd.reporting.adaptadores.entrada.api.TraduccionDeCodigo;
import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.LayoutExportacion;
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

    private static final String PERMISO = "hasAuthority('PERMISO_PUBLICACION:APROBACION')";
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final MediaType YAML = new MediaType("text", "yaml", StandardCharsets.UTF_8);

    /** 256 KB: lo que admite el archivo de mapeo. */
    static final long TAMANO_MAXIMO = 262_144;

    private final GenerarCsvSiga generar;
    private final GestionarLayoutSiga layouts;

    SigaControlador(GenerarCsvSiga generar, GestionarLayoutSiga layouts) {
        this.generar = generar;
        this.layouts = layouts;
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<Resource> generarCsvSiga(String periodo) {
        ArchivoSiga archivo = generar.generar(periodo, ReportesControlador.quien());
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(archivo.nombre())
                                .build()
                                .toString())
                .cacheControl(CacheControl.noStore())
                .body(new ByteArrayResource(archivo.contenido()));
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
