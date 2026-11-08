package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * El distributivo, por la interfaz publicada de Generación en el mismo proceso (MD-04, principio V,
 * F12-H02). No lee tablas de Generación.
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
    public String hash(String periodo) {
        try {
            return parciales.hashDelDistributivo(periodo);
        } catch (DistributivoNoEncontrado sinDistributivo) {
            throw new SinDistributivo();
        }
    }
}
