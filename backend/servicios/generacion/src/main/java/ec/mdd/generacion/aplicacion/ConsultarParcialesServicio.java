package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.HorasDelPeriodo;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarParciales;
import ec.mdd.generacion.puertos.entrada.ParcialConAvance;
import ec.mdd.generacion.puertos.entrada.ParcialesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El avance de cada parcial frente a la oferta de ese momento: una propuesta de un paralelo
 * retirado se muestra, pero no cuenta. Una carrera que ofertó después de crear el distributivo
 * aparece, con su parcial todavía sin propuestas.
 */
public final class ConsultarParcialesServicio implements ConsultarParciales {

    private final DatosParaSugerencia datos;
    private final RepositorioDistributivos distributivos;

    /** Recibe los puertos de salida. */
    public ConsultarParcialesServicio(
            DatosParaSugerencia datos, RepositorioDistributivos distributivos) {
        this.datos = datos;
        this.distributivos = distributivos;
    }

    @Override
    public ParcialesDelPeriodo parciales(String periodo, Optional<String> carrera) {
        List<AsignaturaParalelo> oferta =
                datos.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new);
        Optional<VersionDistributivo> vigente = distributivos.vigenteDe(periodo);
        if (vigente.isEmpty()) {
            return new ParcialesDelPeriodo(false, List.of());
        }
        VersionDistributivo version = vigente.get();
        Map<String, DocenteActivo> docentes =
                datos.docentesActivos().stream()
                        .collect(
                                Collectors.toMap(
                                        DocenteActivo::identificacion, Function.identity()));
        TreeSet<String> carreras = new TreeSet<>();
        version.parciales().forEach(parcial -> carreras.add(parcial.carrera()));
        oferta.stream().filter(una -> !una.retirada()).forEach(una -> carreras.add(una.carrera()));
        return new ParcialesDelPeriodo(
                true,
                carreras.stream()
                        .filter(una -> carrera.isEmpty() || carrera.get().equals(una))
                        .map(una -> parcial(una, version, oferta, docentes))
                        .toList());
    }

    private static ParcialConAvance parcial(
            String carrera,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Map<String, DocenteActivo> docentes) {
        return new ParcialConAvance(
                version.parcialDe(carrera).orElseGet(() -> ParcialCarrera.nuevo(carrera)),
                version.avance(carrera, oferta),
                version.propuestas().stream()
                        .filter(propuesta -> propuesta.carrera().equals(carrera))
                        .map(propuesta -> vista(propuesta, version, oferta, docentes))
                        .toList());
    }

    private static PropuestaVista vista(
            Propuesta propuesta,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Map<String, DocenteActivo> docentes) {
        Optional<AsignaturaParalelo> suyo =
                oferta.stream()
                        .filter(una -> propuesta.esDe(una.asignatura().codigo(), una.paralelo()))
                        .findFirst();
        return new PropuestaVista(
                propuesta,
                suyo.map(AsignaturaParalelo::retirada).orElse(true),
                suyo.map(una -> alcanzan(propuesta, una, version, oferta, docentes)).orElse(true));
    }

    private static boolean alcanzan(
            Propuesta propuesta,
            AsignaturaParalelo paralelo,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Map<String, DocenteActivo> docentes) {
        DocenteActivo docente = docentes.get(propuesta.docente());
        if (docente == null) {
            return true;
        }
        BigDecimal disponibles =
                HorasDelPeriodo.calcular(version.propuestas(), oferta, paralelo)
                        .getOrDefault(propuesta.docente(), HorasDelDocente.NINGUNA)
                        .disponibles(docente.horasContratadas());
        return disponibles.compareTo(paralelo.horasSemanales()) >= 0;
    }
}
