package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * El valor de una celda del CSV para SIGA, tipado y sin formato de texto (D-119). Cómo se escribe
 * (mayúsculas, «NA», decimales, fecha, comillas y fórmulas) lo decide el adaptador del puerto
 * {@code ExportadorSiga}, no el dominio (principio I).
 */
public sealed interface ValorDeColumna {

    /** Lo que no aplica a la fila, o un dato que falta. */
    ValorDeColumna NO_APLICA = new NoAplica();

    /**
     * Un texto o un código, ya traducido si la columna traduce.
     *
     * @param valor el texto, nunca vacío
     */
    record Texto(String valor) implements ValorDeColumna {

        /** Exige el texto. */
        public Texto {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /**
     * Un número entero, como la versión.
     *
     * @param valor el número
     */
    record Entero(int valor) implements ValorDeColumna {}

    /**
     * Las horas semanales, con hasta dos decimales.
     *
     * @param valor las horas
     */
    record Horas(BigDecimal valor) implements ValorDeColumna {

        /** Exige las horas y compara por valor, no por escala. */
        public Horas {
            Objects.requireNonNull(valor, "valor");
            valor = valor.stripTrailingZeros();
        }
    }

    /**
     * Una fecha del calendario de Ecuador.
     *
     * @param valor la fecha
     */
    record Fecha(LocalDate valor) implements ValorDeColumna {

        /** Exige la fecha. */
        public Fecha {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /** Lo que no aplica. */
    record NoAplica() implements ValorDeColumna {}
}
