package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarProporcionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ProporcionDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;

/** La meta de la proporción de la versión que rige un periodo: los parámetros de RN-05. */
public final class ConsultarProporcionDelPeriodoServicio implements ConsultarProporcionDelPeriodo {

    private static final String REGLA = "RN-05";

    private final RepositorioVersiones repositorio;

    /** Recibe el repositorio de versiones. */
    public ConsultarProporcionDelPeriodoServicio(RepositorioVersiones repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public ProporcionDeLaVersion proporcion(String periodo) {
        VersionNormativa version =
                repositorio.porPeriodo(periodo).orElseThrow(() -> new PeriodoSinVersion(periodo));
        Regla regla =
                version.regla(REGLA).orElseThrow(() -> sinLaRegla(version, "la regla " + REGLA));
        return new ProporcionDeLaVersion(
                version.numero(),
                parametro(version, regla, "porcentaje_clase"),
                parametro(version, regla, "porcentaje_otras_docencia"));
    }

    private static BigDecimal parametro(VersionNormativa version, Regla regla, String nombre) {
        return regla.parametro(nombre).orElseThrow(() -> sinLaRegla(version, nombre));
    }

    private static IllegalStateException sinLaRegla(VersionNormativa version, String falta) {
        return new IllegalStateException("La versión " + version.numero() + " no tiene " + falta);
    }
}
