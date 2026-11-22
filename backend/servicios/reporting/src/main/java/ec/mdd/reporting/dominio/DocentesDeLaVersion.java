package ec.mdd.reporting.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Los docentes de una versión congelada, con el hash de esa versión (D-115).
 *
 * @param hash el hash de la versión
 * @param docentes por nombre y, a igual nombre, por identificación
 */
public record DocentesDeLaVersion(String hash, List<Docente> docentes) {

    /** Exige el hash y copia la lista. */
    public DocentesDeLaVersion {
        Objects.requireNonNull(hash, "hash");
        docentes = List.copyOf(docentes);
    }

    /**
     * Un docente de la versión, con las horas de su carga congelada (F20-H02, D-117): las mismas
     * que lee su reporte individual, así el general no puede decir otra cosa.
     *
     * @param identificacion su número de identificación
     * @param nombre sus apellidos y nombres
     * @param dedicacionAlCongelar su dedicación; {@code null} si no estaba activo al congelar
     * @param horas sus horas por componente en la versión
     */
    public record Docente(
            String identificacion,
            String nombre,
            Dedicacion dedicacionAlCongelar,
            HorasPorComponente horas) {

        /** Exige la identificación, el nombre y las horas. */
        public Docente {
            Objects.requireNonNull(identificacion, "identificacion");
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(horas, "horas");
        }

        /** Su dedicación; vacía si no estaba activo al congelar («sin dato» en el reporte). */
        public Optional<Dedicacion> dedicacion() {
            return Optional.ofNullable(dedicacionAlCongelar);
        }
    }
}
