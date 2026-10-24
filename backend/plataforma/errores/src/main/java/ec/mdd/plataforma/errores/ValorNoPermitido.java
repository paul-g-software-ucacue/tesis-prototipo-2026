package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.ValueInstantiationException;

/**
 * El detalle de un cuerpo con un valor que no es de una enumeración del contrato, como una
 * dedicación «XX» (escenario «Edición inválida» de F03-H04): su campo, con la posición en las
 * listas ({@code ficha.cuartoNivel[1].tipo}), y los valores permitidos. Nunca repite el valor
 * recibido ni el mensaje del analizador (principio XIII).
 */
final class ValorNoPermitido {

    private ValorNoPermitido() {}

    /** El detalle, si alguna causa de la excepción es un valor fuera de una enumeración. */
    static Optional<DetalleRespuesta> de(Throwable excepcion) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            Class<?> destino = destino(causa);
            if (destino != null && destino.isEnum() && causa instanceof JacksonException jackson) {
                String campo = campo(jackson.getPath());
                if (!campo.isEmpty()) {
                    return Optional.of(
                            new DetalleRespuesta(
                                    campo,
                                    "no es un valor permitido: usa "
                                            + permitidos(destino.getEnumConstants())));
                }
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return Optional.empty();
    }

    private static Class<?> destino(Throwable causa) {
        if (causa instanceof MismatchedInputException desajuste) {
            return desajuste.getTargetType();
        }
        if (causa instanceof ValueInstantiationException creacion && creacion.getType() != null) {
            return creacion.getType().getRawClass();
        }
        return null;
    }

    private static String campo(List<JacksonException.Reference> camino) {
        StringBuilder campo = new StringBuilder();
        for (JacksonException.Reference paso : camino) {
            if (paso.getPropertyName() != null) {
                if (!campo.isEmpty()) {
                    campo.append('.');
                }
                campo.append(paso.getPropertyName());
            } else if (paso.getIndex() >= 0) {
                campo.append('[').append(paso.getIndex()).append(']');
            }
        }
        return campo.toString();
    }

    // «TC, MT o TP»: el texto de cada constante es su valor en el contrato.
    private static String permitidos(Object[] constantes) {
        List<String> valores = Arrays.stream(constantes).map(String::valueOf).toList();
        if (valores.size() == 1) {
            return valores.getFirst();
        }
        return String.join(", ", valores.subList(0, valores.size() - 1))
                + " o "
                + valores.getLast();
    }
}
