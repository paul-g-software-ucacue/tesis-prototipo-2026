package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.DocenteActivo;
import java.util.List;
import java.util.Optional;

/**
 * Los datos de otros servicios que hacen falta para sugerir docentes, en los tipos de este contexto
 * (principio V, ADR-023). Los pesos y el mínimo salen de {@link DatosParaAfinidad}. Cada método
 * falla si el dueño no responde; nunca devuelve datos a medias.
 */
public interface DatosParaSugerencia {

    /**
     * Una asignatura-paralelo de la oferta, también si está retirada.
     *
     * @return vacío si no está en la oferta del periodo
     */
    Optional<AsignaturaParalelo> paralelo(String periodo, String asignatura, String paralelo);

    /** Los docentes activos, de todas las carreras base. */
    List<DocenteActivo> docentesActivos();
}
