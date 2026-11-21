package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * El contenido de un docente en una versión congelada, guardado una sola vez al congelarla
 * (F20-H01, D-115). Las horas de clase de la versión se calculan con la oferta de Académico, que se
 * puede corregir después de publicar; guardarlas al congelar hace que lo publicado no cambie y
 * coincida con el documento firmado (principio VII).
 *
 * <p>No calcula nada: la carga horaria es la de {@link CargaHoraria#calcular}, la misma regla de
 * RF-27. Solo comprueba que sus asignaciones la sumen.
 *
 * @param identificacion el número de identificación del docente
 * @param nombre sus apellidos y nombres, como en la asignación
 * @param dedicacionAlCongelar su dedicación al congelar; {@code null} si ya no estaba activo
 * @param horasContratadasAlCongelar sus horas contratadas al congelar, de 1 a 40; {@code null} si
 *     no estaba activo
 * @param asignaturas sus asignaturas-paralelo, por carrera, código y paralelo
 * @param actividades sus actividades no lectivas, por componente y nombre
 * @param horas su carga horaria por componente
 */
public record CargaCongelada(
        String identificacion,
        String nombre,
        Dedicacion dedicacionAlCongelar,
        Integer horasContratadasAlCongelar,
        List<AsignaturaCongelada> asignaturas,
        List<ActividadCongelada> actividades,
        CargaHoraria horas) {

    private static final Comparator<AsignaturaCongelada> POR_ASIGNATURA =
            Comparator.comparing(AsignaturaCongelada::carrera)
                    .thenComparing(AsignaturaCongelada::codigo)
                    .thenComparing(AsignaturaCongelada::paralelo);

    private static final Comparator<ActividadCongelada> POR_ACTIVIDAD =
            Comparator.comparing(ActividadCongelada::componente)
                    .thenComparing(ActividadCongelada::nombre)
                    .thenComparing(ActividadCongelada::codigo);

    /** Exige los datos, ordena las asignaciones y comprueba que sumen la carga horaria. */
    public CargaCongelada {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(horas, "horas");
        if (horasContratadasAlCongelar != null
                && (horasContratadasAlCongelar < 1 || horasContratadasAlCongelar > 40)) {
            throw new IllegalArgumentException("horas contratadas fuera de 1 a 40");
        }
        asignaturas = asignaturas.stream().sorted(POR_ASIGNATURA).toList();
        actividades = actividades.stream().sorted(POR_ACTIVIDAD).toList();
        exigirSuma(
                Componente.CLASE,
                asignaturas.stream().map(AsignaturaCongelada::horas).toList(),
                horas.clase());
        for (Componente componente : Componente.values()) {
            if (componente != Componente.CLASE) {
                exigirSuma(
                        componente,
                        actividades.stream()
                                .filter(una -> una.componente() == componente)
                                .map(ActividadCongelada::horas)
                                .toList(),
                        de(horas, componente));
            }
        }
    }

    /** Su dedicación al congelar; vacía si ya no estaba activo. */
    public Optional<Dedicacion> dedicacion() {
        return Optional.ofNullable(dedicacionAlCongelar);
    }

    /** Sus horas contratadas al congelar; vacías si ya no estaba activo. */
    public OptionalInt horasContratadas() {
        return horasContratadasAlCongelar == null
                ? OptionalInt.empty()
                : OptionalInt.of(horasContratadasAlCongelar);
    }

    private static void exigirSuma(
            Componente componente, List<BigDecimal> partes, BigDecimal esperada) {
        BigDecimal suma = partes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(esperada) != 0) {
            throw new IllegalArgumentException(
                    "las asignaciones de " + componente + " suman " + suma + ", no " + esperada);
        }
    }

    private static BigDecimal de(CargaHoraria horas, Componente componente) {
        return switch (componente) {
            case CLASE -> horas.clase();
            case OTRAS_DOCENCIA -> horas.otrasDocencia();
            case INVESTIGACION -> horas.investigacion();
            case VINCULACION -> horas.vinculacion();
            case GESTION -> horas.gestion();
        };
    }
}
