package ec.mdd.plataforma.errores.nucleo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los catálogos de todos los servicios de un proceso, listos para traducir excepciones.
 *
 * <p>Se arma una vez al arrancar. Si dos entradas repiten el código o la excepción, falla y dice
 * cuál, para que el proceso no arranque con un catálogo ambiguo. Busca por la clase de la excepción
 * y, si no está, por la superclase registrada más cercana.
 */
public final class RegistroDeErrores {

    private final Map<Class<?>, DefinicionDeCodigo> porExcepcion = new HashMap<>();

    /**
     * @param catalogos catálogos de los servicios del proceso
     * @throws IllegalStateException si un código o una excepción se repiten
     */
    public RegistroDeErrores(List<? extends CatalogoDeErrores> catalogos) {
        Map<String, Class<?>> porCodigo = new HashMap<>();
        for (CatalogoDeErrores catalogo : catalogos) {
            for (EntradaCatalogo entrada : catalogo.entradas()) {
                String codigo = entrada.definicion().codigo();
                Class<?> anterior = porCodigo.putIfAbsent(codigo, entrada.excepcion());
                if (anterior != null) {
                    throw new IllegalStateException(
                            "El código "
                                    + codigo
                                    + " está registrado dos veces: para "
                                    + anterior.getName()
                                    + " y para "
                                    + entrada.excepcion().getName());
                }
                if (porExcepcion.putIfAbsent(entrada.excepcion(), entrada.definicion()) != null) {
                    throw new IllegalStateException(
                            "La excepción "
                                    + entrada.excepcion().getName()
                                    + " está registrada dos veces en el catálogo");
                }
            }
        }
    }

    /** La definición de la excepción o de su superclase registrada más cercana. */
    public Optional<DefinicionDeCodigo> buscar(Class<?> tipo) {
        for (Class<?> actual = tipo; actual != null; actual = actual.getSuperclass()) {
            DefinicionDeCodigo definicion = porExcepcion.get(actual);
            if (definicion != null) {
                return Optional.of(definicion);
            }
        }
        return Optional.empty();
    }
}
