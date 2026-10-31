package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarPesosDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.PesosDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;

/** Los pesos y el mínimo de afinidad de la versión que rige un periodo (RN-24, RN-19). */
public final class ConsultarPesosDelPeriodoServicio implements ConsultarPesosDelPeriodo {

    private final RepositorioVersiones repositorio;

    /** Recibe el repositorio de versiones. */
    public ConsultarPesosDelPeriodoServicio(RepositorioVersiones repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public PesosDeLaVersion pesos(String periodo) {
        VersionNormativa version =
                repositorio.porPeriodo(periodo).orElseThrow(() -> new PeriodoSinVersion(periodo));
        return new PesosDeLaVersion(
                version.numero(),
                version.pesosAfinidad()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "La versión "
                                                        + version.numero()
                                                        + " no tiene pesos de afinidad (D-68)")),
                version.minimoAfinidad()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "La versión "
                                                        + version.numero()
                                                        + " no tiene mínimo de afinidad (D-68)")));
    }
}
