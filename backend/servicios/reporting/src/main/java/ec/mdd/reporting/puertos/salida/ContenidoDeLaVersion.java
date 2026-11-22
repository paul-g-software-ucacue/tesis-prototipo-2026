package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.CargasDeLaVersion;
import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import java.util.Optional;

/**
 * El contenido congelado de una versión, de Generación (D-115): el que se guardó al congelarla.
 * Solo lee.
 */
public interface ContenidoDeLaVersion {

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return el hash y los docentes; vacío si la versión no existe o no está congelada
     */
    Optional<DocentesDeLaVersion> docentes(String periodo, int version);

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @param identificacion el número de identificación del docente
     * @return su contenido; vacío si la versión no está o el docente no está en ella
     */
    Optional<ContenidoDelDocente> contenido(String periodo, int version, String identificacion);

    /**
     * El contenido de todos los docentes de una versión, en una sola llamada (F19-H01, D-119).
     *
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return el hash y el contenido de cada docente; vacío si la versión no existe o no está
     *     congelada
     * @throws ec.mdd.plataforma.eventos.ProcesoConError si la respuesta no cumple su esquema
     */
    Optional<CargasDeLaVersion> cargas(String periodo, int version);
}
