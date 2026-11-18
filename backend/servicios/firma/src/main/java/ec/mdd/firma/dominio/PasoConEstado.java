package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un paso de la cadena de firmas como lo ve quien sigue el proceso (F16-H02, D-110): sin la
 * identificación, el usuario ni el certificado del firmante (FR-016).
 *
 * @param orden su lugar, desde 1
 * @param rotulo el rótulo, como en el PDF («Elaborado por»)
 * @param firmante el nombre impreso, si se guardó al generar
 * @param estado firmado, en turno, pendiente o rechazado
 * @param firmadoEn cuándo el MDD aceptó la firma; solo con {@link Estado#FIRMADO}
 */
public record PasoConEstado(
        int orden,
        String rotulo,
        Optional<String> firmante,
        Estado estado,
        Optional<Instant> firmadoEn) {

    /** Exige los datos, y la fecha de la firma solo en un paso firmado. */
    public PasoConEstado {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden empieza en 1");
        }
        Objects.requireNonNull(rotulo, "rotulo");
        Objects.requireNonNull(firmante, "firmante");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(firmadoEn, "firmadoEn");
        if (firmadoEn.isPresent() != (estado == Estado.FIRMADO)) {
            throw new IllegalArgumentException("Solo un paso firmado lleva la fecha de su firma");
        }
    }

    /** El estado de un paso. */
    public enum Estado {
        /** Tiene una firma aceptada. */
        FIRMADO,
        /** Es el primer paso sin firma, y alguien puede firmarlo. */
        EN_TURNO,
        /** Le falta la firma, y no es su turno. */
        PENDIENTE,
        /** Su firmante rechazó el documento (D-105). */
        RECHAZADO
    }
}
