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
 * @param tipoIdentificacionAlCongelar cédula o pasaporte; {@code null} si no estaba activo o si el
 *     contenido se guardó antes de F19-H01 (D-119)
 */
public record DatosDelDocente(
        String identificacion,
        String nombre,
        Dedicacion dedicacionAlCongelar,
        Integer horasContratadasAlCongelar,
        TipoDeIdentificacion tipoIdentificacionAlCongelar) {

    /** Exige la identificación y el nombre. */
    public DatosDelDocente {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
    }

    /** Los datos de los reportes, que no usan el tipo de identificación. */
    public DatosDelDocente(
            String identificacion,
            String nombre,
            Dedicacion dedicacionAlCongelar,
            Integer horasContratadasAlCongelar) {
        this(identificacion, nombre, dedicacionAlCongelar, horasContratadasAlCongelar, null);
    }

    /** Su tipo de identificación; vacío si no se conoce («NA» en el CSV para SIGA). */
    public Optional<TipoDeIdentificacion> tipoIdentificacion() {
        return Optional.ofNullable(tipoIdentificacionAlCongelar);
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
