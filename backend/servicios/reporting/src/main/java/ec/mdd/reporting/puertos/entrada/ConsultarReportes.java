package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.ReporteIndividual;
import java.util.List;
import java.util.Objects;

/**
 * Los reportes de la versión publicada vigente (RF-45, F20-H01): las publicaciones, los docentes de
 * una y el reporte individual de uno. Solo lee; ver un reporte no deja entrada en la bitácora.
 */
public interface ConsultarReportes {

    /**
     * @return los periodos con versión publicada vigente, de la publicación más reciente a la más
     *     antigua
     */
    List<Publicacion> publicaciones();

    /**
     * @param periodo el código del periodo
     * @return la publicación vigente y sus docentes, por nombre
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     */
    DocentesDelPeriodo docentes(String periodo);

    /**
     * @param periodo el código del periodo
     * @param identificacion el número de identificación del docente
     * @param quien quién lo pide
     * @return el reporte individual
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.DocenteSinDistributivo si el docente no está en ella
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado
     */
    ReporteIndividual reporte(String periodo, String identificacion, String quien);

    /**
     * @param periodo el código del periodo
     * @param quien quién lo pide
     * @return el reporte general consolidado de la versión publicada vigente (F20-H02, RF-46)
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado o
     *     un docente viene repetido
     */
    ReporteGeneral reporteGeneral(String periodo, String quien);

    /**
     * La publicación vigente de un periodo y sus docentes.
     *
     * @param publicacion la publicación vigente
     * @param docentes los de esa versión, por nombre
     */
    record DocentesDelPeriodo(Publicacion publicacion, List<DocentesDeLaVersion.Docente> docentes) {

        /** Exige la publicación y copia la lista. */
        public DocentesDelPeriodo {
            Objects.requireNonNull(publicacion, "publicacion");
            docentes = List.copyOf(docentes);
        }
    }
}
