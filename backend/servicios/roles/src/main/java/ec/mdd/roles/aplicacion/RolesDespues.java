package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.Rol;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Los roles de cada usuario tal como quedarían después de un cambio, para las reglas. */
final class RolesDespues {

    private RolesDespues() {}

    /**
     * @param asignaciones identificadores de los roles de cada usuario, antes del cambio
     * @param roles todos los roles, ya con el cambio
     * @param usuario el usuario cuyos roles cambian; {@code null} si no cambia ninguno
     * @param nuevos sus roles después del cambio
     */
    static Map<String, List<Rol>> de(
            Map<String, List<String>> asignaciones,
            Collection<Rol> roles,
            String usuario,
            Collection<String> nuevos) {
        Map<String, Rol> porIdentificador =
                roles.stream().collect(Collectors.toMap(Rol::identificador, Function.identity()));
        Map<String, List<String>> identificadores = new HashMap<>(asignaciones);
        if (usuario != null) {
            identificadores.put(usuario, List.copyOf(nuevos));
        }
        Map<String, List<Rol>> despues = new HashMap<>();
        identificadores.forEach(
                (nombre, suyos) ->
                        despues.put(
                                nombre,
                                suyos.stream()
                                        .map(porIdentificador::get)
                                        .filter(java.util.Objects::nonNull)
                                        .toList()));
        return despues;
    }
}
