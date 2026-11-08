package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.ConsultarBitacora;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * La consulta de la bitácora (F17-H02): valida el filtro y la página, fija el corte con el reloj si
 * no viene (research §2) y devuelve las entradas tal como están guardadas. Solo lee (FR-006); no
 * agrega ni reconstruye datos personales de un docente (D-91).
 */
public final class ConsultarBitacoraServicio implements ConsultarBitacora {

    private final RepositorioBitacora repositorio;
    private final Clock reloj;

    /**
     * @param repositorio la bitácora
     * @param reloj el instante de la consulta, que es el corte de una primera página
     */
    public ConsultarBitacoraServicio(RepositorioBitacora repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public PaginaDeEntradas consultar(
            Filtro filtro, Integer pagina, Integer tamano, Instant corte) {
        FiltroDeBitacora valido =
                FiltroDeBitacora.de(
                        filtro.desde(),
                        filtro.hasta(),
                        filtro.usuario(),
                        filtro.servicio(),
                        filtro.accion(),
                        filtro.identificador(),
                        filtro.periodo());
        PaginaPedida pedida = PaginaPedida.de(pagina, tamano, corte, reloj.instant());
        return repositorio.consultar(valido, pedida);
    }

    @Override
    public List<AccionConocida> acciones() {
        return repositorio.acciones();
    }
}
