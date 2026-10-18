package ec.mdd.roles.dominio;

import java.util.Collection;
import java.util.Map;

/**
 * Las reglas de la administración de roles (casos límite de la especificación de F05-H02).
 *
 * <ul>
 *   <li>Los 10 roles de D-16 no se editan ni se borran (D-63).
 *   <li>Un rol asignado no se borra.
 *   <li>Dos roles no pueden tener nombres que den el mismo identificador.
 *   <li>Siempre queda alguien con {@code USUARIOS_ROLES:ESCRITURA}, y quien hace un cambio no se lo
 *       quita a sí mismo. No mira si el usuario está activo, que es dato de Usuarios (research §7).
 * </ul>
 */
public final class ReglaDeAdministracion {

    /** La funcionalidad cuyo permiso de escritura administra los roles. */
    public static final String ADMINISTRACION = "USUARIOS_ROLES";

    private ReglaDeAdministracion() {}

    /**
     * @throws RolPredefinido si es uno de los de D-16
     */
    public static void editar(Rol rol) {
        if (rol.predefinido()) {
            throw new RolPredefinido("Edición del rol predefinido " + rol.identificador());
        }
    }

    /**
     * @param usuarios a cuántos usuarios está asignado
     * @throws RolPredefinido si es uno de los de D-16
     * @throws RolAsignado si está asignado a alguien
     */
    public static void borrar(Rol rol, int usuarios) {
        if (rol.predefinido()) {
            throw new RolPredefinido("Borrado del rol predefinido " + rol.identificador());
        }
        if (usuarios > 0) {
            throw new RolAsignado(rol.identificador(), usuarios);
        }
    }

    /**
     * Comprueba que el nombre no dé el identificador de otro rol, ni el que da el nombre de otro.
     *
     * @param nombre el nombre nuevo
     * @param propio identificador del rol que se edita, que no cuenta; {@code null} al crear
     * @param roles todos los roles
     * @throws NombreRepetido si choca con otro rol
     */
    public static void nombreDisponible(String nombre, String propio, Collection<Rol> roles) {
        String identificador = Rol.identificadorDe(nombre);
        for (Rol otro : roles) {
            if (otro.identificador().equals(propio)) {
                continue;
            }
            if (otro.identificador().equals(identificador)
                    || Rol.identificadorDe(otro.nombre()).equals(identificador)) {
                throw new NombreRepetido(
                        "El nombre da " + identificador + ", igual que " + otro.identificador());
            }
        }
    }

    /**
     * Comprueba que, después del cambio, alguien tenga {@code USUARIOS_ROLES:ESCRITURA} y que quien
     * hace el cambio lo conserve.
     *
     * @param rolesDespues los roles de cada usuario con algún rol, ya con el cambio
     * @param quienCambia el usuario que hace el cambio; {@code null} si no es un usuario
     * @throws SinAdministrador si alguna de las dos cosas no se cumple
     */
    public static void conservaAdministrador(
            Map<String, ? extends Collection<Rol>> rolesDespues, String quienCambia) {
        boolean alguno = rolesDespues.values().stream().anyMatch(ReglaDeAdministracion::administra);
        if (!alguno) {
            throw new SinAdministrador("Nadie conservaría " + ADMINISTRACION + ":ESCRITURA");
        }
        if (quienCambia != null) {
            Collection<Rol> suyos = rolesDespues.get(quienCambia);
            if (suyos == null || !administra(suyos)) {
                throw new SinAdministrador(
                        quienCambia + " perdería " + ADMINISTRACION + ":ESCRITURA");
            }
        }
    }

    private static boolean administra(Collection<Rol> roles) {
        return roles.stream().anyMatch(rol -> rol.da(ADMINISTRACION, TipoDePermiso.ESCRITURA));
    }
}
