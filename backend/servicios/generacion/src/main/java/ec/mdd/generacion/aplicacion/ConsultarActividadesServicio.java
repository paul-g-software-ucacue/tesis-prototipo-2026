package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.SumaPorComponente;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ActividadesDeCarrera;
import ec.mdd.generacion.puertos.entrada.ActividadesDelDocente;
import ec.mdd.generacion.puertos.entrada.ActividadesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.ConsultarActividades;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Las actividades por carrera y docente, con la suma por componente. Solo lee lo guardado: un
 * docente que ya no está activo se sigue viendo.
 */
public final class ConsultarActividadesServicio implements ConsultarActividades {

    private final RepositorioDistributivos distributivos;

    /** Recibe el repositorio de los distributivos. */
    public ConsultarActividadesServicio(RepositorioDistributivos distributivos) {
        this.distributivos = distributivos;
    }

    @Override
    public ActividadesDelPeriodo actividades(String periodo, Optional<String> carrera) {
        Optional<VersionDistributivo> vigente = distributivos.vigenteDe(periodo);
        if (vigente.isEmpty()) {
            return new ActividadesDelPeriodo(false, List.of());
        }
        VersionDistributivo version = vigente.get();
        TreeSet<String> carreras = new TreeSet<>();
        carrera.ifPresentOrElse(
                carreras::add,
                () -> version.parciales().forEach(uno -> carreras.add(uno.carrera())));
        return new ActividadesDelPeriodo(
                true,
                carreras.stream()
                        .map(una -> new ActividadesDeCarrera(una, docentes(una, version)))
                        .toList());
    }

    private static List<ActividadesDelDocente> docentes(
            String carrera, VersionDistributivo version) {
        return version.actividades().stream()
                .filter(una -> una.carrera().equals(carrera))
                .collect(Collectors.groupingBy(ActividadNoLectiva::docente))
                .entrySet()
                .stream()
                .map(
                        delDocente ->
                                new ActividadesDelDocente(
                                        delDocente.getKey(),
                                        delDocente.getValue().getFirst().nombreDocente(),
                                        delDocente.getValue(),
                                        SumaPorComponente.de(delDocente.getValue())))
                .sorted(Comparator.comparing(ActividadesDelDocente::nombreDocente))
                .toList();
    }
}
