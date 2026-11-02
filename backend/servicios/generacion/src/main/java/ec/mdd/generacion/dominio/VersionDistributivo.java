package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Una versión del distributivo: sus parciales y todas sus propuestas forman un solo agregado
 * (MD-01, RF-59). Es un valor: cada cambio lo guarda el repositorio, fila por fila.
 *
 * @param id su identificador
 * @param numero su número
 * @param estado su estado
 * @param parciales un parcial por carrera
 * @param propuestas una por asignatura-paralelo
 * @param actividades las actividades no lectivas de sus parciales
 */
public record VersionDistributivo(
        UUID id,
        int numero,
        EstadoDeVersion estado,
        List<ParcialCarrera> parciales,
        List<Propuesta> propuestas,
        List<ActividadNoLectiva> actividades) {

    /** Exige los datos y copia las listas. */
    public VersionDistributivo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(estado, "estado");
        parciales = List.copyOf(parciales);
        propuestas = List.copyOf(propuestas);
        actividades = List.copyOf(actividades);
    }

    /** Una versión sin actividades no lectivas. */
    public VersionDistributivo(
            UUID id,
            int numero,
            EstadoDeVersion estado,
            List<ParcialCarrera> parciales,
            List<Propuesta> propuestas) {
        this(id, numero, estado, parciales, propuestas, List.of());
    }

    /** La versión 1, editable, con un parcial en elaboración por carrera. */
    public static VersionDistributivo primera(UUID id, List<String> carreras) {
        return new VersionDistributivo(
                id,
                1,
                EstadoDeVersion.EDITABLE,
                carreras.stream().distinct().sorted().map(ParcialCarrera::nuevo).toList(),
                List.of());
    }

    /** La versión con esa propuesta, que reemplaza a la de su paralelo. No cambia esta versión. */
    public VersionDistributivo conPropuesta(Propuesta propuesta) {
        List<Propuesta> nuevas =
                Stream.concat(
                                propuestas.stream()
                                        .filter(
                                                una ->
                                                        !una.esDe(
                                                                propuesta.asignatura(),
                                                                propuesta.paralelo())),
                                Stream.of(propuesta))
                        .toList();
        return new VersionDistributivo(id, numero, estado, parciales, nuevas, actividades);
    }

    /** La versión con esa actividad, que reemplaza a la misma. No cambia esta versión. */
    public VersionDistributivo conActividad(ActividadNoLectiva actividad) {
        List<ActividadNoLectiva> nuevas =
                Stream.concat(
                                actividades.stream()
                                        .filter(
                                                una ->
                                                        !una.esDe(
                                                                actividad.carrera(),
                                                                actividad.docente(),
                                                                actividad.actividad(),
                                                                actividad.proyecto())),
                                Stream.of(actividad))
                        .toList();
        return new VersionDistributivo(id, numero, estado, parciales, propuestas, nuevas);
    }

    /** El parcial de esa carrera, si ya existe. */
    public Optional<ParcialCarrera> parcialDe(String carrera) {
        return parciales.stream().filter(uno -> uno.carrera().equals(carrera)).findFirst();
    }

    /** Esa actividad del parcial, si la tiene; el proyecto es parte de la llave. */
    public Optional<ActividadNoLectiva> actividadDe(
            String carrera, String docente, String actividad, String proyecto) {
        return actividades.stream()
                .filter(una -> una.esDe(carrera, docente, actividad, proyecto))
                .findFirst();
    }

    /** La propuesta de esa asignatura-paralelo, si tiene. */
    public Optional<Propuesta> propuestaDe(String asignatura, String paralelo) {
        return propuestas.stream().filter(una -> una.esDe(asignatura, paralelo)).findFirst();
    }

    /**
     * Comprueba que la carrera puede cambiar sus propuestas. Una carrera sin parcial recibe el suyo
     * con la primera propuesta.
     *
     * @throws ParcialNoEditable si la versión no está editable o el parcial se envió
     */
    public void exigirEditable(String carrera) {
        boolean parcialCerrado =
                parcialDe(carrera).map(parcial -> !parcial.editable()).orElse(false);
        if (estado != EstadoDeVersion.EDITABLE || parcialCerrado) {
            throw new ParcialNoEditable();
        }
    }

    /** El avance de una carrera frente a la oferta: una retirada no cuenta. */
    public AvanceDelParcial avance(String carrera, List<AsignaturaParalelo> oferta) {
        List<AsignaturaParalelo> ofertadas =
                oferta.stream()
                        .filter(una -> una.carrera().equals(carrera) && !una.retirada())
                        .toList();
        long conPropuesta =
                ofertadas.stream()
                        .filter(
                                una ->
                                        propuestaDe(una.asignatura().codigo(), una.paralelo())
                                                .isPresent())
                        .count();
        return new AvanceDelParcial(ofertadas.size(), Math.toIntExact(conPropuesta));
    }
}
