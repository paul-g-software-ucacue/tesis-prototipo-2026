package ec.mdd.cargas.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Un error o una advertencia de una fila del archivo de una carga (RF-09).
 *
 * @param fila la fila del archivo, contando el encabezado como la 1
 * @param campo el encabezado de la columna; vacío si es de toda la fila
 * @param valor el valor recibido, sin los espacios de los extremos
 * @param motivo qué hacer, en español; no repite el valor (principio IX)
 * @param tipo si es un error o una advertencia
 * @param sugerencia el valor corregido, si la regla lo puede proponer
 */
public record ProblemaDeFila(
        int fila,
        String campo,
        String valor,
        String motivo,
        TipoDeProblema tipo,
        Optional<String> sugerencia) {

    /** Valida el problema. */
    public ProblemaDeFila {
        if (fila < 2) {
            throw new IllegalArgumentException(
                    "La fila 1 es el encabezado; un problema va desde la 2");
        }
        Objects.requireNonNull(campo);
        Objects.requireNonNull(valor);
        Objects.requireNonNull(motivo);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(sugerencia);
    }

    /** Un error sin sugerencia. */
    static ProblemaDeFila error(int fila, String campo, String valor, String motivo) {
        return new ProblemaDeFila(
                fila, campo, valor, motivo, TipoDeProblema.ERROR, Optional.empty());
    }

    /** Una advertencia. */
    static ProblemaDeFila advertencia(int fila, String campo, String valor, String motivo) {
        return new ProblemaDeFila(
                fila, campo, valor, motivo, TipoDeProblema.ADVERTENCIA, Optional.empty());
    }
}
