package ec.mdd.plataforma.errores.nucleo;

import java.util.List;

/**
 * Los errores que un servicio registra en el catálogo (ADR-016).
 *
 * <p>Cada servicio con errores propios tiene una implementación en sus adaptadores de entrada
 * ({@code Errores<Servicio>}), y el proceso las reúne al arrancar.
 */
public interface CatalogoDeErrores {

    /** Las entradas del servicio. */
    List<EntradaCatalogo> entradas();
}
