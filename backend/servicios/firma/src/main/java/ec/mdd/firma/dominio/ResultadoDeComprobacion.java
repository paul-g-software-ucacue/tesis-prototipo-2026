package ec.mdd.firma.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El resultado de una comprobación, con su motivo si no se aprobó (D-103).
 *
 * <p>El motivo es un texto del sistema, fijo para cada caso: nunca el mensaje de una excepción de
 * las bibliotecas, que va al registro del proceso (RNF-SEG-04). Llega al usuario en el informe y en
 * los detalles del 422.
 *
 * @param comprobacion cuál
 * @param resultado aprobada, rechazada o no evaluada
 * @param motivo por qué se rechazó o no se evaluó, de 1 a 200 caracteres; vacío si se aprobó
 */
public record ResultadoDeComprobacion(
        Comprobacion comprobacion, Resultado resultado, Optional<String> motivo) {

    /** El largo máximo del motivo, el del contrato. */
    public static final int MOTIVO_MAXIMO = 200;

    /** Exige los datos, un motivo solo si no se aprobó, y su largo. */
    public ResultadoDeComprobacion {
        Objects.requireNonNull(comprobacion, "comprobacion");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(motivo, "motivo");
        if ((resultado == Resultado.APROBADA) == motivo.isPresent()) {
            throw new IllegalArgumentException(
                    "una comprobación aprobada no lleva motivo, y las demás sí");
        }
        motivo.ifPresent(
                texto -> {
                    if (texto.isBlank() || texto.length() > MOTIVO_MAXIMO) {
                        throw new IllegalArgumentException(
                                "el motivo tiene de 1 a " + MOTIVO_MAXIMO + " caracteres");
                    }
                });
    }

    /** La comprobación pasó. */
    public static ResultadoDeComprobacion aprobada(Comprobacion comprobacion) {
        return new ResultadoDeComprobacion(comprobacion, Resultado.APROBADA, Optional.empty());
    }

    /** La comprobación falló, con su motivo. */
    public static ResultadoDeComprobacion rechazada(Comprobacion comprobacion, String motivo) {
        return new ResultadoDeComprobacion(comprobacion, Resultado.RECHAZADA, Optional.of(motivo));
    }

    /** La comprobación no se hizo, con su motivo. */
    public static ResultadoDeComprobacion noEvaluada(Comprobacion comprobacion, String motivo) {
        return new ResultadoDeComprobacion(
                comprobacion, Resultado.NO_EVALUADA, Optional.of(motivo));
    }

    /**
     * Las cuatro comprobaciones a partir de las que se evaluaron, en orden: las que faltan quedan
     * no evaluadas, porque la última evaluada no se aprobó.
     *
     * @param evaluadas las primeras comprobaciones, en orden; si son menos de cuatro, la última no
     *     está aprobada
     * @return las cuatro, en el orden de {@link Comprobacion}
     */
    public static List<ResultadoDeComprobacion> desde(ResultadoDeComprobacion... evaluadas) {
        List<ResultadoDeComprobacion> todas = new ArrayList<>(List.of(evaluadas));
        Comprobacion[] orden = Comprobacion.values();
        if (todas.isEmpty() || todas.size() > orden.length) {
            throw new IllegalArgumentException("de una a cuatro comprobaciones evaluadas");
        }
        ResultadoDeComprobacion ultima = todas.getLast();
        if (todas.size() < orden.length && ultima.resultado() == Resultado.APROBADA) {
            throw new IllegalArgumentException(
                    "solo se dejan de evaluar las que siguen a una no aprobada");
        }
        for (int i = todas.size(); i < orden.length; i++) {
            todas.add(noEvaluada(orden[i], motivoNoEvaluada(ultima.comprobacion())));
        }
        return List.copyOf(todas);
    }

    private static String motivoNoEvaluada(Comprobacion anterior) {
        return switch (anterior) {
            case MISMO_DOCUMENTO -> "El archivo no es la versión vigente.";
            case UNA_FIRMA_NUEVA -> "El archivo no tiene una sola firma nueva.";
            case INTEGRIDAD -> "La firma nueva no es íntegra.";
            case CONFIANZA, VIGENCIA, ORDEN -> throw new UnsupportedOperationException("T006");
            case FIRMANTE -> throw new IllegalArgumentException("es la última comprobación");
        };
    }
}
