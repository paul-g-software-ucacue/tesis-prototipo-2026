package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import ec.mdd.roles.adaptadores.entrada.api.Funcionalidad;
import ec.mdd.roles.adaptadores.entrada.api.FuncionalidadesApi;
import ec.mdd.roles.adaptadores.entrada.api.Permiso;
import ec.mdd.roles.adaptadores.entrada.api.Rol;
import ec.mdd.roles.adaptadores.entrada.api.RolEditable;
import ec.mdd.roles.adaptadores.entrada.api.RolesApi;
import ec.mdd.roles.adaptadores.entrada.api.RolesAsignados;
import ec.mdd.roles.adaptadores.entrada.api.RolesDeUsuario;
import ec.mdd.roles.adaptadores.entrada.api.RolesDeUsuariosApi;
import ec.mdd.roles.aplicacion.RolNoEncontrado;
import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.PermisosInvalidos;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.entrada.AsignarRoles;
import ec.mdd.roles.puertos.entrada.DatosDeRol;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import ec.mdd.roles.puertos.entrada.RolConUsuarios;
import ec.mdd.roles.puertos.entrada.RolDesconocido;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las funcionalidades, los roles y las asignaciones de rol ({@code contratos/roles/openapi.yaml},
 * RF-02, RF-03).
 *
 * <p>Cada cambio lleva el usuario de la sesión, para el registro (FR-011). Las reglas de la
 * administración responden 409 con los códigos de {@link ErroresRoles}; un permiso o un rol que no
 * existe, 400 con el detalle del campo; un rol que no existe en la ruta, 404.
 *
 * <p>Cada operación exige el permiso de su {@code x-mdd-permiso} con {@code @PreAuthorize}
 * (ADR-021); {@code PermisosRolesTest} comprueba que coinciden.
 */
@RestController
class RolesControlador implements FuncionalidadesApi, RolesApi, RolesDeUsuariosApi {

    private static final String LECTURA = "hasAuthority('PERMISO_USUARIOS_ROLES:LECTURA')";
    private static final String ESCRITURA = "hasAuthority('PERMISO_USUARIOS_ROLES:ESCRITURA')";

    private final GestionarRoles gestionar;
    private final AsignarRoles asignar;

    RolesControlador(GestionarRoles gestionar, AsignarRoles asignar) {
        this.gestionar = gestionar;
        this.asignar = asignar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Funcionalidad>> listarFuncionalidades() {
        return ResponseEntity.ok(
                gestionar.funcionalidades().stream()
                        .map(
                                funcionalidad ->
                                        new Funcionalidad(
                                                funcionalidad.codigo(),
                                                funcionalidad.nombre(),
                                                funcionalidad.tipos().stream()
                                                        .map(
                                                                tipo ->
                                                                        ec.mdd.roles.adaptadores
                                                                                .entrada.api
                                                                                .TipoDePermiso
                                                                                .fromValue(
                                                                                        tipo
                                                                                                .name()))
                                                        .toList()))
                        .toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(
                gestionar.roles().stream().map(RolesControlador::aRespuesta).toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Rol> consultarRol(String identificador) {
        return ResponseEntity.ok(aRespuesta(existente(() -> gestionar.consultar(identificador))));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Rol> crearRol(RolEditable rol) {
        RolConUsuarios creado = conPermisosValidos(() -> gestionar.crear(datos(rol), quien()));
        return ResponseEntity.status(HttpStatus.CREATED).body(aRespuesta(creado));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Rol> editarRol(String identificador, RolEditable rol) {
        RolConUsuarios editado =
                existente(
                        () ->
                                conPermisosValidos(
                                        () ->
                                                gestionar.editar(
                                                        identificador, datos(rol), quien())));
        return ResponseEntity.ok(aRespuesta(editado));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Void> borrarRol(String identificador) {
        existente(
                () -> {
                    gestionar.borrar(identificador, quien());
                    return null;
                });
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<RolesDeUsuario>> listarRolesDeUsuarios() {
        return ResponseEntity.ok(
                asignar.rolesDeUsuarios().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .map(fila -> new RolesDeUsuario(fila.getKey(), fila.getValue()))
                        .toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<RolesDeUsuario> asignarRoles(String usuario, RolesAsignados roles) {
        try {
            List<String> asignados = asignar.asignar(usuario, roles.getRoles(), quien());
            return ResponseEntity.ok(
                    new RolesDeUsuario(
                            ec.mdd.roles.dominio.NombreDeUsuario.normalizar(usuario), asignados));
        } catch (RolDesconocido error) {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    "roles",
                                    "No existen estos roles: "
                                            + String.join(", ", error.roles())
                                            + ".")));
        }
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static DatosDeRol datos(RolEditable rol) {
        return new DatosDeRol(
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getPermisos().stream()
                        .map(
                                permiso ->
                                        new ec.mdd.roles.dominio.Permiso(
                                                permiso.getFuncionalidad(),
                                                TipoDePermiso.valueOf(permiso.getTipo().getValue()),
                                                Alcance.valueOf(permiso.getAlcance().getValue())))
                        .toList());
    }

    private static Rol aRespuesta(RolConUsuarios conUsuarios) {
        ec.mdd.roles.dominio.Rol rol = conUsuarios.rol();
        return new Rol(
                rol.identificador(),
                rol.nombre(),
                rol.descripcion(),
                rol.predefinido(),
                rol.permisos().stream()
                        .map(
                                permiso ->
                                        new Permiso(
                                                permiso.funcionalidad(),
                                                ec.mdd.roles.adaptadores.entrada.api.TipoDePermiso
                                                        .fromValue(permiso.tipo().name()),
                                                ec.mdd.roles.adaptadores.entrada.api.Alcance
                                                        .fromValue(permiso.alcance().name())))
                        .toList(),
                conUsuarios.usuarios());
    }

    // Un rol que no existe en la ruta: 404 con el código común (COMUN_RECURSO_NO_ENCONTRADO).
    private static <T> T existente(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (RolNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    // Un permiso que el contrato no puede rechazar: 400 con el detalle del campo permisos.
    private static <T> T conPermisosValidos(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (PermisosInvalidos error) {
            throw new SolicitudInvalida(
                    List.of(new DetalleRespuesta("permisos", error.getMessage())));
        }
    }
}
