package ec.mdd.generacion.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * El docente que una carrera propone para una asignatura-paralelo (RF-24, D-19). Hay una sola por
 * asignatura-paralelo. El nombre es una copia, para seguir mostrándolo si el docente deja de estar
 * activo.
 *
 * @param asignatura el código de la asignatura
 * @param paralelo el paralelo
 * @param carrera el código de su carrera
 * @param docente la identificación del docente
 * @param nombreDocente sus apellidos y nombres
 * @param justificacion por qué se propuso
 * @param propuestaEn cuándo
 * @param propuestaPor quién
 */
public record Propuesta(
        String asignatura,
        String paralelo,
        String carrera,
        String docente,
        String nombreDocente,
        Justificacion justificacion,
        Instant propuestaEn,
        String propuestaPor) {

    /** Exige los datos. */
    public Propuesta {
        Objects.requireNonNull(asignatura, "asignatura");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(nombreDocente, "nombreDocente");
        Objects.requireNonNull(justificacion, "justificacion");
        Objects.requireNonNull(propuestaEn, "propuestaEn");
        Objects.requireNonNull(propuestaPor, "propuestaPor");
    }

    /** Si es la de esa asignatura-paralelo. */
    public boolean esDe(String otraAsignatura, String otroParalelo) {
        return asignatura.equals(otraAsignatura) && paralelo.equals(otroParalelo);
    }

    /**
     * Si es la misma propuesta que otra de su asignatura-paralelo: el mismo docente con la misma
     * justificación, también su texto. Guardarla no cambia nada (D-94); quién la propuso y cuándo
     * no cuentan.
     */
    public boolean mismosDatosQue(Propuesta otra) {
        return docente.equals(otra.docente) && justificacion.equals(otra.justificacion);
    }

    /** Si el texto de su justificación es otro que el de {@code otra} (D-94, sin decir cuál). */
    public boolean otroTextoQue(Propuesta otra) {
        return !Objects.equals(justificacion.texto(), otra.justificacion.texto());
    }

    /** La regla blanda que incumple, ya justificada: una excepción (MD-07). */
    public Optional<ReglaDeAsignacion> inconsistenciaJustificada() {
        return justificacion.tipo() == TipoDeJustificacion.EXCEPCION
                ? Optional.of(ReglaDeAsignacion.RN_19)
                : Optional.empty();
    }
}
