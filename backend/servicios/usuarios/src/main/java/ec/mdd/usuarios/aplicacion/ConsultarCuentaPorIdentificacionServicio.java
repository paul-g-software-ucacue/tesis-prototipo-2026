package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.puertos.entrada.ConsultarCuentaPorIdentificacion;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.Optional;

/** La cuenta de una identificación, para Notificaciones (F16-H02, D-110). Solo lee. */
public final class ConsultarCuentaPorIdentificacionServicio
        implements ConsultarCuentaPorIdentificacion {

    private final RepositorioUsuarios repositorio;

    /**
     * @param repositorio las cuentas
     */
    public ConsultarCuentaPorIdentificacionServicio(RepositorioUsuarios repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<ResumenDeUsuario> de(String identificacion) {
        return repositorio
                .buscarPorIdentificacion(identificacion)
                .map(
                        usuario ->
                                new ResumenDeUsuario(
                                        usuario.nombreUsuario(),
                                        usuario.nombreParaMostrar(),
                                        usuario.activo()));
    }
}
