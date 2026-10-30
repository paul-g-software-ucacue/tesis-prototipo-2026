package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La carga 03, los ciclos académicos (F01-H04): sus columnas, traducidas a los periodos de
 * Académico (FR-012). Registra y corrige con sus casos de uso, que avisan a Roles de cada periodo
 * nuevo. Las semanas, la fecha límite de envío, la resolución y el estado del archivo se validan
 * (F01-H03), pero Académico no los guarda: el estado de un periodo en el MDD es otro
 * (Planificación…Cerrado), así que no cuentan como cambio (research §5).
 */
final class TraductorPeriodos extends TraductorAcademico<PeriodoAcademico> {

    private final RepositorioPeriodos repositorio;
    private final RegistrarPeriodo registrar;
    private final CorregirPeriodo corregir;

    TraductorPeriodos(
            RepositorioPeriodos repositorio, RegistrarPeriodo registrar, CorregirPeriodo corregir) {
        this.repositorio = repositorio;
        this.registrar = registrar;
        this.corregir = corregir;
    }

    @Override
    public String carga() {
        return "ciclos_academicos";
    }

    @Override
    public boolean tieneDatos() {
        return repositorio.hayPeriodos();
    }

    @Override
    public void bloquear() {
        repositorio.bloquearTodos();
    }

    @Override
    public Map<String, PeriodoAcademico> existentes(LoteDeCarga lote) {
        Map<String, PeriodoAcademico> existentes = new LinkedHashMap<>();
        repositorio.todos().forEach(periodo -> existentes.put(periodo.codigo(), periodo));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_periodo");
    }

    @Override
    public Map<String, String> campos(PeriodoAcademico periodo) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", periodo.nombre());
        campos.put("fecha_inicio", periodo.inicio().toString());
        campos.put("fecha_fin", periodo.fin().toString());
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", fila.valor("nombre"));
        campos.put("fecha_inicio", fila.valor("fecha_inicio"));
        campos.put("fecha_fin", fila.valor("fecha_fin"));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        registrar.registrar(
                clave(fila),
                fila.valor("nombre"),
                LocalDate.parse(fila.valor("fecha_inicio")),
                LocalDate.parse(fila.valor("fecha_fin")),
                quien);
    }

    @Override
    public void corregir(FilaDelLote fila, PeriodoAcademico actual, String quien) {
        corregir.corregir(
                clave(fila),
                fila.valor("nombre"),
                LocalDate.parse(fila.valor("fecha_inicio")),
                LocalDate.parse(fila.valor("fecha_fin")),
                quien);
    }
}
