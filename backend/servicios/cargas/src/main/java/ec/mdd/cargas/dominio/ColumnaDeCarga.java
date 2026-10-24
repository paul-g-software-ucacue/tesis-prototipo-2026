package ec.mdd.cargas.dominio;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Una columna de la plantilla de una carga, tal como la describe el diccionario de datos (RF-05,
 * glosario «Diccionario de datos»). Toda columna tiene tipo, obligatoriedad y un ejemplo, como pide
 * el escenario de F01-H01.
 *
 * @param campo el encabezado de la columna, en minúsculas (por ejemplo, {@code codigo_unidad})
 * @param descripcion qué contiene
 * @param tipo Texto, Entero, Decimal, Fecha, Catálogo, S/N o Lista
 * @param longitud la longitud máxima, o {@code null} si no tiene
 * @param obligatorio «Sí», «No» o la condición, como «Sí si tipo_unidad = GESTION»
 * @param formato el formato o los valores permitidos, o vacío
 * @param clave si es clave primaria o foránea, o vacío
 * @param validacion la regla que se comprueba al cargar, o vacío
 * @param ejemplo un valor de ejemplo, ficticio
 * @param catalogo el código del catálogo de sus valores, o vacío
 */
public record ColumnaDeCarga(
        String campo,
        String descripcion,
        String tipo,
        Integer longitud,
        String obligatorio,
        String formato,
        String clave,
        String validacion,
        String ejemplo,
        String catalogo) {

    private static final Pattern CAMPO = Pattern.compile("^[a-z0-9_]+$");

    /** Valida la columna; los textos opcionales sin valor quedan vacíos. */
    public ColumnaDeCarga {
        if (campo == null || !CAMPO.matcher(campo).matches()) {
            throw new IllegalArgumentException(
                    "El campo «"
                            + campo
                            + "» solo admite minúsculas sin tildes, números y guion bajo");
        }
        String donde = "La columna " + campo + ": ";
        descripcion = Textos.obligatorio(descripcion, donde + "la descripción");
        tipo = Textos.obligatorio(tipo, donde + "el tipo");
        if (longitud != null && longitud < 1) {
            throw new IllegalArgumentException(donde + "la longitud máxima es al menos 1");
        }
        obligatorio = Textos.obligatorio(obligatorio, donde + "la obligatoriedad");
        formato = Textos.opcional(formato, donde + "el formato");
        clave = Textos.opcional(clave, donde + "la clave");
        validacion = Textos.opcional(validacion, donde + "la validación");
        ejemplo = Textos.obligatorio(ejemplo, donde + "el ejemplo");
        catalogo = Textos.opcional(catalogo, donde + "el catálogo");
    }

    /** El código del catálogo de sus valores, si lo tiene. */
    public Optional<String> catalogoDeValores() {
        return catalogo.isEmpty() ? Optional.empty() : Optional.of(catalogo);
    }
}
