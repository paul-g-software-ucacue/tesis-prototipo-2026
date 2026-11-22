package ec.mdd.reporting.puertos.salida;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Las versiones cargadas del layout de exportación, en {@code reporting.layout_exportacion}, y el
 * texto del layout por defecto (F19-H01, D-119). Solo agrega y lee: una versión nunca se modifica
 * ni se borra.
 */
public interface LayoutsGuardados {

    /**
     * @return la versión de número más alto; vacía si nunca se cargó ninguna
     */
    Optional<LayoutGuardado> vigente();

    /**
     * @return todas las versiones cargadas, de la más reciente a la más antigua
     */
    List<LayoutGuardado> todos();

    /**
     * Guarda una versión nueva con el número siguiente. Si otra carga tomó ese número al mismo
     * tiempo, reintenta una vez.
     *
     * @param texto el archivo, sin la marca de orden de bytes
     * @param sha256 su SHA-256
     * @param quien quién lo carga
     * @param momento cuándo
     * @return el número de la versión guardada
     */
    int guardar(String texto, String sha256, String quien, Instant momento);

    /**
     * @return el texto de la plantilla, que es el layout por defecto (versión 0)
     */
    String porDefecto();

    /**
     * Una versión guardada, tal cual.
     *
     * @param numero la versión del layout de exportación, desde 1
     * @param texto el archivo
     * @param sha256 su SHA-256
     * @param cargadoPor quién lo cargó
     * @param cargadoEn cuándo
     */
    record LayoutGuardado(
            int numero, String texto, String sha256, String cargadoPor, Instant cargadoEn) {

        /** Exige los datos. */
        public LayoutGuardado {
            Objects.requireNonNull(texto, "texto");
            Objects.requireNonNull(sha256, "sha256");
            Objects.requireNonNull(cargadoPor, "cargadoPor");
            Objects.requireNonNull(cargadoEn, "cargadoEn");
        }
    }
}
