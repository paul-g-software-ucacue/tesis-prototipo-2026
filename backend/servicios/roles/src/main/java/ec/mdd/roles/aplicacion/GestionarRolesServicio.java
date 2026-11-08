package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.CatalogoDeFuncionalidades;
import ec.mdd.roles.dominio.Funcionalidad;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.ReglaDeAdministracion;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.puertos.entrada.DatosDeRol;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import ec.mdd.roles.puertos.entrada.RolConUsuarios;
import ec.mdd.roles.puertos.entrada.RolNoEncontrado;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** La administración de roles con las reglas de {@link ReglaDeAdministracion} y su registro. */
public final class GestionarRolesServicio implements GestionarRoles {

    private final RepositorioRoles roles;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * Recibe los puertos de salida y el reloj. El rol y su registro van en una transacción: si no,
     * la entrada de la bitácora no se publica (F17-H01).
     */
    public GestionarRolesServicio(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.roles = roles;
        this.asignaciones = asignaciones;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public List<Funcionalidad> funcionalidades() {
        return roles.funcionalidades().funcionalidades();
    }

    @Override
    public List<RolConUsuarios> roles() {
        Map<String, Integer> usuarios = asignaciones.usuariosPorRol();
        return roles.todos().stream()
                .map(rol -> new RolConUsuarios(rol, usuarios.getOrDefault(rol.identificador(), 0)))
                .toList();
    }

    @Override
    public RolConUsuarios consultar(String identificador) {
        return conUsuarios(existente(identificador));
    }

    @Override
    public RolConUsuarios crear(DatosDeRol datos, String quien) {
        CatalogoDeFuncionalidades catalogo = roles.funcionalidades();
        catalogo.validar(datos.permisos());
        ReglaDeAdministracion.nombreDisponible(datos.nombre(), null, roles.todos());
        Rol rol = Rol.nuevo(datos.nombre(), datos.descripcion(), datos.permisos());
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    roles.guardar(rol, ahora);
                    registrar(ahora, quien, CambioDeRoles.Accion.CREAR_ROL, rol, Map.of());
                    return rol;
                });
        return new RolConUsuarios(rol, 0);
    }

    @Override
    public RolConUsuarios editar(String identificador, DatosDeRol datos, String quien) {
        Rol actual = existente(identificador);
        ReglaDeAdministracion.editar(actual);
        roles.funcionalidades().validar(datos.permisos());
        List<Rol> todos = roles.todos();
        ReglaDeAdministracion.nombreDisponible(datos.nombre(), identificador, todos);
        Rol editado = actual.editado(datos.nombre(), datos.descripcion(), datos.permisos());
        List<Rol> conElCambio =
                todos.stream()
                        .map(rol -> rol.identificador().equals(identificador) ? editado : rol)
                        .toList();
        ReglaDeAdministracion.conservaAdministrador(
                RolesDespues.de(asignaciones.todas(), conElCambio, null, List.of()), quien);
        // Los mismos datos no cambian nada: no se guarda ni se registra (D-94).
        if (editado.mismosDatosQue(actual)) {
            return conUsuarios(actual);
        }
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    roles.guardar(editado, ahora);
                    registrar(
                            ahora,
                            quien,
                            CambioDeRoles.Accion.EDITAR_ROL,
                            editado,
                            valores(actual));
                    return editado;
                });
        return conUsuarios(editado);
    }

    @Override
    public void borrar(String identificador, String quien) {
        Rol actual = existente(identificador);
        ReglaDeAdministracion.borrar(
                actual, asignaciones.usuariosPorRol().getOrDefault(identificador, 0));
        transaccion.ejecutar(
                () -> {
                    roles.borrar(identificador);
                    registro.registrar(
                            new CambioDeRoles(
                                    reloj.instant(),
                                    quien,
                                    CambioDeRoles.Accion.BORRAR_ROL,
                                    Map.of(
                                            "identificador",
                                            identificador,
                                            "nombre",
                                            actual.nombre()),
                                    valores(actual),
                                    Map.of()));
                    return identificador;
                });
    }

    private Rol existente(String identificador) {
        return roles.buscar(identificador).orElseThrow(() -> new RolNoEncontrado(identificador));
    }

    private RolConUsuarios conUsuarios(Rol rol) {
        return new RolConUsuarios(
                rol, asignaciones.usuariosPorRol().getOrDefault(rol.identificador(), 0));
    }

    private void registrar(
            Instant ahora,
            String quien,
            CambioDeRoles.Accion accion,
            Rol rol,
            Map<String, Object> anterior) {
        registro.registrar(
                new CambioDeRoles(
                        ahora,
                        quien,
                        accion,
                        Map.of(
                                "identificador",
                                rol.identificador(),
                                "nombre",
                                rol.nombre(),
                                "permisos",
                                permisos(rol)),
                        anterior,
                        valores(rol)));
    }

    /** Lo que se edita de un rol: su nombre, su descripción y sus permisos. */
    private static Map<String, Object> valores(Rol rol) {
        return Map.of(
                "nombre",
                rol.nombre(),
                "descripcion",
                rol.descripcion(),
                "permisos",
                permisos(rol));
    }

    private static List<String> permisos(Rol rol) {
        return rol.permisos().stream().map(Permiso::texto).toList();
    }
}
