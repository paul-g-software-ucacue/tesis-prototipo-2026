package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El estado del documento firmable de un periodo, para quien lo consulta (esquema {@code
 * EstadoDelDocumento}, F13-H02, FR-014).
 *
 * @param periodo el código del periodo
 * @param version la versión congelada; vacía si Aprobación todavía no pidió el documento
 * @param estado SIN_GENERAR, EN_CURSO, FALLIDA o GENERADO
 * @param motivo por qué falló; solo con FALLIDA
 * @param documento el documento; solo con GENERADO
 * @param acciones lo que puede hacer quien consulta: GENERAR, o nada
 */
public record EstadoDelDocumento(
        String periodo,
        Optional<Integer> version,
        Estado estado,
        Optional<String> motivo,
        Optional<Documento> documento,
        List<String> acciones) {

    /** La acción de reintentar. */
    public static final String GENERAR = "GENERAR";

    /** Exige los datos y copia las acciones. */
    public EstadoDelDocumento {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(estado, "estado");
        acciones = List.copyOf(acciones);
    }

    /** Los estados que ve la pantalla. */
    public enum Estado {
        /** Aprobación todavía no lo pidió, o lo pidió y todavía no empezó. */
        SIN_GENERAR,
        /** Se está generando. */
        EN_CURSO,
        /** Falló; el rol de la etapa puede reintentar. */
        FALLIDA,
        /** Está registrado. */
        GENERADO
    }

    /**
     * El documento registrado.
     *
     * @param sha256 el SHA-256 del archivo, la huella del PDF
     * @param hashContenido el hash de contenido de la versión
     * @param generadoPor {@code sistema} o quien pidió el reintento
     * @param generadoEn cuándo
     */
    public record Documento(
            String sha256, String hashContenido, String generadoPor, Instant generadoEn) {}
}
