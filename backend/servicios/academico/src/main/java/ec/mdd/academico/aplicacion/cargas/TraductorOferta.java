package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.puertos.entrada.DatosDeAsignaturaParalelo;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La carga 05, la oferta del periodo (F01-H04): sus columnas, traducidas a las asignaturas-paralelo
 * de Académico (FR-012). Las bajas se buscan solo en los periodos del archivo. Las franjas horarias
 * se validan (F01-H03), pero no se guardan todavía (RF-60), así que no cuentan como cambio
 * (research §5). Registra y corrige con {@link MantenerOferta}: la oferta solo cambia mientras su
 * periodo está en Planificación.
 */
final class TraductorOferta extends TraductorAcademico<AsignaturaParalelo> {

    private final RepositorioOferta repositorio;
    private final MantenerOferta mantener;

    TraductorOferta(RepositorioOferta repositorio, MantenerOferta mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "oferta_periodo";
    }

    @Override
    public boolean tieneDatos() {
        return repositorio.hayOfertadas();
    }

    @Override
    public boolean tieneDatosEn(String periodo) {
        return repositorio.hayOfertadasEn(periodo);
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, AsignaturaParalelo> existentes(LoteDeCarga lote) {
        Map<String, AsignaturaParalelo> existentes = new LinkedHashMap<>();
        lote.filas().stream()
                .map(fila -> fila.valor("codigo_periodo"))
                .distinct()
                .forEach(
                        periodo ->
                                repositorio
                                        .delPeriodo(periodo)
                                        .forEach(
                                                paralelo ->
                                                        existentes.put(
                                                                clave(
                                                                        paralelo.periodo(),
                                                                        paralelo.asignatura(),
                                                                        paralelo.paralelo()),
                                                                paralelo)));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return clave(
                fila.valor("codigo_periodo"),
                fila.valor("codigo_asignatura"),
                fila.valor("paralelo"));
    }

    @Override
    public Map<String, String> campos(AsignaturaParalelo paralelo) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("jornada", paralelo.jornada().name());
        campos.put("modalidad", paralelo.modalidad().name());
        campos.put("codigo_unidad_sede", texto(paralelo.sede()));
        campos.put("numero_estudiantes", String.valueOf(paralelo.estudiantes()));
        campos.put("horas_semanales", numero(paralelo.horasIndicadas()));
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("jornada", fila.valor("jornada"));
        campos.put("modalidad", fila.valor("modalidad"));
        campos.put("codigo_unidad_sede", fila.valor("codigo_unidad_sede"));
        campos.put(
                "numero_estudiantes",
                String.valueOf(Integer.parseInt(fila.valor("numero_estudiantes"))));
        campos.put("horas_semanales", numero(fila.valor("horas_semanales")));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        DatosDeAsignaturaParalelo datos = datos(fila);
        mantener.ofertar(
                AsignaturaParalelo.nueva(
                        fila.valor("codigo_periodo"),
                        fila.valor("codigo_asignatura"),
                        fila.valor("paralelo"),
                        datos.jornada(),
                        datos.modalidad(),
                        datos.sede(),
                        datos.estudiantes(),
                        datos.horasIndicadas()),
                quien);
    }

    @Override
    public void corregir(FilaDelLote fila, AsignaturaParalelo actual, String quien) {
        mantener.corregir(
                actual.periodo(), actual.asignatura(), actual.paralelo(), datos(fila), quien);
    }

    private static DatosDeAsignaturaParalelo datos(FilaDelLote fila) {
        return new DatosDeAsignaturaParalelo(
                Jornada.valueOf(fila.valor("jornada")),
                Modalidad.valueOf(fila.valor("modalidad")),
                nulo(fila.valor("codigo_unidad_sede")),
                Integer.parseInt(fila.valor("numero_estudiantes")),
                decimal(fila.valor("horas_semanales")));
    }

    private static String clave(String periodo, String asignatura, String paralelo) {
        return periodo + " · " + asignatura + " · " + paralelo;
    }
}
