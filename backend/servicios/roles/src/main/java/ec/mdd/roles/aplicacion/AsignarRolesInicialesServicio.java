package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.RolDesconocido;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Roles iniciales de la demo, una sola vez por usuario, con su registro (research §8). */
public final class AsignarRolesInicialesServicio implements AsignarRolesIniciales {

    /** Quién figura en el registro de los roles iniciales. */
    static final String SISTEMA = "sistema";

    private final RepositorioRoles roles;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RegistroDeCambios registro;
    private final Clock reloj;

    /** Recibe los puertos de salida y el reloj. */
    public AsignarRolesInicialesServicio(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Clock reloj) {
        this.roles = roles;
        this.asignaciones = asignaciones;
        this.registro = registro;
        this.reloj = reloj;
    }

    @Override
    public boolean asignar(String usuario, Collection<String> iniciales) {
        Set<String> existentes =
                roles.todos().stream().map(Rol::identificador).collect(Collectors.toSet());
        List<String> desconocidos =
                iniciales.stream().filter(rol -> !existentes.contains(rol)).sorted().toList();
        if (!desconocidos.isEmpty()) {
            throw new RolDesconocido(desconocidos);
        }
        String nombre = NombreDeUsuario.normalizar(usuario);
        List<String> ordenados = iniciales.stream().sorted().distinct().toList();
        Instant ahora = reloj.instant();
        if (!asignaciones.inicializar(nombre, ordenados, ahora)) {
            return false;
        }
        registro.registrar(
                new CambioDeRoles(
                        ahora,
                        SISTEMA,
                        CambioDeRoles.Accion.INICIALIZAR,
                        Map.of("usuario", nombre, "roles", ordenados)));
        return true;
    }
}
