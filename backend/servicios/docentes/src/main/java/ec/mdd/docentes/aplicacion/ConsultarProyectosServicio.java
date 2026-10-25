package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectoNoEncontrado;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consultar los proyectos y si son asignables en un periodo (F03-H05, RF-18, research §7). Las
 * fechas del periodo vienen de Académico; la regla es del dominio ({@link Proyecto#asignableEn}).
 */
public final class ConsultarProyectosServicio implements ConsultarProyectos {

    private final RepositorioProyectos repositorio;
    private final RepositorioDocentes docentes;
    private final ReferenciasAcademicas referencias;

    /** Recibe los repositorios y las referencias de Académico. */
    public ConsultarProyectosServicio(
            RepositorioProyectos repositorio,
            RepositorioDocentes docentes,
            ReferenciasAcademicas referencias) {
        this.repositorio = repositorio;
        this.docentes = docentes;
        this.referencias = referencias;
    }

    @Override
    public List<ProyectoConsultado> listar(
            String periodo, TipoDeProyecto tipo, EstadoDeProyecto estado) {
        Optional<Referencias.Periodo> elPeriodo = periodo(periodo);
        Map<String, Docente> porIdentificacion =
                docentes.todos().stream()
                        .collect(
                                Collectors.toMap(
                                        uno -> uno.identificacion().numero(), Function.identity()));
        return repositorio.todos().stream()
                .filter(proyecto -> tipo == null || proyecto.tipo() == tipo)
                .filter(proyecto -> estado == null || proyecto.estado() == estado)
                .map(
                        proyecto ->
                                consultado(
                                        proyecto,
                                        elPeriodo,
                                        uno -> Optional.ofNullable(porIdentificacion.get(uno))))
                .toList();
    }

    @Override
    public ProyectoConsultado proyecto(String codigo, String periodo) {
        Optional<Referencias.Periodo> elPeriodo = periodo(periodo);
        Proyecto proyecto = repositorio.buscar(codigo).orElseThrow(ProyectoNoEncontrado::new);
        return consultado(proyecto, elPeriodo, docentes::buscar);
    }

    private Optional<Referencias.Periodo> periodo(String codigo) {
        return codigo == null
                ? Optional.empty()
                : Optional.of(referencias.referencias().periodo(codigo));
    }

    private static ProyectoConsultado consultado(
            Proyecto proyecto,
            Optional<Referencias.Periodo> periodo,
            Function<String, Optional<Docente>> buscar) {
        Optional<Asignabilidad> asignabilidad =
                periodo.map(uno -> proyecto.asignableEn(uno.inicio(), uno.fin()));
        return new ProyectoConsultado(
                proyecto, NombresDeDocentes.de(proyecto, buscar), asignabilidad);
    }
}
