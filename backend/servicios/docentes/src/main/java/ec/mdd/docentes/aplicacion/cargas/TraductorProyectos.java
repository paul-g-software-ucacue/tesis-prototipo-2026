package ec.mdd.docentes.aplicacion.cargas;

import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * La carga 09, los proyectos y prácticas aprobados (F01-H04): sus columnas, traducidas a los
 * proyectos de Docentes (FR-012), con sus carreras y sus participantes. Registra y corrige con
 * {@link MantenerProyectos}, así que se aplican sus reglas, como la suma de horas de los
 * participantes.
 */
final class TraductorProyectos extends TraductorDeDocentes<Proyecto> {

    private final RepositorioProyectos repositorio;
    private final MantenerProyectos mantener;

    TraductorProyectos(RepositorioProyectos repositorio, MantenerProyectos mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "proyectos_practicas";
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Proyecto> existentes(LoteDeCarga lote) {
        Map<String, Proyecto> existentes = new LinkedHashMap<>();
        repositorio.todos().forEach(proyecto -> existentes.put(proyecto.codigo(), proyecto));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("codigo_proyecto");
    }

    @Override
    public Map<String, String> campos(Proyecto proyecto) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", proyecto.nombre());
        campos.put("tipo_proyecto", proyecto.tipo().name());
        campos.put("codigos_carrera", String.join("; ", proyecto.carreras()));
        campos.put("resolucion_ocs", proyecto.resolucionOcs());
        campos.put("fecha_resolucion", proyecto.fechaResolucion().toString());
        campos.put("estado", proyecto.estado().name());
        campos.put("vigencia_inicio", proyecto.vigenciaInicio().toString());
        campos.put("vigencia_fin", proyecto.vigenciaFin().toString());
        campos.put("director_identificacion", proyecto.director());
        campos.put(
                "participantes",
                proyecto.participantes().stream()
                        .map(p -> p.docente() + "|" + p.rol().name() + "|" + numero(p.horas()))
                        .collect(Collectors.joining("; ")));
        campos.put("horas_semanales_aprobadas", numero(proyecto.horasAprobadas()));
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nombre", fila.valor("nombre"));
        campos.put("tipo_proyecto", fila.valor("tipo_proyecto"));
        campos.put("codigos_carrera", String.join("; ", elementos(fila.valor("codigos_carrera"))));
        campos.put("resolucion_ocs", fila.valor("resolucion_ocs"));
        campos.put("fecha_resolucion", fila.valor("fecha_resolucion"));
        campos.put("estado", fila.valor("estado"));
        campos.put("vigencia_inicio", fila.valor("vigencia_inicio"));
        campos.put("vigencia_fin", fila.valor("vigencia_fin"));
        campos.put("director_identificacion", fila.valor("director_identificacion"));
        campos.put(
                "participantes",
                participantes(fila).stream()
                        .map(p -> p.docente() + "|" + p.rol().name() + "|" + numero(p.horas()))
                        .collect(Collectors.joining("; ")));
        campos.put("horas_semanales_aprobadas", numero(fila.valor("horas_semanales_aprobadas")));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        mantener.registrar(proyecto(fila), quien);
    }

    @Override
    public void corregir(FilaDelLote fila, Proyecto actual, String quien) {
        mantener.corregir(proyecto(fila), quien);
    }

    private static Proyecto proyecto(FilaDelLote fila) {
        return new Proyecto(
                fila.valor("codigo_proyecto"),
                fila.valor("nombre"),
                TipoDeProyecto.valueOf(fila.valor("tipo_proyecto")),
                elementos(fila.valor("codigos_carrera")),
                fila.valor("resolucion_ocs"),
                LocalDate.parse(fila.valor("fecha_resolucion")),
                EstadoDeProyecto.valueOf(fila.valor("estado")),
                LocalDate.parse(fila.valor("vigencia_inicio")),
                LocalDate.parse(fila.valor("vigencia_fin")),
                fila.valor("director_identificacion"),
                participantes(fila),
                new BigDecimal(fila.valor("horas_semanales_aprobadas")));
    }

    private static List<Participante> participantes(FilaDelLote fila) {
        return elementos(fila.valor("participantes")).stream()
                .map(TraductorDeDocentes::partes)
                .map(
                        p ->
                                new Participante(
                                        p[0],
                                        RolDeParticipante.valueOf(p[1]),
                                        new BigDecimal(p[2])))
                .toList();
    }
}
