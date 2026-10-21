package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.CambiarEstadoPeriodo;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Doble de los casos de uso de Académico escrito a mano (principio VIII): los periodos en memoria,
 * cambiados con las operaciones reales del dominio. Empieza con un periodo 2026-2 en Planificación
 * sin versión y un 2026-1 vigente con la versión 1. Las versiones de normativa que existen son la 1
 * y la 2.
 */
final class AcademicoEnMemoria
        implements ConsultarPeriodos,
                RegistrarPeriodo,
                CorregirPeriodo,
                AbrirPeriodo,
                CambiarEstadoPeriodo {

    private static final Set<Integer> VERSIONES = Set.of(1, 2);

    private final Map<String, PeriodoAcademico> periodos = new LinkedHashMap<>();
    private final Map<String, Integer> versiones = new LinkedHashMap<>();
    private String ultimoUsuario;

    AcademicoEnMemoria() {
        reiniciar();
    }

    /** Vuelve a los dos periodos de inicio, para que cada prueba empiece igual. */
    void reiniciar() {
        periodos.clear();
        versiones.clear();
        periodos.put(
                "2026-2",
                PeriodoAcademico.nuevo(
                        "2026-2",
                        "Periodo 2026-2",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2027, 2, 28)));
        periodos.put(
                "2026-1",
                new PeriodoAcademico(
                        "2026-1",
                        "Periodo 2026-1",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 8, 15),
                        EstadoPeriodo.VIGENTE));
        versiones.put("2026-1", 1);
        ultimoUsuario = null;
    }

    /** Deja un periodo en el estado pedido, para preparar una prueba. */
    void conEstado(String codigo, EstadoPeriodo estado, Integer version) {
        PeriodoAcademico periodo = periodos.get(codigo);
        periodos.put(
                codigo,
                new PeriodoAcademico(
                        codigo, periodo.nombre(), periodo.inicio(), periodo.fin(), estado));
        if (version != null) {
            versiones.put(codigo, version);
        }
    }

    /** El usuario que recibió la última operación de escritura. */
    String ultimoUsuario() {
        return ultimoUsuario;
    }

    @Override
    public List<PeriodoConsultado> todos() {
        return periodos.values().stream().map(this::consultado).toList();
    }

    @Override
    public PeriodoConsultado porCodigo(String codigo) {
        return consultado(existente(codigo));
    }

    @Override
    public PeriodoConsultado registrar(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien) {
        PeriodoAcademico nuevo = PeriodoAcademico.nuevo(codigo, nombre, inicio, fin);
        if (periodos.containsKey(codigo)) {
            throw new CodigoRepetido(codigo);
        }
        periodos.put(codigo, nuevo);
        ultimoUsuario = quien;
        return consultado(nuevo);
    }

    @Override
    public PeriodoConsultado corregir(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien) {
        PeriodoAcademico corregido = existente(codigo).corregido(nombre, inicio, fin).periodo();
        periodos.put(codigo, corregido);
        ultimoUsuario = quien;
        return consultado(corregido);
    }

    @Override
    public PeriodoConsultado abrir(String codigo, int version, String quien) {
        PeriodoAcademico periodo = existente(codigo);
        if (periodo.estado() == EstadoPeriodo.CERRADO) {
            throw new PeriodoCerrado(codigo);
        }
        Integer actual = versiones.get(codigo);
        if (actual != null && actual != version) {
            throw new PeriodoConOtraVersion(codigo, actual);
        }
        if (!VERSIONES.contains(version)) {
            throw new VersionInexistente(version);
        }
        versiones.put(codigo, version);
        ultimoUsuario = quien;
        return consultado(periodo);
    }

    @Override
    public PeriodoConsultado cambiar(String codigo, EstadoPeriodo destino, String quien) {
        PeriodoAcademico periodo = existente(codigo);
        PeriodoAcademico cambiado = periodo.conEstado(destino, versiones.containsKey(codigo));
        periodos.put(codigo, cambiado);
        ultimoUsuario = quien;
        return consultado(cambiado);
    }

    private PeriodoAcademico existente(String codigo) {
        PeriodoAcademico periodo = periodos.get(codigo);
        if (periodo == null) {
            throw new PeriodoNoEncontrado(codigo);
        }
        return periodo;
    }

    private PeriodoConsultado consultado(PeriodoAcademico periodo) {
        Integer version = versiones.get(periodo.codigo());
        return new PeriodoConsultado(
                periodo, version == null ? OptionalInt.empty() : OptionalInt.of(version));
    }
}
