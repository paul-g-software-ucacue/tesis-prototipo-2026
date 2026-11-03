package ec.mdd.validacionocs.dominio;

import java.util.List;
import java.util.Optional;

/**
 * El resultado de revisar un archivo: la configuración, o todo lo que hay que corregir.
 *
 * @param configuracion la configuración, si no hay problemas
 * @param problemas lo que hay que corregir
 */
public record Revision(Optional<ConfiguracionFlujo> configuracion, List<String> problemas) {

    /** Copia los problemas. */
    public Revision {
        problemas = List.copyOf(problemas);
    }
}
