package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado;
import ec.mdd.generacion.puertos.salida.RepositorioContenidoCongelado;
import java.util.Optional;

/**
 * El contenido congelado de una versión, para Reporting (F20-H01, D-115). Si una versión congelada
 * no lo tiene, lo calcula y lo guarda como tardío antes de leerlo.
 */
public final class ConsultarContenidoCongeladoServicio implements ConsultarContenidoCongelado {

    private final GuardarContenidoCongeladoServicio guardado;
    private final RepositorioContenidoCongelado repositorio;

    /**
     * @param guardado el guardado, para el caso tardío
     * @param repositorio el contenido congelado
     */
    public ConsultarContenidoCongeladoServicio(
            GuardarContenidoCongeladoServicio guardado, RepositorioContenidoCongelado repositorio) {
        this.guardado = guardado;
        this.repositorio = repositorio;
    }

    @Override
    public Optional<CargasCongeladas> cargas(String periodo, int version) {
        if (!guardado.guardarSiFalta(periodo, version, true)) {
            return Optional.empty();
        }
        return repositorio
                .hash(periodo, version)
                .map(hash -> new CargasCongeladas(hash, repositorio.docentes(periodo, version)));
    }

    @Override
    public Optional<CargaDeLaVersion> carga(String periodo, int version, String docente) {
        if (!guardado.guardarSiFalta(periodo, version, true)) {
            return Optional.empty();
        }
        return repositorio
                .hash(periodo, version)
                .flatMap(
                        hash ->
                                repositorio
                                        .carga(periodo, version, docente)
                                        .map(carga -> new CargaDeLaVersion(hash, carga)));
    }
}
