package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.CargaGuardada;
import java.time.Instant;
import java.util.Optional;

/** Las cargas de cada periodo. Solo se insertan, y la vigente es la última (principio VII). */
public interface RepositorioConfiguracion {

    /**
     * @param periodo el código del periodo
     * @param contenido el archivo tal como se subió
     * @param por quién lo cargó
     * @param en cuándo
     */
    void guardar(String periodo, String contenido, String por, Instant en);

    /**
     * @param periodo el código del periodo
     * @return la última carga, o vacío si no hay ninguna
     */
    Optional<CargaGuardada> vigente(String periodo);

    /**
     * La carga que regía en un instante: la última con {@code cargada_en} hasta ese instante
     * (F12-H02, research §5). La tabla es solo de inserción, así que se puede reconstruir.
     *
     * @param periodo el código del periodo
     * @param instante el instante, por ejemplo la creación del distributivo
     * @return esa carga, o vacío si todavía regía la configuración por defecto
     */
    Optional<CargaGuardada> vigenteEn(String periodo, Instant instante);
}
