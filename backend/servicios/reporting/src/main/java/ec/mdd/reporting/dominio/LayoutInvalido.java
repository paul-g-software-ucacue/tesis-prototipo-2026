package ec.mdd.reporting.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Un archivo de mapeo que no cumple las reglas del layout de exportación (FR-015, {@code
 * REPORTING_LAYOUT_INVALIDO}). Lleva hasta 50 problemas y, si hay más, uno que dice cuántos faltan:
 * con los primeros basta para corregir, y un archivo de 256 KB podría tener miles.
 */
public class LayoutInvalido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Cuántos problemas se informan como mucho. */
    public static final int TOPE = 50;

    @SuppressWarnings("serial")
    private final List<ProblemaDelLayout> problemas;

    /**
     * @param todos todos los problemas encontrados; al menos uno
     */
    public LayoutInvalido(List<ProblemaDelLayout> todos) {
        super("El archivo de mapeo tiene " + todos.size() + " problemas.");
        if (todos.isEmpty()) {
            throw new IllegalArgumentException("un layout inválido tiene al menos un problema");
        }
        List<ProblemaDelLayout> informados =
                new ArrayList<>(todos.subList(0, Math.min(TOPE, todos.size())));
        if (todos.size() > TOPE) {
            informados.add(
                    new ProblemaDelLayout(
                            "archivo", "Hay " + (todos.size() - TOPE) + " problemas más."));
        }
        this.problemas = List.copyOf(informados);
    }

    /** Los problemas informados, en orden. */
    public List<ProblemaDelLayout> problemas() {
        return problemas;
    }
}
