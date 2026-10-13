package ec.mdd.plataforma.errores.nucleo;

import java.util.Objects;

/**
 * Relaciona una excepción de dominio o de aplicación con su código del catálogo (ADR-016).
 *
 * <p>La relación vive en los adaptadores de cada servicio: la excepción sigue siendo una clase Java
 * común, sin HTTP.
 *
 * @param excepcion clase de la excepción; también vale para sus subclases
 * @param definicion su código, su estado y sus textos
 */
public record EntradaCatalogo(Class<? extends Throwable> excepcion, DefinicionDeCodigo definicion) {

    /** Exige la excepción y la definición. */
    public EntradaCatalogo {
        Objects.requireNonNull(excepcion, "excepcion");
        Objects.requireNonNull(definicion, "definicion");
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
}
