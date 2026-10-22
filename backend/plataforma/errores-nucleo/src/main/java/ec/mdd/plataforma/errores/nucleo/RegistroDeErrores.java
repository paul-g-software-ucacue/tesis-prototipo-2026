package ec.mdd.plataforma.errores.nucleo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los catálogos de todos los servicios de un proceso, listos para traducir excepciones.
 *
 * <p>Se arma una vez al arrancar. Si dos entradas repiten el código o la excepción, falla y dice
 * cuál, para que el proceso no arranque con un catálogo ambiguo. La excepción es un código común
 * ({@code COMUN_…}) con la misma definición: dos servicios del mismo proceso pueden traducir cada
 * uno su excepción a él, como los receptores de eventos de Usuarios y Roles con {@code
 * COMUN_EVENTO_NO_RECONOCIDO} (ADR-018, F05-H03). Busca por la clase de la excepción y, si no está,
 * por la superclase registrada más cercana.
 */
public final class RegistroDeErrores {

    private static final String PREFIJO_COMUN = "COMUN_";

    private final Map<Class<?>, EntradaCatalogo> porExcepcion = new HashMap<>();

    /**
     * @param catalogos catálogos de los servicios del proceso
     * @throws IllegalStateException si un código o una excepción se repiten
     */
    public RegistroDeErrores(List<? extends CatalogoDeErrores> catalogos) {
        Map<String, EntradaCatalogo> porCodigo = new HashMap<>();
        for (CatalogoDeErrores catalogo : catalogos) {
            for (EntradaCatalogo entrada : catalogo.entradas()) {
                String codigo = entrada.definicion().codigo();
                EntradaCatalogo anterior = porCodigo.putIfAbsent(codigo, entrada);
                if (anterior != null && !comunCompartido(anterior, entrada)) {
                    throw new IllegalStateException(
                            "El código "
                                    + codigo
                                    + " está registrado dos veces: para "
                                    + anterior.excepcion().getName()
                                    + " y para "
                                    + entrada.excepcion().getName());
                }
                if (porExcepcion.putIfAbsent(entrada.excepcion(), entrada) != null) {
                    throw new IllegalStateException(
                            "La excepción "
                                    + entrada.excepcion().getName()
                                    + " está registrada dos veces en el catálogo");
                }
            }
        }
    }

    private static boolean comunCompartido(EntradaCatalogo una, EntradaCatalogo otra) {
        return una.definicion().codigo().startsWith(PREFIJO_COMUN)
                && una.definicion().equals(otra.definicion());
    }

    /** La definición de la excepción o de su superclase registrada más cercana. */
    public Optional<DefinicionDeCodigo> buscar(Class<?> tipo) {
        return buscarEntrada(tipo).map(EntradaCatalogo::definicion);
    }

    /** La entrada de la excepción o de su superclase registrada más cercana. */
    public Optional<EntradaCatalogo> buscarEntrada(Class<?> tipo) {
        for (Class<?> actual = tipo; actual != null; actual = actual.getSuperclass()) {
            EntradaCatalogo entrada = porExcepcion.get(actual);
            if (entrada != null) {
                return Optional.of(entrada);
            }
        }
        return Optional.empty();
    }
}
