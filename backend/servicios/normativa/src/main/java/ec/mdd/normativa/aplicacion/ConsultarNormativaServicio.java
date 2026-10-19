package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.util.List;

/** La consulta de las versiones de normativa, tal como las guarda el repositorio. */
public final class ConsultarNormativaServicio implements ConsultarNormativa {

    private final RepositorioVersiones repositorio;

    /** Recibe el repositorio de versiones. */
    public ConsultarNormativaServicio(RepositorioVersiones repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<VersionNormativa> versiones() {
        return repositorio.todas();
    }

    @Override
    public VersionNormativa version(int numero) {
        return repositorio.buscar(numero).orElseThrow(() -> new VersionNoEncontrada(numero));
    }

    @Override
    public AsignacionDelPeriodo periodo(String periodo) {
        return repositorio
                .porPeriodo(periodo)
                .flatMap(version -> AsignacionesDeVersion.del(version, periodo))
                .orElseThrow(() -> new PeriodoSinVersion(periodo));
    }
}
