package ec.mdd.roles.puertos.entrada;

import ec.mdd.roles.dominio.Funcionalidad;
import java.util.List;

/**
 * Caso de uso: el administrador ve, crea, edita y borra roles (RF-02, FR-004). Los 10 de D-16 se
 * ven, pero no se editan ni se borran (D-63). Cada cambio queda registrado con quién y cuándo
 * (FR-011).
 */
public interface GestionarRoles {

    /** Las funcionalidades, en su orden, con los tipos que admite cada una. */
    List<Funcionalidad> funcionalidades();

    /** Todos los roles por nombre, con cuántos usuarios tiene cada uno. */
    List<RolConUsuarios> roles();

    /**
     * @throws ec.mdd.roles.aplicacion.RolNoEncontrado si no existe
     */
    RolConUsuarios consultar(String identificador);

    /**
     * Crea un rol con el identificador derivado de su nombre.
     *
     * @param quien el usuario que lo crea
     */
    RolConUsuarios crear(DatosDeRol datos, String quien);

    /**
     * Cambia el nombre, la descripción y los permisos de un rol propio; el identificador no cambia.
     *
     * @param quien el usuario que lo edita
     */
    RolConUsuarios editar(String identificador, DatosDeRol datos, String quien);

    /**
     * Borra un rol propio que no está asignado a nadie.
     *
     * @param quien el usuario que lo borra
     */
    void borrar(String identificador, String quien);
}
