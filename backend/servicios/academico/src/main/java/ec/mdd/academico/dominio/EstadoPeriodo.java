package ec.mdd.academico.dominio;

import java.util.Optional;

/**
 * El estado de un periodo académico (RF-13, glosario §7.3). Solo avanza, un paso a la vez y en el
 * orden en que se declaran: Planificación, En aprobación, Vigente y Cerrado (pregunta 4 del modelo
 * de dominio, F02-H01).
 */
public enum EstadoPeriodo {
    /** Recién registrado; aquí se abre, al elegir su versión de normativa. */
    PLANIFICACION,
    /** El distributivo del periodo está en su flujo de aprobación. */
    EN_APROBACION,
    /** El periodo está en curso. */
    VIGENTE,
    /** El último estado: el periodo ya no cambia. */
    CERRADO;

    /** El estado que sigue; vacío en {@link #CERRADO}. */
    public Optional<EstadoPeriodo> siguiente() {
        EstadoPeriodo[] estados = values();
        return ordinal() + 1 < estados.length
                ? Optional.of(estados[ordinal() + 1])
                : Optional.empty();
    }
}
