package ec.mdd.reporting.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Un docente del reporte general (F20-H02, RF-46): su identificación, su nombre, su dedicación y
 * sus horas por componente, con su total ({@link HorasPorComponente#total()}).
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres
 * @param dedicacionAlCongelar su dedicación; {@code null} si no estaba activo al congelar
 * @param horas sus horas por componente en la versión publicada
 */
public record FilaDelDocente(
        String identificacion,
        String nombre,
        Dedicacion dedicacionAlCongelar,
        HorasPorComponente horas) {

    /** Exige la identificación, el nombre y las horas. */
    public FilaDelDocente {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(horas, "horas");
    }

    /** Su dedicación; vacía si no estaba activo al congelar («sin dato» en el reporte). */
    public Optional<Dedicacion> dedicacion() {
        return Optional.ofNullable(dedicacionAlCongelar);
    }
}
