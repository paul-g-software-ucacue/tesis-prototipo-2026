package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.entrada.VersionCongeladaEnGeneracion;
import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El distributivo, por la interfaz publicada de Generación en el mismo proceso (MD-04, principio V,
 * F12-H02). No lee tablas de Generación. Traduce sus excepciones a las de Aprobación; desde F13-H01
 * también le ordena congelar la versión, que Generación hace en su propia transacción.
 */
public final class DistributivoDesdeGeneracion implements Distributivo {

    private final ParcialesDelDistributivo parciales;

    /**
     * @param parciales la interfaz publicada de Generación
     */
    public DistributivoDesdeGeneracion(ParcialesDelDistributivo parciales) {
        this.parciales = parciales;
    }

    @Override
    public Optional<DistributivoActual> actual(String periodo) {
        EstadoDelDistributivo estado;
        try {
            estado = parciales.estadoDelDistributivo(periodo);
        } catch (DistributivoNoEncontrado sinDistributivo) {
            return Optional.empty();
        }
        Map<String, Boolean> enviados = new LinkedHashMap<>();
        estado.parciales().forEach(uno -> enviados.put(uno.carrera(), uno.enviado()));
        return Optional.of(new DistributivoActual(estado.version(), estado.creadoEn(), enviados));
    }

    @Override
    public ParcialEnviadoEnGeneracion marcarEnviado(String periodo, String carrera) {
        try {
            ParcialMarcado marcado = parciales.marcarParcialEnviado(periodo, carrera);
            return new ParcialEnviadoEnGeneracion(marcado.version(), marcado.hash());
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        } catch (ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado sinParcial) {
            throw new ParcialNoEncontrado();
        }
    }

    @Override
    public int marcarDevueltos(String periodo, List<String> carreras) {
        try {
            return parciales.marcarParcialesDevueltos(periodo, carreras);
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        } catch (ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado sinParcial) {
            throw new ParcialNoEncontrado();
        }
    }

    // F13-H01: Generación congela con la versión bloqueada y es idempotente (D-100).
    @Override
    public VersionCongeladaEnAprobacion congelar(String periodo) {
        try {
            VersionCongeladaEnGeneracion congelada = parciales.congelarVersion(periodo);
            return new VersionCongeladaEnAprobacion(congelada.version(), congelada.hash());
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }

    // F12-H04: el único motivo que Aprobación ordena hoy es el rechazo en firma.
    @Override
    public Optional<VersionNuevaEnAprobacion> crearVersionNueva(
            String periodo, int anulada, java.time.Instant momento) {
        try {
            return parciales
                    .crearVersionNueva(
                            periodo,
                            anulada,
                            ec.mdd.generacion.puertos.entrada.MotivoDeVersion.RECHAZO_EN_FIRMA,
                            "sistema",
                            momento)
                    .map(nueva -> new VersionNuevaEnAprobacion(nueva.numero(), nueva.hash()));
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }

    // F18-H02, D-113: la reforma, que deja la publicada CONGELADA; la crea quien la inició.
    @Override
    public Optional<VersionNuevaEnAprobacion> crearVersionDeReforma(
            String periodo, int publicada, String quien, java.time.Instant momento) {
        try {
            return parciales
                    .crearVersionNueva(
                            periodo,
                            publicada,
                            ec.mdd.generacion.puertos.entrada.MotivoDeVersion.REFORMA,
                            quien,
                            momento)
                    .map(nueva -> new VersionNuevaEnAprobacion(nueva.numero(), nueva.hash()));
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }

    @Override
    public String hash(String periodo) {
        try {
            return parciales.hashDelDistributivo(periodo);
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }

    @Override
    public CondicionesDeRevision condicionesDeRevision(String periodo) {
        try {
            ec.mdd.generacion.puertos.entrada.CondicionesDeRevision condiciones =
                    parciales.condicionesDeRevision(periodo);
            return new CondicionesDeRevision(
                    condiciones.consolidado(),
                    condiciones.desactualizado(),
                    condiciones.informe(),
                    condiciones.duras(),
                    condiciones.blandas());
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }
}
