package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ActividadesDeCarrera;
import ec.mdd.generacion.puertos.entrada.ActividadesDelDocente;
import ec.mdd.generacion.puertos.entrada.ActividadesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.ConsultarActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Las actividades por carrera y docente, con los totales de cada docente en todas las carreras
 * (RF-27). Las actividades son solo las de la carrera. Un docente que ya no está activo se sigue
 * viendo. Necesita la oferta para sumar las clases.
 */
public final class ConsultarActividadesServicio implements ConsultarActividades {

    private final DatosParaSugerencia datos;
    private final RepositorioDistributivos distributivos;

    /** Recibe la oferta y el repositorio de los distributivos. */
    public ConsultarActividadesServicio(
            DatosParaSugerencia datos, RepositorioDistributivos distributivos) {
        this.datos = datos;
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
        List<AsignaturaParalelo> oferta = datos.ofertaDelPeriodo(periodo).orElse(List.of());
        return new ActividadesDelPeriodo(
                true,
                carreras.stream()
                        .map(una -> new ActividadesDeCarrera(una, docentes(una, version, oferta)))
                        .toList());
    }

    private static List<ActividadesDelDocente> docentes(
            String carrera, VersionDistributivo version, List<AsignaturaParalelo> oferta) {
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
                                        CargaHoraria.calcular(
                                                delDocente.getKey(),
                                                version.propuestas(),
                                                version.actividades(),
                                                oferta)))
                .sorted(Comparator.comparing(ActividadesDelDocente::nombreDocente))
                .toList();
    }
}
