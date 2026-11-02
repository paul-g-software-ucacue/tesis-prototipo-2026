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
 * @param limite lo máximo que se le puede asignar
 * @param motivo la regla por la que no se le puede proponer; {@code null} si se puede
 */
public record Candidato(
        String identificacion,
        String nombre,
        Dedicacion dedicacion,
        int horasContratadas,
        HorasDelDocente horas,
        PuntajeAfinidad puntaje,
        BigDecimal limite,
        ReglaDeAsignacion motivo) {

    /** Exige los datos. */
    public Candidato {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dedicacion, "dedicacion");
        Objects.requireNonNull(horas, "horas");
        Objects.requireNonNull(puntaje, "puntaje");
        Objects.requireNonNull(limite, "limite");
    }

    /** Un candidato que se puede proponer, con sus horas contratadas como límite. */
    public Candidato(
            String identificacion,
            String nombre,
            Dedicacion dedicacion,
            int horasContratadas,
            HorasDelDocente horas,
            PuntajeAfinidad puntaje) {
        this(
                identificacion,
                nombre,
                dedicacion,
                horasContratadas,
                horas,
                puntaje,
                BigDecimal.valueOf(horasContratadas),
                null);
    }

    /** Si se le puede proponer (RF-28). */
    public boolean sePuedeProponer() {
        return motivo == null;
    }

    /** Su límite menos las horas asignadas, nunca menos de 0 (D-75). */
    public BigDecimal horasDisponibles() {
        return limite.subtract(horas.asignadas()).max(BigDecimal.ZERO);
    }

    /** Si sus horas disponibles alcanzan para las del paralelo. */
    public boolean tieneHorasPara(BigDecimal horasDelParalelo) {
        return horasDisponibles().compareTo(horasDelParalelo) >= 0;
    }
}
