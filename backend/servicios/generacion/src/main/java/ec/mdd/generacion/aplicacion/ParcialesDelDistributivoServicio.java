package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ContenidoDelParcial;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo.ParcialYSuEstado;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.util.Map;
import java.util.TreeMap;

/**
 * Los parciales del distributivo para Aprobación (F12-H02, MD-04). Marcar un parcial cambia su
 * estado por orden de Aprobación y no se registra en Generación: la transición la registra
 * Aprobación (D-95). Por eso este servicio no recibe el registro de cambios.
 */
public final class ParcialesDelDistributivoServicio implements ParcialesDelDistributivo {

    private final RepositorioDistributivos distributivos;
    private final Transaccion transaccion;

    /**
     * @param distributivos las versiones del distributivo
     * @param transaccion la transacción de Generación
     */
    public ParcialesDelDistributivoServicio(
            RepositorioDistributivos distributivos, Transaccion transaccion) {
        this.distributivos = distributivos;
        this.transaccion = transaccion;
    }

    @Override
    public EstadoDelDistributivo estadoDelDistributivo(String periodo) {
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(DistributivoNoEncontrado::new);
        return new EstadoDelDistributivo(
                periodo,
                version.numero(),
                distributivos.creadoEn(periodo).orElseThrow(DistributivoNoEncontrado::new),
                version.parciales().stream()
                        .sorted(java.util.Comparator.comparing(ParcialCarrera::carrera))
                        .map(
                                una ->
                                        new ParcialYSuEstado(
                                                una.carrera(),
                                                una.estado() == EstadoDelParcial.ENVIADO))
                        .toList());
    }

    @Override
    public ParcialMarcado marcarParcialEnviado(String periodo, String carrera) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo version =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(DistributivoNoEncontrado::new);
                    ParcialCarrera parcial =
                            version.parcialDe(carrera).orElseThrow(ParcialNoEncontrado::new);
                    // Idempotente: un reintento de Aprobación recibe el mismo hash (research §2).
                    if (parcial.estado() != EstadoDelParcial.ENVIADO) {
                        distributivos.marcarParcial(
                                version.id(), carrera, EstadoDelParcial.ENVIADO);
                    }
                    return new ParcialMarcado(
                            version.numero(),
                            ContenidoDelParcial.de(periodo, carrera, version).hash());
                });
    }

    @Override
    public String hashDelDistributivo(String periodo) {
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(DistributivoNoEncontrado::new);
        Map<String, String> hashes = new TreeMap<>();
        version.parciales()
                .forEach(
                        parcial ->
                                hashes.put(
                                        parcial.carrera(),
                                        ContenidoDelParcial.de(periodo, parcial.carrera(), version)
                                                .hash()));
        return ContenidoDelParcial.hashDelDistributivo(periodo, version.numero(), hashes);
    }
}
