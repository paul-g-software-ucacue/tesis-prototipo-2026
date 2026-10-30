package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.aplicacion.ConsultarOfertaServicio;
import ec.mdd.academico.aplicacion.MantenerOfertaServicio;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Doble de los repositorios de la oferta y de los periodos escrito a mano (principio VIII), con los
 * servicios y las reglas reales del dominio sobre las carreras de {@link CarrerasEnMemoria}.
 * Empieza con el periodo {@code 2026-I} en Planificación y el {@code 2025-II} vigente, sin oferta.
 */
final class OfertaEnMemoria implements RepositorioOferta, RepositorioPeriodos {

    private final List<PeriodoAcademico> periodos = new ArrayList<>();
    private final List<AsignaturaParalelo> oferta = new ArrayList<>();

    OfertaEnMemoria() {
        reiniciar();
    }

    /** Vuelve a los dos periodos de inicio, sin oferta, para que cada prueba empiece igual. */
    void reiniciar() {
        periodos.clear();
        oferta.clear();
        periodos.add(
                new PeriodoAcademico(
                        "2026-I",
                        "Periodo 2026-I",
                        LocalDate.of(2026, 4, 6),
                        LocalDate.of(2026, 8, 7),
                        EstadoPeriodo.PLANIFICACION));
        periodos.add(
                new PeriodoAcademico(
                        "2025-II",
                        "Periodo 2025-II",
                        LocalDate.of(2025, 10, 6),
                        LocalDate.of(2026, 2, 6),
                        EstadoPeriodo.VIGENTE));
    }

    /** La consulta real sobre este doble. */
    ConsultarOferta consultar(CarrerasEnMemoria carreras) {
        return new ConsultarOfertaServicio(this, this, carreras, CarrerasEnMemoria.estructura());
    }

    /** El mantenimiento real sobre este doble, sin registro y sin transacción. */
    MantenerOferta mantener(CarrerasEnMemoria carreras) {
        Transaccion directa =
                new Transaccion() {
                    @Override
                    public <T> T ejecutar(Supplier<T> trabajo) {
                        return trabajo.get();
                    }
                };
        return new MantenerOfertaServicio(
                this,
                this,
                carreras,
                CarrerasEnMemoria.estructura(),
                cambio -> {},
                directa,
                Clock.systemUTC());
    }

    @Override
    public List<AsignaturaParalelo> delPeriodo(String periodo) {
        return oferta.stream()
                .filter(una -> una.periodo().equals(periodo))
                .sorted(
                        Comparator.comparing(AsignaturaParalelo::asignatura)
                                .thenComparing(AsignaturaParalelo::paralelo))
                .toList();
    }

    @Override
    public void bloquear() {}

    @Override
    public void registrar(AsignaturaParalelo asignaturaParalelo, String quien, Instant momento) {
        oferta.add(asignaturaParalelo);
    }

    @Override
    public void guardar(AsignaturaParalelo asignaturaParalelo) {
        oferta.replaceAll(
                una ->
                        una.periodo().equals(asignaturaParalelo.periodo())
                                        && una.asignatura().equals(asignaturaParalelo.asignatura())
                                        && una.paralelo().equals(asignaturaParalelo.paralelo())
                                ? asignaturaParalelo
                                : una);
    }

    @Override
    public List<PeriodoAcademico> todos() {
        return List.copyOf(periodos);
    }

    @Override
    public boolean hayPeriodos() {
        return !periodos.isEmpty();
    }

    @Override
    public boolean hayOfertadasEn(String periodo) {
        return oferta.stream()
                .anyMatch(
                        una ->
                                una.periodo().equals(periodo)
                                        && una.estado() == EstadoDeAsignaturaParalelo.OFERTADA);
    }

    @Override
    public boolean hayOfertadas() {
        return oferta.stream().anyMatch(una -> una.estado() == EstadoDeAsignaturaParalelo.OFERTADA);
    }

    @Override
    public Optional<PeriodoAcademico> porCodigo(String codigo) {
        return periodos.stream().filter(periodo -> periodo.codigo().equals(codigo)).findFirst();
    }

    @Override
    public void bloquearTodos() {}

    @Override
    public Optional<PeriodoAcademico> bloquear(String codigo) {
        return porCodigo(codigo);
    }

    @Override
    public boolean registrar(PeriodoAcademico periodo, String quien, Instant momento) {
        return periodos.add(periodo);
    }

    @Override
    public void guardar(PeriodoAcademico periodo) {
        periodos.replaceAll(uno -> uno.codigo().equals(periodo.codigo()) ? periodo : uno);
    }
}
