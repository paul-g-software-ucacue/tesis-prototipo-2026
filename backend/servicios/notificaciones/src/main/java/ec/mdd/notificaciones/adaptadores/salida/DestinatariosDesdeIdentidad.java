package ec.mdd.notificaciones.adaptadores.salida;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import ec.mdd.roles.puertos.entrada.ConsultarDestinatarios;
import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Destinatarios} sobre las interfaces publicadas de Roles y Usuarios en el mismo proceso
 * {@code identidad} (principio V, D-108): Roles dice quién tiene el rol, el designado del periodo o
 * la carrera; Usuarios, quién está activo y su nombre. Solo lee.
 */
public final class DestinatariosDesdeIdentidad implements Destinatarios {

    private final ConsultarDestinatarios roles;
    private final ListarUsuarios usuarios;

    /**
     * @param roles el caso de uso publicado por Roles
     * @param usuarios el caso de uso publicado por Usuarios
     */
    public DestinatariosDesdeIdentidad(ConsultarDestinatarios roles, ListarUsuarios usuarios) {
        this.roles = roles;
        this.usuarios = usuarios;
    }

    @Override
    public List<Destinatario> de(DestinoDelMensaje destino, String periodo) {
        Map<String, ResumenDeUsuario> porUsuario = porUsuario();
        return roles.destinatarios(destino.rol(), periodo, destino.carrera()).stream()
                .filter(
                        uno -> {
                            ResumenDeUsuario usuario = porUsuario.get(uno.usuario());
                            return usuario != null && usuario.activo();
                        })
                .map(uno -> new Destinatario(uno.usuario(), uno.carrera()))
                .toList();
    }

    @Override
    public Optional<String> nombre(String usuario) {
        return Optional.ofNullable(porUsuario().get(usuario))
                .map(ResumenDeUsuario::nombre)
                .filter(nombre -> !nombre.isBlank());
    }

    private Map<String, ResumenDeUsuario> porUsuario() {
        return usuarios.listar().stream()
                .collect(
                        Collectors.toMap(
                                ResumenDeUsuario::usuario,
                                Function.identity(),
                                (primero, segundo) -> primero));
    }
}
