package ec.mdd.generacion.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Un docente de una versión congelada, con su dedicación y sus horas por componente (F20-H01,
 * F20-H02, D-117). Las horas son las de su {@link CargaCongelada}, guardadas al congelar: así el
 * reporte general de Reporting y el individual leen las mismas cifras y no pueden diferir. No suma
 * nada: los totales institucionales los calcula Reporting.
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres, como en la asignación
 * @param dedicacionAlCongelar su dedicación; {@code null} si ya no estaba activo al congelar
 * @param horas sus horas por componente en la versión
 */
public record DocenteDeLaVersion(
        String identificacion, String nombre, Dedicacion dedicacionAlCongelar, CargaHoraria horas) {

    /** Exige la identificación, el nombre y las horas. */
    public DocenteDeLaVersion {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(horas, "horas");
    }

    /**
     * @param carga la carga congelada del docente
     * @return el docente con su dedicación y sus horas
     */
    public static DocenteDeLaVersion de(CargaCongelada carga) {
        return new DocenteDeLaVersion(
                carga.identificacion(),
                carga.nombre(),
                carga.dedicacion().orElse(null),
                carga.horas());
    }

    /** Su dedicación al congelar; vacía si ya no estaba activo. */
    public Optional<Dedicacion> dedicacion() {
        return Optional.ofNullable(dedicacionAlCongelar);
    }
}
