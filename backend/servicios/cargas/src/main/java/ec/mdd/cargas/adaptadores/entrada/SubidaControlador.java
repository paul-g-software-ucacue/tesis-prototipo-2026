package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.FormatoDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.LecturaDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.ProblemaDeFila;
import ec.mdd.cargas.adaptadores.entrada.api.SubidaApi;
import ec.mdd.cargas.adaptadores.entrada.api.TipoDeProblema;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Subir el archivo de una carga, validar su estructura y sus filas, y descargar el informe ({@code
 * contratos/cargas/openapi.yaml}, RF-06 a RF-09, F01-H02, F01-H03). Las dos operaciones exigen
 * «Cargas: escritura», como dice su {@code x-mdd-permiso} (ADR-021, D-71); {@code
 * PermisosCargasTest} lo comprueba. El archivo y el informe no se guardan ni se registran; cada
 * rechazo de estructura responde su código de {@link ErroresCargas}, y una carga que no existe,
 * 404. Un archivo con errores en sus filas responde 200 con su informe (principio XIII).
 */
@RestController
class SubidaControlador implements SubidaApi {

    /** Los problemas que trae la respuesta JSON; el CSV los trae todos (FR-011). */
    static final int PROBLEMAS_EN_LA_RESPUESTA = 100;

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ValidarArchivo validar;

    SubidaControlador(ValidarArchivo validar) {
        this.validar = validar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CARGAS:ESCRITURA')")
    public ResponseEntity<LecturaDeArchivo> validarArchivo(String codigo, MultipartFile archivo) {
        ArchivoValidado validado = validado(codigo, archivo);
        ec.mdd.cargas.dominio.InformeValidacionCarga informe = validado.informe();
        return ResponseEntity.ok(
                new LecturaDeArchivo(
                        validado.carga(),
                        validado.archivo(),
                        FormatoDeArchivo.fromValue(validado.formato().name()),
                        validado.registros(),
                        informe.valido(),
                        informe.errores(),
                        informe.advertencias(),
                        informe.primeros(PROBLEMAS_EN_LA_RESPUESTA).stream()
                                .map(SubidaControlador::problema)
                                .toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CARGAS:ESCRITURA')")
    public ResponseEntity<String> descargarInforme(String codigo, MultipartFile archivo) {
        ArchivoValidado validado = validado(codigo, archivo);
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("informe-" + validado.carga() + ".csv")
                                .build()
                                .toString())
                .body(EscritorDeInforme.escribir(validado.informe()));
    }

    private ArchivoValidado validado(String codigo, MultipartFile archivo) {
        try {
            return validar.validar(
                    codigo, new ArchivoSubido(archivo.getOriginalFilename(), archivo.getBytes()));
        } catch (CargaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static ProblemaDeFila problema(ec.mdd.cargas.dominio.ProblemaDeFila problema) {
        ProblemaDeFila respuesta =
                new ProblemaDeFila(
                        problema.fila(),
                        problema.campo(),
                        problema.valor(),
                        problema.motivo(),
                        TipoDeProblema.fromValue(problema.tipo().name()));
        problema.sugerencia().ifPresent(respuesta::setSugerencia);
        return respuesta;
    }
}
