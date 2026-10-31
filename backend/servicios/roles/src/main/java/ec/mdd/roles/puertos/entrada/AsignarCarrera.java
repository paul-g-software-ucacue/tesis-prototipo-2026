package ec.mdd.roles.puertos.entrada;

import java.util.Map;

/**
 * Caso de uso: el administrador indica la carrera de un Coordinador de carrera, que decide qué es
 * «lo propio» de su alcance (D-64). El cambio vale desde la próxima renovación de su sesión (D-62).
 */
public interface AsignarCarrera {

    /** El rol que puede tener carrera. */
    String ROL = "COORDINADOR_CARRERA";

    /** La carrera de cada usuario que tiene una. */
    Map<String, String> carreras();

    /**
     * Indica la carrera del usuario; reemplaza la que tenga. Un coordinador tiene una sola.
     *
     * @param quien el usuario que hace el cambio
     * @throws CarreraSinRol si no tiene el rol Coordinador de carrera
     */
    void indicar(String usuario, String carrera, String quien);

    /**
     * Quita la carrera del usuario.
     *
     * @param quien el usuario que hace el cambio
     * @throws UsuarioSinCarrera si no tiene carrera
     */
    void quitar(String usuario, String quien);
}
