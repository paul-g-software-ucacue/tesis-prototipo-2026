package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.puertos.entrada.MantenerMatriculas;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La carga 10, los matriculados por carrera (F01-H04): sus columnas, traducidas a los matriculados
 * de Académico (FR-012). El diccionario no le da clave; sus registros se comparan por periodo y
 * carrera, sus dos referencias (supuesto de la especificación), y las bajas se buscan solo en los
 * periodos del archivo.
 */
final class TraductorMatriculas extends TraductorAcademico<Matricula> {

    private final RepositorioMatriculas repositorio;
    private final MantenerMatriculas mantener;

    TraductorMatriculas(RepositorioMatriculas repositorio, MantenerMatriculas mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "matriculados_carrera";
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Matricula> existentes(LoteDeCarga lote) {
        Map<String, Matricula> existentes = new LinkedHashMap<>();
        repositorio
                .deLosPeriodos(
                        lote.filas().stream()
                                .map(fila -> fila.valor("codigo_periodo"))
                                .distinct()
                                .toList())
                .forEach(
                        matricula ->
                                existentes.put(
                                        matricula.periodo() + " · " + matricula.carrera(),
                                        matricula));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_periodo") + " · " + fila.valor("codigo_carrera");
    }

    @Override
    public Map<String, String> campos(Matricula matricula) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("total_matriculados", String.valueOf(matricula.total()));
        campos.put("fecha_corte", matricula.fechaCorte().toString());
        campos.put("fuente", texto(matricula.fuente()));
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put(
                "total_matriculados",
                String.valueOf(Integer.parseInt(fila.valor("total_matriculados"))));
        campos.put("fecha_corte", fila.valor("fecha_corte"));
        campos.put("fuente", fila.valor("fuente"));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        mantener.registrar(matricula(fila), quien);
    }

    @Override
    public void corregir(FilaDelLote fila, Matricula actual, String quien) {
        mantener.corregir(matricula(fila), quien);
    }

    private static Matricula matricula(FilaDelLote fila) {
        return new Matricula(
                fila.valor("codigo_periodo"),
                fila.valor("codigo_carrera"),
                Integer.parseInt(fila.valor("total_matriculados")),
                LocalDate.parse(fila.valor("fecha_corte")),
                nulo(fila.valor("fuente")));
    }
}
