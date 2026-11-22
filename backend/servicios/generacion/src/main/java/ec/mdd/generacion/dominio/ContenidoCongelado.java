package ec.mdd.generacion.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * El contenido congelado de una versión: su hash y la carga de cada docente, tal como quedaron al
 * congelarla (F20-H01, D-115). Se guarda una sola vez y no cambia.
 *
 * @param periodo el código del periodo
 * @param numero el número de la versión
 * @param hash su hash de contenido, el de {@link ContenidoDelParcial#hashDeLaVersion}
 * @param cargas la carga de cada docente con asignaciones, por nombre
 */
public record ContenidoCongelado(
        String periodo, int numero, String hash, List<CargaCongelada> cargas) {

    /** Exige los datos y ordena las cargas por nombre y, a igual nombre, por identificación. */
    public ContenidoCongelado {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hash, "hash");
        if (numero < 1) {
            throw new IllegalArgumentException("número de versión menor que 1");
        }
        cargas =
                cargas.stream()
                        .sorted(
                                Comparator.comparing(CargaCongelada::nombre)
                                        .thenComparing(CargaCongelada::identificacion))
                        .toList();
    }

    /**
     * Calcula el contenido congelado de una versión con la oferta y los docentes de ahora. Es una
     * función pura: no calcula reglas nuevas, solo junta lo que ya dan {@link
     * CargaHoraria#calcular} y {@link ContenidoDelParcial#hashDeLaVersion}. Una asignatura-paralelo
     * retirada no cuenta, como en la carga horaria.
     *
     * @param periodo el código del periodo
     * @param version la versión con sus propuestas y actividades
     * @param oferta las asignaturas-paralelo del periodo
     * @param nombresDeCarreras los nombres de las carreras por código; sin nombre, el código
     * @param activos los docentes activos por identificación; un docente que no está queda sin
     *     dedicación
     * @return el contenido, con un docente por cada uno con una propuesta o una actividad
     */
    public static ContenidoCongelado de(
            String periodo,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Map<String, String> nombresDeCarreras,
            Map<String, DocenteActivo> activos) {
        Map<String, AsignaturaParalelo> vigentes =
                oferta.stream()
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
        List<CargaCongelada> cargas =
                nombreDe.entrySet().stream()
                        .map(
                                docente ->
                                        carga(
                                                docente.getKey(),
                                                docente.getValue(),
                                                version,
                                                oferta,
                                                vigentes,
                                                nombresDeCarreras,
                                                Optional.ofNullable(activos.get(docente.getKey()))))
                        .toList();
        return new ContenidoCongelado(
                periodo,
                version.numero(),
                ContenidoDelParcial.hashDeLaVersion(periodo, version),
                cargas);
    }

    private static CargaCongelada carga(
            String docente,
            String nombre,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Map<String, AsignaturaParalelo> vigentes,
            Map<String, String> nombresDeCarreras,
            Optional<DocenteActivo> activo) {
        List<AsignaturaCongelada> asignaturas =
                version.propuestas().stream()
                        .filter(una -> una.docente().equals(docente))
                        .map(una -> vigentes.get(una.asignatura() + "/" + una.paralelo()))
                        .filter(Objects::nonNull)
                        .map(
                                una ->
                                        new AsignaturaCongelada(
                                                una.asignatura().codigo(),
                                                una.asignatura().nombre(),
                                                una.paralelo(),
                                                una.carrera(),
                                                nombresDeCarreras.getOrDefault(
                                                        una.carrera(), una.carrera()),
                                                una.horasSemanales()))
                        .toList();
        List<ActividadCongelada> actividades =
                version.actividades().stream()
                        .filter(una -> una.docente().equals(docente))
                        .map(
                                una ->
                                        new ActividadCongelada(
                                                una.actividad(),
                                                una.nombreActividad(),
                                                una.componente(),
                                                una.carrera(),
                                                una.proyecto(),
                                                una.nombreProyecto(),
                                                una.horas()))
                        .toList();
        return new CargaCongelada(
                docente,
                nombre,
                activo.map(DocenteActivo::tipoIdentificacion).orElse(null),
                activo.map(DocenteActivo::dedicacion).orElse(null),
                activo.map(DocenteActivo::horasContratadas).orElse(null),
                asignaturas,
                actividades,
                CargaHoraria.calcular(
                        docente, version.propuestas(), version.actividades(), oferta));
    }
}
