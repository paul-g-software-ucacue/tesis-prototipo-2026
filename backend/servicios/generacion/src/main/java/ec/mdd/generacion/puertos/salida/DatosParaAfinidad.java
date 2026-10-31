package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import java.util.List;
import java.util.Optional;

/**
 * Los datos de otros servicios que hacen falta para calcular la afinidad, en los tipos de este
 * contexto (principio V, ADR-023). Cada método falla si el dueño no responde; nunca devuelve datos
 * a medias.
 */
public interface DatosParaAfinidad {

    /**
     * Las asignaturas ofertadas del periodo.
     *
     * @param carrera solo las de esta carrera; vacío, todas
     * @return vacío si el periodo no existe; una lista vacía si existe sin oferta
     */
    Optional<List<AsignaturaOfertada>> asignaturasOfertadas(
            String periodo, Optional<String> carrera);

    /**
     * Los pesos de la versión que rige el periodo.
     *
     * @return vacío si el periodo no tiene versión
     */
    Optional<PesosDelPeriodo> pesos(String periodo);

    /**
     * La formación del docente.
     *
     * @return vacío si el docente no existe
     */
    Optional<FormacionDelDocente> formacion(String identificacion);
}
