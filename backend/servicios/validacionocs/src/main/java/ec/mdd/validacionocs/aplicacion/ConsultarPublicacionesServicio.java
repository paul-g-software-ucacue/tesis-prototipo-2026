package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarPublicaciones;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import java.util.List;
import java.util.Optional;

/**
 * La publicación vigente de cada periodo (F20-H01, D-115). La decide el agregado: la última
 * transición a PUBLICADO, sin un estado HISTÓRICA ni otra tabla (D-113).
 */
public final class ConsultarPublicacionesServicio implements ConsultarPublicaciones {

    private final RepositorioProceso procesos;

    /**
     * @param procesos los procesos de aprobación
     */
    public ConsultarPublicacionesServicio(RepositorioProceso procesos) {
        this.procesos = procesos;
    }

    @Override
    public List<PublicacionVigente> vigentes() {
        return procesos.conPublicacion().stream()
                .map(procesos::de)
                .flatMap(Optional::stream)
                .map(ConsultarPublicacionesServicio::vigente)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<PublicacionVigente> vigente(ProcesoAprobacion proceso) {
        return proceso.publicacion()
                .map(
                        una ->
                                new PublicacionVigente(
                                        proceso.periodo(),
                                        una.version(),
                                        una.hash(),
                                        una.quien(),
                                        una.momento(),
                                        proceso.reformaEnCurso().isPresent()));
    }
}
