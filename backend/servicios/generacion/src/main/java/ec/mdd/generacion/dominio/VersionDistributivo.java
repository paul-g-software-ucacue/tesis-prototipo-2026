package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Una versión del distributivo: sus parciales y todas sus propuestas forman un solo agregado
 * (MD-01, RF-59). Es un valor: cada cambio lo guarda el repositorio, fila por fila.
 *
 * @param id su identificador
 * @param numero su número
 * @param estado su estado
 * @param parciales un parcial por carrera
 * @param propuestas una por asignatura-paralelo
 */
public record VersionDistributivo(
        UUID id,
        int numero,
        EstadoDeVersion estado,
        List<ParcialCarrera> parciales,
        List<Propuesta> propuestas) {

    /** Exige los datos y copia las listas. */
    public VersionDistributivo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(estado, "estado");
        parciales = List.copyOf(parciales);
        propuestas = List.copyOf(propuestas);
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

    /** El parcial de esa carrera, si ya existe. */
    public Optional<ParcialCarrera> parcialDe(String carrera) {
        return parciales.stream().filter(uno -> uno.carrera().equals(carrera)).findFirst();
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
