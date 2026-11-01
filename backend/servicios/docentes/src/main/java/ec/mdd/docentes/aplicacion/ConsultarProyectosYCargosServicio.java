package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectosYCargos;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectosYCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;

/** Los proyectos asignables en los que participa un docente y sus cargos vigentes (RN-25). */
public final class ConsultarProyectosYCargosServicio implements ConsultarProyectosYCargos {

    private final RepositorioDocentes docentes;
    private final ConsultarProyectos proyectos;
    private final ConsultarCargos cargos;

    /** Recibe el repositorio de los docentes y las consultas de proyectos y cargos. */
    public ConsultarProyectosYCargosServicio(
            RepositorioDocentes docentes, ConsultarProyectos proyectos, ConsultarCargos cargos) {
        this.docentes = docentes;
        this.proyectos = proyectos;
        this.cargos = cargos;
    }

    @Override
    public ProyectosYCargos delDocente(String identificacion, String periodo) {
        docentes.buscar(identificacion).orElseThrow(DocenteNoEncontrado::new);
        return new ProyectosYCargos(
                proyectos.listar(periodo, null, null).stream()
                        .filter(uno -> asignable(uno) && participa(uno, identificacion))
                        .map(ProyectoConsultado::proyecto)
                        .toList(),
                cargos.listar(EstadoDeCargo.VIGENTE, identificacion).stream()
                        .map(uno -> uno.cargo().tipo())
                        .toList());
    }

    private static boolean asignable(ProyectoConsultado proyecto) {
        return proyecto.asignabilidad().map(Asignabilidad::asignable).orElse(false);
    }

    private static boolean participa(ProyectoConsultado proyecto, String identificacion) {
        return proyecto.proyecto().participantes().stream()
                .anyMatch(uno -> uno.docente().equals(identificacion));
    }
}
