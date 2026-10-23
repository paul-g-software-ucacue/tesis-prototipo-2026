package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.ConsultarEstructura;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import java.util.List;

/** Consultar la estructura orgánica y el catálogo de gestiones autorizadas. */
public final class ConsultarEstructuraServicio implements ConsultarEstructura {

    private final RepositorioEstructura repositorio;

    /** Recibe el repositorio de la estructura. */
    public ConsultarEstructuraServicio(RepositorioEstructura repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<UnidadOrganica> unidades() {
        return repositorio.todas();
    }

    @Override
    public UnidadOrganica unidad(String codigo) {
        return repositorio.todas().stream()
                .filter(unidad -> unidad.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new UnidadNoEncontrada(codigo));
    }

    @Override
    public List<GestionAutorizada> gestionesAutorizadas() {
        return repositorio.catalogo();
    }
}
