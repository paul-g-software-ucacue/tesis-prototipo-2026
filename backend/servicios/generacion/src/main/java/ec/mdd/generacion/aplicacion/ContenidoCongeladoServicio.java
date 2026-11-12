package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.ContenidoCongelado;
import ec.mdd.generacion.puertos.entrada.ContenidoDeLaVersion;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.VersionNoCongelada;
import ec.mdd.generacion.puertos.salida.DatosParaElDocumento;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * El contenido de la versión congelada para el documento firmable (F13-H02, D-101). Junta lo que ya
 * calculan el consolidado y la carga horaria, sin cálculos nuevos: el resumen por carrera, los
 * totales y, por docente, sus asignaturas-paralelo, sus actividades no lectivas y sus horas por
 * componente. Una asignatura-paralelo retirada no cuenta, como en la carga horaria.
 */
public final class ContenidoCongeladoServicio implements ContenidoCongelado {

    private static final Comparator<ContenidoDeLaVersion.Asignatura> POR_ASIGNATURA =
            Comparator.comparing(ContenidoDeLaVersion.Asignatura::codigo)
                    .thenComparing(ContenidoDeLaVersion.Asignatura::paralelo);

    private final RepositorioDistributivos distributivos;
    private final ConsultarConsolidado consolidado;
    private final DatosParaSugerencia oferta;
    private final DatosParaElDocumento datos;
    private final ParcialesDelDistributivo parciales;

    /**
     * @param distributivos las versiones del distributivo
     * @param consolidado el consolidado, con su resumen por carrera y sus totales
     * @param oferta la oferta del periodo, por los nombres de las asignaturas
     * @param datos los nombres de las carreras
     * @param parciales el hash del distributivo
     */
    public ContenidoCongeladoServicio(
            RepositorioDistributivos distributivos,
            ConsultarConsolidado consolidado,
            DatosParaSugerencia oferta,
            DatosParaElDocumento datos,
            ParcialesDelDistributivo parciales) {
        this.distributivos = distributivos;
        this.consolidado = consolidado;
        this.oferta = oferta;
        this.datos = datos;
        this.parciales = parciales;
    }

    @Override
    public ContenidoDeLaVersion contenido(String periodo) {
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(DistributivoNoEncontrado::new);
        if (version.estado() != EstadoDeVersion.CONGELADA) {
            throw new VersionNoCongelada();
        }
        List<AsignaturaParalelo> ofertadas =
                oferta.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new);
        ConsolidadoDelPeriodo resumen = consolidado.consolidado(periodo);
        Map<String, String> nombres = datos.nombresDeCarreras();
        List<ContenidoDeLaVersion.Carrera> carreras =
                resumen.carreras().stream()
                        .map(
                                una ->
                                        new ContenidoDeLaVersion.Carrera(
                                                una.carrera(),
                                                nombres.getOrDefault(una.carrera(), una.carrera()),
                                                una.estado().name(),
                                                una.avance().ofertadas(),
                                                una.avance().conPropuesta(),
                                                una.horas()))
                        .toList();
        return new ContenidoDeLaVersion(
                periodo,
                version.numero(),
                parciales.hashDelDistributivo(periodo),
                carreras,
                resumen.totales(),
                docentes(version, ofertadas));
    }

    // Un docente por cada uno con una propuesta o una actividad, por nombre.
    private static List<ContenidoDeLaVersion.Docente> docentes(
            VersionDistributivo version, List<AsignaturaParalelo> ofertadas) {
        Map<String, AsignaturaParalelo> vigentes =
                ofertadas.stream()
                        .filter(una -> !una.retirada())
                        .collect(
                                Collectors.toMap(
                                        una -> una.asignatura().codigo() + "/" + una.paralelo(),
                                        Function.identity(),
                                        (primera, segunda) -> primera));
        Map<String, String> nombreDe = new TreeMap<>();
        Stream.concat(
                        version.propuestas().stream()
                                .map(una -> Map.entry(una.docente(), una.nombreDocente())),
                        version.actividades().stream()
                                .map(una -> Map.entry(una.docente(), una.nombreDocente())))
                .forEach(par -> nombreDe.putIfAbsent(par.getKey(), par.getValue()));
        Map<String, ContenidoDeLaVersion.Docente> porDocente = new LinkedHashMap<>();
        nombreDe.forEach(
                (docente, nombre) ->
                        porDocente.put(
                                docente,
                                new ContenidoDeLaVersion.Docente(
                                        docente,
                                        nombre,
                                        asignaturas(docente, version.propuestas(), vigentes),
                                        actividades(docente, version.actividades()),
                                        CargaHoraria.calcular(
                                                docente,
                                                version.propuestas(),
                                                version.actividades(),
                                                ofertadas))));
        return porDocente.values().stream()
                .sorted(
                        Comparator.comparing(ContenidoDeLaVersion.Docente::nombre)
                                .thenComparing(ContenidoDeLaVersion.Docente::identificacion))
                .toList();
    }

    private static List<ContenidoDeLaVersion.Asignatura> asignaturas(
            String docente, List<Propuesta> propuestas, Map<String, AsignaturaParalelo> vigentes) {
        return propuestas.stream()
                .filter(una -> una.docente().equals(docente))
                .map(
                        una ->
                                Optional.ofNullable(
                                        vigentes.get(una.asignatura() + "/" + una.paralelo())))
                .flatMap(Optional::stream)
                .map(
                        una ->
                                new ContenidoDeLaVersion.Asignatura(
                                        una.asignatura().codigo(),
                                        una.asignatura().nombre(),
                                        una.paralelo(),
                                        una.carrera(),
                                        una.horasSemanales()))
                .sorted(POR_ASIGNATURA)
                .toList();
    }

    private static List<ContenidoDeLaVersion.Actividad> actividades(
            String docente, List<ActividadNoLectiva> actividades) {
        return actividades.stream()
                .filter(una -> una.docente().equals(docente))
                .map(
                        una ->
                                new ContenidoDeLaVersion.Actividad(
                                        una.nombreActividad(),
                                        una.componente().name(),
                                        una.horas()))
                .sorted(Comparator.comparing(ContenidoDeLaVersion.Actividad::nombre))
                .toList();
    }
}
