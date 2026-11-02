package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentesConProyecto;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.DocenteConProyecto;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Quiénes participan en proyectos asignables de un periodo, con sus tipos (RN-13). */
public final class ConsultarDocentesConProyectoServicio implements ConsultarDocentesConProyecto {

    private final ConsultarProyectos proyectos;

    /** Recibe la consulta de proyectos. */
    public ConsultarDocentesConProyectoServicio(ConsultarProyectos proyectos) {
        this.proyectos = proyectos;
    }

    @Override
    public List<DocenteConProyecto> delPeriodo(String periodo) {
        Map<String, Set<TipoDeProyecto>> tipos = new TreeMap<>();
        proyectos.listar(periodo, null, null).stream()
                .filter(ConsultarDocentesConProyectoServicio::asignable)
                .forEach(
                        uno ->
                                uno.proyecto()
                                        .participantes()
                                        .forEach(
                                                participante ->
                                                        tipos.computeIfAbsent(
                                                                        participante.docente(),
                                                                        docente ->
                                                                                EnumSet.noneOf(
                                                                                        TipoDeProyecto
                                                                                                .class))
                                                                .add(uno.proyecto().tipo())));
        return tipos.entrySet().stream()
                .map(entrada -> new DocenteConProyecto(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private static boolean asignable(ProyectoConsultado proyecto) {
        return proyecto.asignabilidad().map(Asignabilidad::asignable).orElse(false);
    }
}
