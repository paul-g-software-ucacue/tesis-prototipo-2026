package ec.mdd.reporting.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Los datos del docente que imprime el reporte, tal como quedaron al congelar la versión (D-115).
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres
 * @param dedicacionAlCongelar su dedicación; {@code null} si ya no estaba activo al congelar
 * @param horasContratadasAlCongelar sus horas contratadas; {@code null} si no estaba activo
 */
public record DatosDelDocente(
        String identificacion,
        String nombre,
        Dedicacion dedicacionAlCongelar,
        Integer horasContratadasAlCongelar) {

    /** Exige la identificación y el nombre. */
    public DatosDelDocente {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
    }

    /** Su dedicación; vacía si no estaba activo al congelar («sin dato» en el reporte). */
    public Optional<Dedicacion> dedicacion() {
        return Optional.ofNullable(dedicacionAlCongelar);
    }

    /** Sus horas contratadas; vacías si no estaba activo al congelar. */
    public OptionalInt horasContratadas() {
        return horasContratadasAlCongelar == null
                ? OptionalInt.empty()
                : OptionalInt.of(horasContratadasAlCongelar);
    }
}
