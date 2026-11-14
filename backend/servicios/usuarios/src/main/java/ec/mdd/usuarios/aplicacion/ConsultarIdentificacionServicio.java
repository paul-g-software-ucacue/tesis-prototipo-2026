package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.ConsultarIdentificacion;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;

/**
 * La identificación de una cuenta para otro proceso (F14-H01, D-102). Sin cuenta y sin
 * identificación lanza lo mismo, para no decir cuál de los dos es.
 */
public final class ConsultarIdentificacionServicio implements ConsultarIdentificacion {

    private final RepositorioUsuarios repositorio;

    /**
     * @param repositorio las cuentas
     */
    public ConsultarIdentificacionServicio(RepositorioUsuarios repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public String de(String usuario) {
        return repositorio
                .buscarPorNombre(usuario)
                .flatMap(Usuario::identificacion)
                .orElseThrow(SinIdentificacion::new);
    }
}
