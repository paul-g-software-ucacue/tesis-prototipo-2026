package ec.mdd.plataforma.errores.nucleo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Relaciona una excepción de dominio o de aplicación con su código del catálogo (ADR-016).
 *
 * <p>La relación vive en los adaptadores de cada servicio: la excepción sigue siendo una clase Java
 * común, sin HTTP. Si la respuesta necesita encabezados (por ejemplo {@code Retry-After}) o
 * detalles (por ejemplo, las columnas que faltan en un archivo, F01-H02), la entrada los calcula
 * con los datos de la excepción, también en el adaptador. El título y la descripción siguen fijos.
 *
 * @param excepcion clase de la excepción; también vale para sus subclases
 * @param definicion su código, su estado y sus textos
 * @param encabezados encabezados de la respuesta según la excepción; ninguno por defecto
 * @param detalles detalles de la respuesta según la excepción; ninguno por defecto
 */
public record EntradaCatalogo(
        Class<? extends Throwable> excepcion,
        DefinicionDeCodigo definicion,
        Function<Throwable, Map<String, String>> encabezados,
        Function<Throwable, List<DetalleRespuesta>> detalles) {

    /** Exige la excepción y la definición. */
    public EntradaCatalogo {
        Objects.requireNonNull(excepcion, "excepcion");
        Objects.requireNonNull(definicion, "definicion");
        encabezados = encabezados == null ? error -> Map.of() : encabezados;
        detalles = detalles == null ? error -> List.of() : detalles;
    }

    /** Entrada sin encabezados. */
    public EntradaCatalogo(Class<? extends Throwable> excepcion, DefinicionDeCodigo definicion) {
        this(excepcion, definicion, null, null);
    }

    /** Atajo para declarar una entrada en una línea. */
    public static EntradaCatalogo de(
            Class<? extends Throwable> excepcion,
            String codigo,
            int estado,
            String titulo,
            String descripcion) {
        return new EntradaCatalogo(
                excepcion, new DefinicionDeCodigo(codigo, estado, titulo, descripcion));
    }

    /**
     * Entrada cuya respuesta lleva encabezados calculados con los datos de la excepción.
     *
     * @param excepcion clase de la excepción
     * @param definicion su código, su estado y sus textos
     * @param encabezados nombre y valor de cada encabezado; nunca datos sensibles
     */
    public static <E extends Throwable> EntradaCatalogo conEncabezados(
            Class<E> excepcion,
            DefinicionDeCodigo definicion,
            Function<? super E, Map<String, String>> encabezados) {
        Objects.requireNonNull(encabezados, "encabezados");
        return new EntradaCatalogo(
                excepcion, definicion, error -> encabezados.apply(excepcion.cast(error)), null);
    }

    /**
     * Entrada cuya respuesta lleva detalles calculados con los datos de la excepción, como las
     * columnas que faltan en un archivo de carga (F01-H02).
     *
     * @param excepcion clase de la excepción
     * @param definicion su código, su estado y sus textos
     * @param detalles un detalle por campo, en español; nunca datos sensibles ni valores recibidos
     */
    public static <E extends Throwable> EntradaCatalogo conDetalles(
            Class<E> excepcion,
            DefinicionDeCodigo definicion,
            Function<? super E, List<DetalleRespuesta>> detalles) {
        Objects.requireNonNull(detalles, "detalles");
        return new EntradaCatalogo(
                excepcion, definicion, null, error -> detalles.apply(excepcion.cast(error)));
    }

    /** Los encabezados de la respuesta para esta excepción. */
    public Map<String, String> encabezados(Throwable error) {
        return encabezados.apply(error);
    }

    /** Los detalles de la respuesta para esta excepción; vacío si no tiene. */
    public List<DetalleRespuesta> detalles(Throwable error) {
        return detalles.apply(error);
    }
}
