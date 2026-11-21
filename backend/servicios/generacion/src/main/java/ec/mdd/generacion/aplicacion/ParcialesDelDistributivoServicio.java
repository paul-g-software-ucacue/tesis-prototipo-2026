package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.ContenidoDelParcial;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.CondicionesDeRevision;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo.ParcialYSuEstado;
import ec.mdd.generacion.puertos.entrada.MotivoDeVersion;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.entrada.VersionCongeladaEnGeneracion;
import ec.mdd.generacion.puertos.entrada.VersionNuevaEnGeneracion;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.util.List;

/**
 * Los parciales del distributivo para Aprobación (F12-H02, MD-04). Marcar un parcial cambia su
 * estado por orden de Aprobación y no se registra en Generación: la transición la registra
 * Aprobación (D-95); lo mismo al congelar la versión (F13-H01, D-100). Por eso este servicio no
 * recibe el registro de cambios. Las condiciones para enviar a revisión salen del consolidado, que
 * ya arma el panel (F10-H03): no se evalúan dos veces.
 */
public final class ParcialesDelDistributivoServicio implements ParcialesDelDistributivo {

    private final RepositorioDistributivos distributivos;
    private final Transaccion transaccion;
    private final ConsultarConsolidado consolidado;

    /**
     * @param distributivos las versiones del distributivo
     * @param transaccion la transacción de Generación
     * @param consolidado el consolidado, con su panel (F10-H03)
     */
    public ParcialesDelDistributivoServicio(
            RepositorioDistributivos distributivos,
            Transaccion transaccion,
            ConsultarConsolidado consolidado) {
        this.distributivos = distributivos;
        this.transaccion = transaccion;
        this.consolidado = consolidado;
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
    public int marcarParcialesDevueltos(String periodo, List<String> carreras) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo version =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(DistributivoNoEncontrado::new);
                    // Todo o nada (FR-005): primero se comprueban todas, después se marcan.
                    List<ParcialCarrera> parciales =
                            carreras.stream()
                                    .map(
                                            carrera ->
                                                    version.parcialDe(carrera)
                                                            .orElseThrow(ParcialNoEncontrado::new))
                                    .toList();
                    parciales.stream()
                            .filter(parcial -> parcial.estado() != EstadoDelParcial.DEVUELTO)
                            .forEach(
                                    parcial ->
                                            distributivos.marcarParcial(
                                                    version.id(),
                                                    parcial.carrera(),
                                                    EstadoDelParcial.DEVUELTO));
                    return version.numero();
                });
    }

    // F13-H01, D-100: con la versión bloqueada, como marcar un parcial; ninguna propuesta queda
    // después. Idempotente: un reintento de Aprobación recibe el mismo hash.
    @Override
    public VersionCongeladaEnGeneracion congelarVersion(String periodo) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo version =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(DistributivoNoEncontrado::new);
                    if (version.estado() != EstadoDeVersion.CONGELADA) {
                        distributivos.congelar(version.id());
                    }
                    return new VersionCongeladaEnGeneracion(
                            version.numero(), hashDe(periodo, version));
                });
    }

    /** Quién crea la versión nueva: la ordena Aprobación como el sistema (D-106). */
    private static final String SISTEMA = "sistema";

    // F12-H04, D-106: con la versión bloqueada, como congelar. Idempotente: si la vigente ya es la
    // siguiente, la devuelve. F18-H02, D-113: en una reforma la anterior sigue CONGELADA, así que
    // el
    // reintento se reconoce por el motivo de la siguiente; en un rechazo, por la anterior ANULADA.
    @Override
    public java.util.Optional<VersionNuevaEnGeneracion> crearVersionNueva(
            String periodo,
            int anulada,
            MotivoDeVersion motivo,
            String quien,
            java.time.Instant momento) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo vigente =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(DistributivoNoEncontrado::new);
                    if (vigente.numero() == anulada
                            && vigente.estado() == EstadoDeVersion.CONGELADA) {
                        distributivos.crearVersionNueva(vigente.id(), motivo, quien, momento);
                        return java.util.Optional.of(
                                nueva(
                                        periodo,
                                        distributivos
                                                .vigenteDe(periodo)
                                                .orElseThrow(DistributivoNoEncontrado::new)));
                    }
                    if (vigente.numero() == anulada + 1 && yaCreada(periodo, anulada, motivo)) {
                        return java.util.Optional.of(nueva(periodo, vigente));
                    }
                    return java.util.Optional.empty();
                });
    }

    private boolean yaCreada(String periodo, int anterior, MotivoDeVersion motivo) {
        if (motivo == MotivoDeVersion.REFORMA) {
            return distributivos
                            .estadoDe(periodo, anterior)
                            .filter(EstadoDeVersion.CONGELADA::equals)
                            .isPresent()
                    && distributivos
                            .motivoDe(periodo, anterior + 1)
                            .filter(MotivoDeVersion.REFORMA::equals)
                            .isPresent();
        }
        return distributivos
                .estadoDe(periodo, anterior)
                .filter(EstadoDeVersion.ANULADA::equals)
                .isPresent();
    }

    private static VersionNuevaEnGeneracion nueva(String periodo, VersionDistributivo version) {
        return new VersionNuevaEnGeneracion(version.numero(), hashDe(periodo, version));
    }

    @Override
    public String hashDelDistributivo(String periodo) {
        return hashDe(
                periodo,
                distributivos.vigenteDe(periodo).orElseThrow(DistributivoNoEncontrado::new));
    }

    private static String hashDe(String periodo, VersionDistributivo version) {
        return ContenidoDelParcial.hashDeLaVersion(periodo, version);
    }

    // Solo lee: el consolidado se calcula al pedirlo, con el panel (F10-H03, research §1). Las
    // reglas sin evaluar no están en las abiertas, así que no bloquean.
    @Override
    public CondicionesDeRevision condicionesDeRevision(String periodo) {
        ConsolidadoDelPeriodo actual = consolidado.consolidado(periodo);
        if (!actual.creado()) {
            throw new DistributivoNoEncontrado();
        }
        return new CondicionesDeRevision(
                actual.numero(),
                actual.consolidacion().isPresent(),
                actual.desactualizado(),
                actual.informe().map(InformeSustentado::huella),
                actual.duras(),
                actual.blandas());
    }
}
