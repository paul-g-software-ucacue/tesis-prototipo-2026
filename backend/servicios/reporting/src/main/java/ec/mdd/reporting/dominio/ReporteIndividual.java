package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * El reporte individual de distributivo de un docente (RF-45, F20-H01): sus asignaturas y
 * paralelos, sus actividades y sus horas por componente, con su total, de la versión publicada
 * vigente. Es un agregado de lectura: no se guarda. El PDF, el CSV y la pantalla salen de él, con
 * el mismo orden y los mismos datos (RF-48).
 *
 * @param publicacion la versión publicada de la que sale
 * @param docente los datos del docente
 * @param asignaturas por carrera, código y paralelo
 * @param actividades por componente, nombre y código
 * @param horas sus horas por componente
 * @param generadoPor quién lo pidió; solo para el PDF y la pantalla
 * @param generadoEn cuándo; solo para el PDF y la pantalla
 */
public record ReporteIndividual(
        Publicacion publicacion,
        DatosDelDocente docente,
        List<FilaDeAsignatura> asignaturas,
        List<FilaDeActividad> actividades,
        HorasPorComponente horas,
        String generadoPor,
        Instant generadoEn) {

    private static final Comparator<FilaDeAsignatura> POR_ASIGNATURA =
            Comparator.comparing(FilaDeAsignatura::carrera)
                    .thenComparing(FilaDeAsignatura::codigo)
                    .thenComparing(FilaDeAsignatura::paralelo);

    private static final Comparator<FilaDeActividad> POR_ACTIVIDAD =
            Comparator.comparing(FilaDeActividad::componente)
                    .thenComparing(FilaDeActividad::nombre)
                    .thenComparing(FilaDeActividad::codigo);

    /** Exige los datos, ordena las filas y copia las listas. */
    public ReporteIndividual {
        Objects.requireNonNull(publicacion, "publicacion");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(horas, "horas");
        Objects.requireNonNull(generadoPor, "generadoPor");
        Objects.requireNonNull(generadoEn, "generadoEn");
        asignaturas = asignaturas.stream().sorted(POR_ASIGNATURA).toList();
        actividades = actividades.stream().sorted(POR_ACTIVIDAD).toList();
    }

    /**
     * Arma el reporte con el contenido congelado de la versión publicada. Comprueba que el
     * contenido sea el de esa versión, por su hash, y que sus filas sumen su carga horaria: si no,
     * el documento diría algo distinto de lo publicado.
     *
     * @param publicacion la versión publicada vigente
     * @param contenido el contenido congelado del docente
     * @param quien quién lo pide
     * @param momento cuándo
     * @return el reporte
     * @throws ContenidoInconsistente si el hash no coincide o las filas no suman su carga
     */
    public static ReporteIndividual de(
            Publicacion publicacion, ContenidoDelDocente contenido, String quien, Instant momento) {
        if (!publicacion.hash().equals(contenido.hash())) {
            throw new ContenidoInconsistente(
                    "El hash de la publicación "
                            + publicacion.hash()
                            + " no es el del contenido "
                            + contenido.hash()
                            + " (periodo "
                            + publicacion.periodo()
                            + ", versión "
                            + publicacion.version()
                            + ").");
        }
        exigirSuma(
                Componente.CLASE,
                contenido.asignaturas().stream().map(FilaDeAsignatura::horas).toList(),
                contenido.horas());
        for (Componente componente : Componente.values()) {
            if (componente != Componente.CLASE) {
                exigirSuma(
                        componente,
                        contenido.actividades().stream()
                                .filter(una -> una.componente() == componente)
                                .map(FilaDeActividad::horas)
                                .toList(),
                        contenido.horas());
            }
        }
        return new ReporteIndividual(
                publicacion,
                contenido.docente(),
                contenido.asignaturas(),
                contenido.actividades(),
                contenido.horas(),
                quien,
                momento);
    }

    private static void exigirSuma(
            Componente componente, List<BigDecimal> filas, HorasPorComponente horas) {
        BigDecimal suma = filas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(horas.de(componente)) != 0) {
            throw new ContenidoInconsistente(
                    "Las filas de "
                            + componente
                            + " suman "
                            + suma
                            + " y la carga dice "
                            + horas.de(componente)
                            + ".");
        }
    }
}
