package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Un docente activo en una sugerencia, con su puntaje, su desglose y sus horas (RF-25).
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres
 * @param dedicacion su dedicación
 * @param horasContratadas sus horas semanales contratadas
 * @param horas sus horas ya asignadas
 * @param puntaje su puntaje de afinidad en la asignatura, con el desglose
 */
public record Candidato(
        String identificacion,
        String nombre,
        Dedicacion dedicacion,
        int horasContratadas,
        HorasDelDocente horas,
        PuntajeAfinidad puntaje) {

    /** Exige los datos. */
    public Candidato {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dedicacion, "dedicacion");
        Objects.requireNonNull(horas, "horas");
        Objects.requireNonNull(puntaje, "puntaje");
    }

    /** Sus horas contratadas menos las asignadas, nunca menos de 0 (D-75). */
    public BigDecimal horasDisponibles() {
        return horas.disponibles(horasContratadas);
    }

    /** Si sus horas disponibles alcanzan para las del paralelo. */
    public boolean tieneHorasPara(BigDecimal horasDelParalelo) {
        return horasDisponibles().compareTo(horasDelParalelo) >= 0;
    }
}
