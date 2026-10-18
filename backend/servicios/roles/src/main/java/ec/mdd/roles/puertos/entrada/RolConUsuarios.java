package ec.mdd.roles.puertos.entrada;

import ec.mdd.roles.dominio.Rol;

/**
 * Un rol con cuántos usuarios lo tienen (esquema {@code Rol}, campo {@code usuariosAsignados}).
 *
 * @param rol el rol
 * @param usuarios a cuántos usuarios está asignado
 */
public record RolConUsuarios(Rol rol, int usuarios) {}
