package ec.mdd.generacion.dominio;

import java.time.Instant;
import java.util.Objects;

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
}
