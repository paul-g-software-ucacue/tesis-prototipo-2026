package ec.mdd.cargas.dominio;

/** Si un problema de una fila impide continuar (glosario «Informe de validación de la carga»). */
public enum TipoDeProblema {
    /** Impide continuar: el archivo queda rechazado. */
    ERROR,
    /** No lo impide: avisa de algo que el sistema tolera y ajusta. */
    ADVERTENCIA
}
