package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.errores.nucleo.RegistroDeErrores;
import ec.mdd.plataforma.errores.nucleo.RespuestaError;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * El único {@code @RestControllerAdvice} de cada proceso: traduce todos los errores al esquema
 * {@code Error} (principio XIII, ADR-016).
 *
 * <ul>
 *   <li>Una excepción registrada en un {@code CatalogoDeErrores}, o subclase de una registrada,
 *       responde con su código, su estado, sus textos del catálogo y los encabezados que la entrada
 *       calcule (por ejemplo {@code Retry-After}).
 *   <li>Un cuerpo que no pasa la validación del contrato, o una {@link SolicitudInvalida}, responde
 *       400 {@code COMUN_SOLICITUD_INVALIDA}, con un detalle por campo.
 *   <li>Los errores de Spring MVC antes del controlador responden con su código común.
 *   <li>Cualquier otra excepción responde 500 {@code COMUN_ERROR_INTERNO} con un {@code
 *       idIncidente}, y se registra con su traza saneada.
 * </ul>
 *
 * <p>No traduce las excepciones de seguridad: las vuelve a lanzar, para que las atienda la
 * seguridad del proceso y no se conviertan en un 500.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    // Por nombre: plataforma:errores no depende de Spring Security.
    private static final Set<String> EXCEPCIONES_DE_SEGURIDAD =
            Set.of(
                    "org.springframework.security.core.AuthenticationException",
                    "org.springframework.security.access.AccessDeniedException");

    private final RegistroDeErrores registro;

    /**
     * @param registro los catálogos de los servicios del proceso
     */
    public ManejadorDeErrores(RegistroDeErrores registro) {
        this.registro = registro;
    }

    /** Errores registrados y errores técnicos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> traducir(Exception excepcion, HttpServletRequest peticion)
            throws Exception {
        if (esDeSeguridad(excepcion)) {
            throw excepcion;
        }
        if (excepcion instanceof SolicitudInvalida invalida) {
            return json(
                    400,
                    RespuestaError.conDetalles(
                            CodigosComunes.SOLICITUD_INVALIDA, invalida.detalles()));
        }
        Optional<EntradaCatalogo> entrada = registro.buscarEntrada(excepcion.getClass());
        if (entrada.isPresent()) {
            ResponseEntity.BodyBuilder respuesta =
                    ResponseEntity.status(entrada.get().definicion().estado())
                            .contentType(MediaType.APPLICATION_JSON);
            entrada.get().encabezados(excepcion).forEach(respuesta::header);
            return respuesta.body(RespuestaError.deCatalogo(entrada.get().definicion()));
        }
        String id =
                RegistroDeIncidentes.registrar(
                        excepcion, peticion.getMethod(), peticion.getRequestURI());
        return json(500, RespuestaError.deIncidente(CodigosComunes.ERROR_INTERNO, id));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException excepcion,
            HttpHeaders encabezados,
            HttpStatusCode estado,
            WebRequest peticion) {
        List<DetalleRespuesta> detalles = new ArrayList<>();
        for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
            detalles.add(new DetalleRespuesta(error.getField(), DescripcionDeValidacion.de(error)));
        }
        return conDetalles(detalles);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException excepcion,
            HttpHeaders encabezados,
            HttpStatusCode estado,
            WebRequest peticion) {
        List<DetalleRespuesta> detalles = new ArrayList<>();
        excepcion
                .getParameterValidationResults()
                .forEach(
                        resultado ->
                                resultado
                                        .getResolvableErrors()
                                        .forEach(
                                                error ->
                                                        detalles.add(
                                                                error instanceof FieldError campo
                                                                        ? new DetalleRespuesta(
                                                                                campo.getField(),
                                                                                DescripcionDeValidacion
                                                                                        .de(campo))
                                                                        : new DetalleRespuesta(
                                                                                resultado
                                                                                        .getMethodParameter()
                                                                                        .getParameterName(),
                                                                                resultado
                                                                                                        .getArgument()
                                                                                                == null
                                                                                        ? DescripcionDeValidacion
                                                                                                .OBLIGATORIO
                                                                                        : DescripcionDeValidacion
                                                                                                .NO_VALIDO))));
        return conDetalles(detalles);
    }

    /** Los demás errores de Spring MVC: el cuerpo pasa a ser el del código común del estado. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception excepcion,
            Object cuerpo,
            HttpHeaders encabezados,
            HttpStatusCode estado,
            WebRequest peticion) {
        int codigo = estado.value();
        RespuestaError respuesta;
        if (codigo >= 500 && codigo != 503) {
            HttpServletRequest servlet = ((ServletWebRequest) peticion).getRequest();
            String id =
                    RegistroDeIncidentes.registrar(
                            excepcion, servlet.getMethod(), servlet.getRequestURI());
            respuesta = RespuestaError.deIncidente(CodigosComunes.ERROR_INTERNO, id);
        } else if (excepcion instanceof HttpMessageNotReadableException) {
            respuesta = RespuestaError.deCatalogo(CodigosComunes.CUERPO_ILEGIBLE);
        } else {
            respuesta = RespuestaError.deCatalogo(CodigosComunes.segunEstado(codigo));
        }
        HttpHeaders conTipo = new HttpHeaders();
        conTipo.putAll(encabezados);
        conTipo.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(respuesta, conTipo, estado);
    }

    private static ResponseEntity<Object> conDetalles(List<DetalleRespuesta> detalles) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(RespuestaError.conDetalles(CodigosComunes.SOLICITUD_INVALIDA, detalles));
    }

    private static ResponseEntity<RespuestaError> json(int estado, RespuestaError respuesta) {
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(respuesta);
    }

    private static boolean esDeSeguridad(Throwable excepcion) {
        for (Class<?> tipo = excepcion.getClass(); tipo != null; tipo = tipo.getSuperclass()) {
            if (EXCEPCIONES_DE_SEGURIDAD.contains(tipo.getName())) {
                return true;
            }
        }
        return false;
    }
}
