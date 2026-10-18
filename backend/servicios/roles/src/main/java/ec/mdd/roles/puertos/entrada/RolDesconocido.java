package ec.mdd.roles.puertos.entrada;

import java.util.Collection;
import java.util.List;

/** Uno o más identificadores no son de ningún rol. El mensaje dice cuáles. */
public class RolDesconocido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> roles;

    /**
     * @param roles los identificadores que no existen
     */
    public RolDesconocido(Collection<String> roles) {
        super("Roles desconocidos: " + String.join(", ", roles));
        this.roles = List.copyOf(roles);
    }

    /** Los identificadores que no existen. */
    public List<String> roles() {
        return roles;
    }
}
