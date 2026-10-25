package ec.mdd.plataforma.errores;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.regex.Matcher;
import org.springframework.validation.FieldError;

/**
 * La descripción en español de un campo que no pasó la validación (research §7, RNF-USA-01).
 *
 * <p>No usa los mensajes por defecto del validador, que salen en el idioma del servidor. Un valor
 * nulo o vacío, texto o lista, siempre es «es obligatorio», aunque la restricción que falló sea de
 * longitud. El tamaño de una lista se cuenta en elementos, no en caracteres (F03-H02). Un patrón de
 * solo dígitos y los límites de un número dicen qué se espera, para quien llena el formulario
 * (F03-H04); otro patrón queda en «no tiene el formato esperado».
 */
final class DescripcionDeValidacion {

    static final String OBLIGATORIO = "es obligatorio";
    static final String NO_VALIDO = "no es válido";

    /** «^[0-9]{3}$» o «^[0-9]{7,10}$», con o sin anclas. */
    private static final java.util.regex.Pattern DIGITOS =
            java.util.regex.Pattern.compile("\\^?\\[0-9]\\{(\\d+)(?:,(\\d+))?}\\$?");

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
        if (restriccion instanceof Pattern patron) {
            return digitos(patron.regexp());
        }
        if (restriccion instanceof Min minimo) {
            return "debe ser al menos " + minimo.value();
        }
        if (restriccion instanceof Max maximo) {
            return "no puede ser mayor que " + maximo.value();
        }
        if (restriccion instanceof DecimalMin minimo) {
            return (minimo.inclusive() ? "debe ser al menos " : "debe ser mayor que ")
                    + minimo.value();
        }
        if (restriccion instanceof DecimalMax maximo) {
            return (maximo.inclusive() ? "no puede ser mayor que " : "debe ser menor que ")
                    + maximo.value();
        }
        return NO_VALIDO;
    }

    private static String digitos(String patron) {
        Matcher digitos = DIGITOS.matcher(patron);
        if (!digitos.matches()) {
            return "no tiene el formato esperado";
        }
        String desde = digitos.group(1);
        String hasta = digitos.group(2);
        return hasta == null
                ? "debe tener " + desde + " dígitos, sin letras ni espacios"
                : "debe tener de " + desde + " a " + hasta + " dígitos, sin letras ni espacios";
    }
}
