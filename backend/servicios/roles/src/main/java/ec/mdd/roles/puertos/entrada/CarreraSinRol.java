package ec.mdd.roles.puertos.entrada;

import java.io.Serial;

/**
 * El usuario no tiene el rol Coordinador de carrera, así que no puede tener carrera. Responde
 * {@code ROLES_CARRERA_SIN_ROL}.
 */
public final class CarreraSinRol extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
