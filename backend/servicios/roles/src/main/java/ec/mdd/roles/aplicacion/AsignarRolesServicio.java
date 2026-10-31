package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.dominio.ReglaDeAdministracion;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.AsignarRoles;
import ec.mdd.roles.puertos.entrada.RolDesconocido;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Asignaciones de rol con la regla de no quedarse sin administrador y su registro (FR-011). Al
 * quitar el rol Coordinador de carrera, la carrera se va con él, en la misma transacción (F06-H05).
 */
public final class AsignarRolesServicio implements AsignarRoles {

    private final RepositorioRoles roles;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RegistroDeCambios registro;
    private final RepositorioCarrerasDeCoordinadores carreras;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida y el reloj. */
    public AsignarRolesServicio(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            RepositorioCarrerasDeCoordinadores carreras,
            Transaccion transaccion,
            Clock reloj) {
        this.roles = roles;
        this.asignaciones = asignaciones;
        this.registro = registro;
        this.carreras = carreras;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Map<String, List<String>> rolesDeUsuarios() {
        return asignaciones.todas();
    }

    @Override
    public List<String> asignar(String usuario, Collection<String> nuevos, String quien) {
        List<Rol> todos = roles.todos();
        Set<String> existentes = todos.stream().map(Rol::identificador).collect(Collectors.toSet());
        List<String> desconocidos =
                nuevos.stream().filter(rol -> !existentes.contains(rol)).sorted().toList();
        if (!desconocidos.isEmpty()) {
            throw new RolDesconocido(desconocidos);
        }
        String nombre = NombreDeUsuario.normalizar(usuario);
        List<String> ordenados = nuevos.stream().distinct().sorted().toList();
        ReglaDeAdministracion.conservaAdministrador(
                RolesDespues.de(asignaciones.todas(), todos, nombre, ordenados), quien);
        List<String> antes = asignaciones.rolesDe(nombre);
        Instant ahora = reloj.instant();
        return transaccion.ejecutar(
                () -> {
                    asignaciones.reemplazar(nombre, ordenados, quien, ahora);
                    registrar(
                            ahora,
                            quien,
                            CambioDeRoles.Accion.ASIGNAR_ROL,
                            nombre,
                            diferencia(ordenados, antes));
                    registrar(
                            ahora,
                            quien,
                            CambioDeRoles.Accion.QUITAR_ROL,
                            nombre,
                            diferencia(antes, ordenados));
                    if (!ordenados.contains(AsignarCarrera.ROL) && carreras.quitar(nombre)) {
                        registro.registrar(
                                new CambioDeRoles(
                                        ahora,
                                        quien,
                                        CambioDeRoles.Accion.QUITAR_CARRERA,
                                        Map.of("usuario", nombre)));
                    }
                    return ordenados;
                });
    }

    private void registrar(
            Instant ahora,
            String quien,
            CambioDeRoles.Accion accion,
            String usuario,
            List<String> cambiados) {
        if (!cambiados.isEmpty()) {
            registro.registrar(
                    new CambioDeRoles(
                            ahora, quien, accion, Map.of("usuario", usuario, "roles", cambiados)));
        }
    }

    private static List<String> diferencia(List<String> unos, List<String> otros) {
        return unos.stream().filter(rol -> !otros.contains(rol)).toList();
    }
}
