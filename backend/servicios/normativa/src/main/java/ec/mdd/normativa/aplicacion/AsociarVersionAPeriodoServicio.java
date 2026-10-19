package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.normativa.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Asociar una versión a un periodo. Primero se bloquea el periodo, para que dos asociaciones al
 * mismo no se crucen; después la versión, para que una edición en curso termine antes de que la
 * versión quede en uso (research §3).
 */
public final class AsociarVersionAPeriodoServicio implements AsociarVersionAPeriodo {

    private final RepositorioVersiones repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public AsociarVersionAPeriodoServicio(
            RepositorioVersiones repositorio,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public AsignacionDelPeriodo asociar(String periodo, int version, String quien) {
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquearPeriodo(periodo);
                    Optional<VersionNormativa> actual = repositorio.porPeriodo(periodo);
                    if (actual.isPresent()) {
                        if (actual.get().numero() != version) {
                            throw new PeriodoConVersion(periodo, actual.get().numero());
                        }
                        return AsignacionesDeVersion.del(actual.get(), periodo).orElseThrow();
                    }
                    VersionNormativa elegida =
                            repositorio
                                    .bloquear(version)
                                    .orElseThrow(() -> new VersionNoEncontrada(version));
                    Instant ahora = reloj.instant();
                    repositorio.guardar(elegida.asociadaA(periodo, ahora), quien, ahora);
                    registro.registrar(
                            new CambioDeNormativa(
                                    ahora,
                                    quien,
                                    CambioDeNormativa.Accion.VERSION_ASOCIADA,
                                    Map.of("numero", version, "periodo", periodo)));
                    return new AsignacionDelPeriodo(periodo, version, ahora);
                });
    }
}
