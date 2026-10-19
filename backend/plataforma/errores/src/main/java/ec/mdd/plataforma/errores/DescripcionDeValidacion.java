package ec.mdd.plataforma.errores;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import org.springframework.validation.FieldError;

/**
 * La descripción en español de un campo que no pasó la validación (research §7, RNF-USA-01).
 *
 * <p>No usa los mensajes por defecto del validador, que salen en el idioma del servidor. Un valor
 * nulo o vacío siempre es «es obligatorio», aunque la restricción que falló sea de longitud.
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
                error.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAnnotation());
    }

    /** La de un parámetro que validó Spring antes del controlador. */
    static String de(ConstraintViolation<?> violacion) {
        if (vacio(violacion.getInvalidValue())) {
            return OBLIGATORIO;
        }
        return segun(violacion.getConstraintDescriptor().getAnnotation());
    }

    private static boolean vacio(Object rechazado) {
        return rechazado == null
                || (rechazado instanceof CharSequence texto && texto.toString().isBlank());
    }

    private static String segun(Annotation restriccion) {
        if (restriccion instanceof Size tamano) {
            return tamano.max() == Integer.MAX_VALUE
                    ? "debe tener al menos " + tamano.min() + " caracteres"
                    : "debe tener entre " + tamano.min() + " y " + tamano.max() + " caracteres";
        }
        if (restriccion instanceof Pattern) {
            return "no tiene el formato esperado";
        }
        return NO_VALIDO;
    }
}
