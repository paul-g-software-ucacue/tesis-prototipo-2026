package ec.mdd.validacionocs.puertos.entrada;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/** El archivo tiene problemas. Los lleva todos, para corregirlos de una vez. */
public final class ConfiguracionInvalida extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final ArrayList<String> problemas;

    /** Recibe los problemas, cada uno con qué corregir. */
    public ConfiguracionInvalida(List<String> problemas) {
        this.problemas = new ArrayList<>(problemas);
    }

    /** Los problemas, cada uno con qué corregir. */
    public List<String> problemas() {
        return List.copyOf(problemas);
    }
}
