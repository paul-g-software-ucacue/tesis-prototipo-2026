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
 * @param anulacion la anulación de sus firmas, si la hubo (F12-H04)
 * @param cadena los pasos de la cadena con su estado, solo con GENERADO y sin anulación (F16-H02)
 */
public record EstadoDelDocumento(
        String periodo,
        Optional<Integer> version,
        Estado estado,
        Optional<String> motivo,
        Optional<Documento> documento,
        List<String> acciones,
        Optional<Anulacion> anulacion,
        List<Paso> cadena) {

    /** La acción de reintentar. */
    public static final String GENERAR = "GENERAR";

    /** Exige los datos y copia las acciones. */
    public EstadoDelDocumento {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(anulacion, "anulacion");
        acciones = List.copyOf(acciones);
        cadena = List.copyOf(cadena);
    }

    /** Un estado sin cadena: sin generar, en generación, fallido o con las firmas anuladas. */
    public EstadoDelDocumento(
            String periodo,
            Optional<Integer> version,
            Estado estado,
            Optional<String> motivo,
            Optional<Documento> documento,
            List<String> acciones,
            Optional<Anulacion> anulacion) {
        this(periodo, version, estado, motivo, documento, acciones, anulacion, List.of());
    }

    /** Un estado sin anulación de las firmas. */
    public EstadoDelDocumento(
            String periodo,
            Optional<Integer> version,
            Estado estado,
            Optional<String> motivo,
            Optional<Documento> documento,
            List<String> acciones) {
        this(periodo, version, estado, motivo, documento, acciones, Optional.empty());
    }

    /**
     * Las firmas del documento quedaron anuladas por orden de Aprobación (F12-H04, D-106).
     *
     * @param motivo por qué
     * @param firmasAnuladas cuántas firmas aceptadas tenía
     * @param anuladaEn cuándo
     */
    public record Anulacion(String motivo, int firmasAnuladas, Instant anuladaEn) {}

    /**
     * Un paso de la cadena de firmas como lo ve quien sigue el proceso (esquema {@code
     * PasoConEstado}, F16-H02, D-110): sin la identificación, el usuario ni el certificado.
     *
     * @param orden su lugar, desde 1
     * @param rotulo el rótulo, como en el PDF
     * @param firmante el nombre impreso, si se guardó al generar
     * @param estado FIRMADO, EN_TURNO, PENDIENTE o RECHAZADO
     * @param firmadoEn cuándo el MDD aceptó la firma; solo con FIRMADO
     */
    public record Paso(
            int orden,
            String rotulo,
            Optional<String> firmante,
            String estado,
            Optional<Instant> firmadoEn) {}

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
