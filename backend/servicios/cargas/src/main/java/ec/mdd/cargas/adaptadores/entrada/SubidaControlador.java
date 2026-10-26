package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.CambioDeCampo;
import ec.mdd.cargas.adaptadores.entrada.api.FormatoDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.LecturaDeArchivo;
import ec.mdd.cargas.adaptadores.entrada.api.ProblemaDeFila;
import ec.mdd.cargas.adaptadores.entrada.api.RegistroDeLaVista;
import ec.mdd.cargas.adaptadores.entrada.api.ResultadoDeConfirmacion;
import ec.mdd.cargas.adaptadores.entrada.api.SubidaApi;
import ec.mdd.cargas.adaptadores.entrada.api.TipoDeProblema;
import ec.mdd.cargas.adaptadores.entrada.api.TipoDeRegistro;
import ec.mdd.cargas.adaptadores.entrada.api.VistaPrevia;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.Confirmacion;
import ec.mdd.cargas.puertos.entrada.ConfirmarCarga;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Subir el archivo de una carga, validar su estructura y sus filas, ver su vista previa, descargar
 * el informe y confirmar ({@code contratos/cargas/openapi.yaml}, RF-06 a RF-10, F01-H02 a F01-H04).
 * Las tres operaciones exigen «Cargas: escritura», como dice su {@code x-mdd-permiso} (ADR-021,
 * D-71); {@code PermisosCargasTest} lo comprueba. El archivo y el informe no se guardan ni se
 * registran; cada rechazo de estructura responde su código de {@link ErroresCargas}, y una carga
 * que no existe, 404. Un archivo con errores en sus filas responde 200 con su informe (principio
 * XIII).
 */
@RestController
class SubidaControlador implements SubidaApi {

    /** Los problemas que trae la respuesta JSON; el CSV los trae todos (FR-011). */
    static final int PROBLEMAS_EN_LA_RESPUESTA = 100;

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    /** Los registros de cada tipo que trae la vista previa (FR-002). */
    static final int REGISTROS_EN_LA_VISTA = 100;

    private final ValidarArchivo validar;
    private final ConfirmarCarga confirmar;

    SubidaControlador(ValidarArchivo validar, ConfirmarCarga confirmar) {
        this.validar = validar;
        this.confirmar = confirmar;
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
                                .toList(),
                        vista(validado.vistaPrevia())));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CARGAS:ESCRITURA')")
    public ResponseEntity<ResultadoDeConfirmacion> confirmarCarga(
            String codigo, MultipartFile archivo, String huella) {
        Confirmacion confirmacion;
        try {
            confirmacion =
                    confirmar.confirmar(
                            codigo,
                            new ArchivoSubido(archivo.getOriginalFilename(), archivo.getBytes()),
                            huella,
                            SecurityContextHolder.getContext().getAuthentication().getName());
        } catch (CargaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        return ResponseEntity.ok(
                new ResultadoDeConfirmacion(
                        ResultadoDeConfirmacion.ResultadoEnum.fromValue(
                                confirmacion.resultado().name()),
                        confirmacion.nuevos(),
                        confirmacion.cambiados(),
                        confirmacion.bajasSinAplicar(),
                        confirmacion.confirmadaEl().atOffset(ZoneOffset.UTC)));
    }

    private static VistaPrevia vista(ec.mdd.cargas.dominio.VistaPrevia vista) {
        VistaPrevia respuesta = new VistaPrevia(vista.filasConErrores(), java.util.List.of());
        vista.delDueno()
                .ifPresent(
                        delDueno -> {
                            respuesta.setNuevos(
                                    delDueno.cuantos(
                                            ec.mdd.plataforma.cargas.TipoDeRegistro.NUEVO));
                            respuesta.setCambian(
                                    delDueno.cuantos(
                                            ec.mdd.plataforma.cargas.TipoDeRegistro.CAMBIA));
                            respuesta.setBajas(
                                    delDueno.cuantos(ec.mdd.plataforma.cargas.TipoDeRegistro.BAJA));
                            respuesta.setIguales(
                                    delDueno.cuantos(
                                            ec.mdd.plataforma.cargas.TipoDeRegistro.IGUAL));
                            respuesta.setRegistros(
                                    delDueno.primeros(REGISTROS_EN_LA_VISTA).stream()
                                            .map(SubidaControlador::registro)
                                            .toList());
                            respuesta.setHuella(delDueno.huella());
                        });
        return respuesta;
    }

    private static RegistroDeLaVista registro(ec.mdd.plataforma.cargas.RegistroDeLaVista registro) {
        RegistroDeLaVista respuesta =
                new RegistroDeLaVista(
                        registro.clave(),
                        TipoDeRegistro.fromValue(registro.tipo().name()),
                        registro.cambios().stream()
                                .map(
                                        cambio ->
                                                new CambioDeCampo(
                                                        cambio.campo(),
                                                        cambio.ahora(),
                                                        cambio.archivo()))
                                .toList());
        respuesta.setFila(registro.fila());
        return respuesta;
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
