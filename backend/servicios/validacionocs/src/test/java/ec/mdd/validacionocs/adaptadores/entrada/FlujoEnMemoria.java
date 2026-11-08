package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * El flujo con el servicio real y sus puertos en memoria, para las pruebas de contrato (F12-H02).
 *
 * <ul>
 *   <li>{@value #PERIODO}: un distributivo con las carreras {@value #DS} y {@value #TDS};
 *   <li>{@value #SIN_DISTRIBUTIVO}: sin distributivo;
 *   <li>{@value #CERRADO}: un periodo cerrado, con distributivo;
 *   <li>{@value #CON_UNIDADES}: con BORRADOR_UNIDADES activa y una sola carrera, {@value #DS}.
 * </ul>
 */
final class FlujoEnMemoria {

    static final String PERIODO = "2026-I";
    static final String SIN_DISTRIBUTIVO = "2027-I";
    static final String CERRADO = "2025-II";
    static final String CON_UNIDADES = "2026-II";
    static final String DS = "550411C02-P-0301";
    static final String TDS = "550613A01-P-0301";

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-07T15:00:00Z");

    private final Map<String, Map<String, Boolean>> enviados = new HashMap<>();
    private final Map<String, ProcesoAprobacion> procesos = new HashMap<>();

    FlujoEnMemoria() {
        reiniciar();
    }

    /** Todo como al empezar: distributivos sin parciales enviados y sin procesos. */
    void reiniciar() {
        enviados.clear();
        procesos.clear();
        for (String periodo : List.of(PERIODO, CERRADO)) {
            Map<String, Boolean> carreras = new LinkedHashMap<>();
            carreras.put(DS, false);
            carreras.put(TDS, false);
            enviados.put(periodo, carreras);
        }
        Map<String, Boolean> una = new LinkedHashMap<>();
        una.put(DS, false);
        enviados.put(CON_UNIDADES, una);
    }

    FlujoDelDistributivoServicio servicio() {
        return new FlujoDelDistributivoServicio(
                distributivo(),
                repositorio(),
                new EtapasAlCrearse(configuraciones(), new LectorYaml(), new PlantillaEnRecursos()),
                datos(),
                cambio -> {},
                new Transaccion() {
                    @Override
                    public <T> T ejecutar(Supplier<T> trabajo) {
                        return trabajo.get();
                    }
                },
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Distributivo distributivo() {
        return new Distributivo() {
            @Override
            public Optional<DistributivoActual> actual(String periodo) {
                return Optional.ofNullable(enviados.get(periodo))
                        .map(carreras -> new DistributivoActual(1, CREADO, carreras));
            }

            @Override
            public ParcialEnviadoEnGeneracion marcarEnviado(String periodo, String carrera) {
                Map<String, Boolean> carreras = enviados.get(periodo);
                if (carreras == null) {
                    throw new SinDistributivo();
                }
                carreras.put(carrera, true);
                return new ParcialEnviadoEnGeneracion(1, "a".repeat(64));
            }

            @Override
            public String hash(String periodo) {
                return "b".repeat(64);
            }
        };
    }

    private RepositorioProceso repositorio() {
        return new RepositorioProceso() {
            @Override
            public Optional<ProcesoAprobacion> de(String periodo) {
                return Optional.ofNullable(procesos.get(periodo));
            }

            @Override
            public Optional<ProcesoAprobacion> bloquear(String periodo) {
                return de(periodo);
            }

            @Override
            public void crear(ProcesoAprobacion proceso, Instant ahora) {
                procesos.put(proceso.periodo(), proceso);
            }

            @Override
            public void guardar(ProcesoAprobacion proceso, List<Transicion> nuevas) {
                procesos.put(proceso.periodo(), proceso);
            }
        };
    }

    private static RepositorioConfiguracion configuraciones() {
        String conUnidades =
                new PlantillaEnRecursos()
                        .texto()
                        .replaceFirst("(BORRADOR_UNIDADES:\\s*\\n\\s*activa:) false", "$1 true");
        return new RepositorioConfiguracion() {
            @Override
            public void guardar(String periodo, String contenido, String por, Instant en) {
                throw new UnsupportedOperationException("El flujo no carga configuraciones");
            }

            @Override
            public Optional<CargaGuardada> vigente(String periodo) {
                return Optional.empty();
            }

            @Override
            public Optional<CargaGuardada> vigenteEn(String periodo, Instant instante) {
                return CON_UNIDADES.equals(periodo)
                        ? Optional.of(new CargaGuardada(1, conUnidades, "admin.demo", CREADO))
                        : Optional.empty();
            }
        };
    }

    private static DatosDelPeriodo datos() {
        return new DatosDelPeriodo() {
            @Override
            public Optional<EstadoDelPeriodo> estado(String periodo) {
                return Optional.of(
                        new EstadoDelPeriodo(
                                CERRADO.equals(periodo) ? "CERRADO" : "PLANIFICACION"));
            }

            @Override
            public Firmantes firmantes(String periodo) {
                return new Firmantes(Set.of(), true);
            }
        };
    }
}
