package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.MetaDeProporcion;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.ReglasDelPeriodo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lo que otros servicios saben y hace falta para asignar actividades (RF-26, ADR-023). */
public interface DatosParaActividades {

    /**
     * El catálogo de la versión que rige el periodo.
     *
     * @return vacío si el periodo no tiene versión
     */
    Optional<List<ActividadDelCatalogo>> catalogo(String periodo);

    /**
     * Los proyectos asignables del periodo en los que participa el docente y sus cargos vigentes.
     *
     * @return vacío si el docente o el periodo no existen
     */
    Optional<ProyectosYCargosDelDocente> proyectosYCargos(String docente, String periodo);

    /**
     * La meta de la proporción en la versión que rige el periodo (RN-05).
     *
     * @return vacío si el periodo no tiene versión
     */
    Optional<MetaDeProporcion> meta(String periodo);

    /**
     * Las cifras de las reglas duras en la versión que rige el periodo.
     *
     * @return vacío si el periodo no tiene versión
     */
    Optional<ReglasDelPeriodo> reglas(String periodo);

    /**
     * Quiénes participan en proyectos asignables del periodo, con los tipos de esos proyectos.
     *
     * @return por identificación; quien no está no tiene proyectos
     */
    Map<String, List<String>> docentesConProyecto(String periodo);
}
