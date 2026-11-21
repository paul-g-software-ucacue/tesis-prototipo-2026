package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.ContenidoCongelado;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El contenido congelado de cada versión, en {@code generacion.contenido_congelado} y {@code
 * carga_congelada} (F20-H01, D-115). Solo inserta y lee: nunca se modifica.
 */
public interface RepositorioContenidoCongelado {

    /**
     * Guarda el contenido si la versión todavía no lo tiene; si ya lo tiene, no hace nada.
     *
     * @param version el identificador de la versión, de la que cuelga
     * @param contenido el contenido
     * @param momento cuándo se guarda
     * @param tardio si se calculó al pedirlo y no al congelar
     * @return si lo guardó
     */
    boolean guardar(UUID version, ContenidoCongelado contenido, Instant momento, boolean tardio);

    /**
     * El hash guardado de una versión.
     *
     * @param periodo el código del periodo
     * @param numero el número de la versión
     * @return vacío si no tiene contenido guardado
     */
    Optional<String> hash(String periodo, int numero);

    /**
     * Los docentes de una versión con contenido guardado, por nombre y, a igual nombre, por
     * identificación.
     *
     * @param periodo el código del periodo
     * @param numero el número de la versión
     * @return la lista; vacía si no tiene contenido guardado
     */
    List<DocenteDeLaVersion> docentes(String periodo, int numero);

    /**
     * La carga guardada de un docente.
     *
     * @param periodo el código del periodo
     * @param numero el número de la versión
     * @param docente su número de identificación
     * @return vacía si la versión no tiene contenido guardado o el docente no está
     */
    Optional<CargaCongelada> carga(String periodo, int numero, String docente);
}
