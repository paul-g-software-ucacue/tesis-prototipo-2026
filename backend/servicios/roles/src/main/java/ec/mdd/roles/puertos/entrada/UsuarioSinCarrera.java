package ec.mdd.roles.puertos.entrada;

import java.io.Serial;

/** El usuario no tiene una carrera indicada. Responde {@code ROLES_USUARIO_SIN_CARRERA}. */
public final class UsuarioSinCarrera extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
