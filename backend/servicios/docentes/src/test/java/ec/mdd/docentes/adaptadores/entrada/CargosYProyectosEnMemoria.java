package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.aplicacion.ConsultarCargosServicio;
import ec.mdd.docentes.aplicacion.ConsultarDocentesServicio;
import ec.mdd.docentes.aplicacion.ConsultarProyectosServicio;
import ec.mdd.docentes.aplicacion.MantenerCargosServicio;
import ec.mdd.docentes.aplicacion.MantenerProyectosServicio;
import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los cargos y los proyectos en memoria para las pruebas de contrato (F03-H05), con los servicios y
 * las reglas reales del dominio. Los docentes, el registro y la transacción son los de {@link
 * DocentesEnMemoria}; las referencias, las de las cargas 01 a 04 de prueba.
 */
final class CargosYProyectosEnMemoria {

    /** El reloj de las pruebas: el 2026-10-25. */
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC);

    private final DocentesEnMemoria docentes;
    private final Cargos cargos = new Cargos();
    private final Proyectos proyectos = new Proyectos();

    CargosYProyectosEnMemoria(DocentesEnMemoria docentes) {
        this.docentes = docentes;
    }

    void reiniciar() {
        cargos.porClave.clear();
        proyectos.porCodigo.clear();
    }

    ConsultarDocentes consultarDocentes() {
        return new ConsultarDocentesServicio(docentes, CargasDePrueba::referencias);
    }

    ConsultarCargos consultarCargos() {
        return new ConsultarCargosServicio(cargos, docentes, RELOJ);
    }

    MantenerCargos mantenerCargos() {
        return new MantenerCargosServicio(
                cargos, docentes, CargasDePrueba::referencias, docentes, docentes, RELOJ);
    }

    ConsultarProyectos consultarProyectos() {
        return new ConsultarProyectosServicio(proyectos, docentes, CargasDePrueba::referencias);
    }

    MantenerProyectos mantenerProyectos() {
        return new MantenerProyectosServicio(
                proyectos, docentes, CargasDePrueba::referencias, docentes, docentes, RELOJ);
    }

    /** Los proyectos guardados, por código. */
    List<Proyecto> proyectos() {
        return proyectos.todos();
    }

    /** Los cargos en memoria. */
    private static final class Cargos implements RepositorioCargos {

        private final Map<String, Cargo> porClave = new LinkedHashMap<>();

        @Override
        public boolean hayVigentes() {
            return porClave.values().stream()
                    .anyMatch(cargo -> cargo.estado() == EstadoDeCargo.VIGENTE);
        }

        @Override
        public List<Cargo> todos() {
            return porClave.values().stream()
                    .sorted(
                            Comparator.comparing(Cargo::docente)
                                    .thenComparing(Cargo::tipo)
                                    .thenComparing(Cargo::fechaDesignacion))
                    .toList();
        }

        @Override
        public Optional<Cargo> buscar(
                String docente, TipoDeCargo tipo, LocalDate fechaDesignacion) {
            return Optional.ofNullable(porClave.get(clave(docente, tipo, fechaDesignacion)));
        }

        @Override
        public boolean hayCoordinadorVigente(String carrera, Cargo salvo) {
            return porClave.values().stream()
                    .filter(Cargo::esCoordinadorVigente)
                    .filter(cargo -> salvo == null || !cargo.mismaClave(salvo))
                    .anyMatch(cargo -> cargo.carrera().equals(carrera));
        }

        @Override
        public void bloquear() {}

        @Override
        public void registrar(Cargo cargo, String quien, Instant momento) {
            porClave.put(clave(cargo.docente(), cargo.tipo(), cargo.fechaDesignacion()), cargo);
        }

        @Override
        public void guardar(Cargo cargo) {
            registrar(cargo, null, null);
        }

        private static String clave(String docente, TipoDeCargo tipo, LocalDate fecha) {
            return docente + "|" + tipo + "|" + fecha;
        }
    }

    /** Los proyectos en memoria. */
    private static final class Proyectos implements RepositorioProyectos {

        private final Map<String, Proyecto> porCodigo = new LinkedHashMap<>();

        @Override
        public boolean hayProyectos() {
            return !porCodigo.isEmpty();
        }

        @Override
        public List<Proyecto> todos() {
            return porCodigo.values().stream()
                    .sorted(Comparator.comparing(Proyecto::codigo))
                    .toList();
        }

        @Override
        public Optional<Proyecto> buscar(String codigo) {
            return Optional.ofNullable(porCodigo.get(codigo));
        }

        @Override
        public void bloquear() {}

        @Override
        public void registrar(Proyecto proyecto, String quien, Instant momento) {
            porCodigo.put(proyecto.codigo(), proyecto);
        }

        @Override
        public void guardar(Proyecto proyecto) {
            porCodigo.put(proyecto.codigo(), proyecto);
        }
    }
}
