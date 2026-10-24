package ec.mdd.plataforma.errores;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.util.Collection;
import org.springframework.validation.FieldError;

/**
 * La descripción en español de un campo que no pasó la validación (research §7, RNF-USA-01).
 *
 * <p>No usa los mensajes por defecto del validador, que salen en el idioma del servidor. Un valor
 * nulo o vacío, texto o lista, siempre es «es obligatorio», aunque la restricción que falló sea de
 * longitud. El tamaño de una lista se cuenta en elementos, no en caracteres (F03-H02).
 */
final class DescripcionDeValidacion {

    static final String OBLIGATORIO = "es obligatorio";
    static final String NO_VALIDO = "no es válido";

    private DescripcionDeValidacion() {}

    static String de(FieldError error) {
        if (vacio(error.getRejectedValue())) {
            return OBLIGATORIO;
        }
        if (!error.contains(ConstraintViolation.class)) {
            return NO_VALIDO;
        }
        return segun(
                error.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAnnotation(),
                error.getRejectedValue());
    }

    /** La de un parámetro que validó Spring antes del controlador. */
    static String de(ConstraintViolation<?> violacion) {
        if (vacio(violacion.getInvalidValue())) {
            return OBLIGATORIO;
        }
        return segun(
                violacion.getConstraintDescriptor().getAnnotation(), violacion.getInvalidValue());
    }

    private static boolean vacio(Object rechazado) {
        return rechazado == null
                || (rechazado instanceof CharSequence texto && texto.toString().isBlank())
                || (rechazado instanceof Collection<?> lista && lista.isEmpty());
    }

    private static String segun(Annotation restriccion, Object rechazado) {
        if (restriccion instanceof Size tamano) {
            String unidad = rechazado instanceof Collection<?> ? " elementos" : " caracteres";
            return tamano.max() == Integer.MAX_VALUE
                    ? "debe tener al menos " + tamano.min() + unidad
                    : "debe tener entre " + tamano.min() + " y " + tamano.max() + unidad;
        }
        if (restriccion instanceof Pattern) {
            return "no tiene el formato esperado";
        }
        return NO_VALIDO;
    }
}
