package ec.mdd.plataforma.errores.nucleo;

import java.util.regex.Pattern;

/**
 * Un código del catálogo de errores, con su estado HTTP y sus textos públicos (principio XIII).
 *
 * <p>Los textos son los de su fila en {@code contratos/comun/errores.md}; una prueba lo comprueba.
 *
 * @param codigo identificador estable, en mayúsculas con guion bajo y con el prefijo de su servicio
 *     o el de la plataforma ({@code COMUN_}, {@code SEGURIDAD_})
 * @param estado estado HTTP, siempre 4xx o 5xx
 * @param titulo categoría breve del error
 * @param descripcion qué ocurrió y qué puede hacer el usuario, en español
 */
public record DefinicionDeCodigo(String codigo, int estado, String titulo, String descripcion) {

    private static final Pattern FORMATO = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)+");

    /** Valida el formato del código, que el estado sea de error y que haya textos. */
    public DefinicionDeCodigo {
        if (codigo == null || !FORMATO.matcher(codigo).matches()) {
            throw new IllegalArgumentException("Código de error mal formado: " + codigo);
        }
        if (estado < 400 || estado > 599) {
            throw new IllegalArgumentException(
                    "El estado de " + codigo + " no es 4xx ni 5xx: " + estado);
        }
        if (titulo == null || titulo.isBlank() || descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "El código " + codigo + " no tiene título o descripción");
        }
    }
}
