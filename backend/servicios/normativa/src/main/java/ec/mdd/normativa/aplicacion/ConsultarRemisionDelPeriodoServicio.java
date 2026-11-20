package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarRemisionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.RemisionDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;

/**
 * Los días de RN-23 de la versión que rige un periodo (F12-H03, D-114): el parámetro {@code
 * dias_antes_del_inicio}, que V7 agregó a cada versión con los 30 días de POL 5.2. Es una cifra de
 * la política, así que vive en la versión y no en el código (principio VI).
 */
public final class ConsultarRemisionDelPeriodoServicio implements ConsultarRemisionDelPeriodo {

    private static final String REGLA = "RN-23";
    private static final String PARAMETRO = "dias_antes_del_inicio";

    private final RepositorioVersiones repositorio;

    /** Recibe el repositorio de versiones. */
    public ConsultarRemisionDelPeriodoServicio(RepositorioVersiones repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public RemisionDeLaVersion remision(String periodo) {
        VersionNormativa version =
                repositorio.porPeriodo(periodo).orElseThrow(() -> new PeriodoSinVersion(periodo));
        Regla regla =
                version.regla(REGLA).orElseThrow(() -> sinLaRegla(version, "la regla " + REGLA));
        return new RemisionDeLaVersion(
                version.numero(),
                regla.parametro(PARAMETRO)
                        .orElseThrow(() -> sinLaRegla(version, PARAMETRO))
                        .intValueExact());
    }

    private static IllegalStateException sinLaRegla(VersionNormativa version, String falta) {
        return new IllegalStateException("La versión " + version.numero() + " no tiene " + falta);
    }
}
