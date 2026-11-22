package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Las filas del CSV para SIGA de una versión publicada, comprobadas contra el reporte general de la
 * misma versión (RF-52, F19-H01, D-119). Es un agregado de lectura: no se guarda.
 *
 * <p>Las horas salen de dos lecturas distintas del contenido congelado: el reporte general lee las
 * horas guardadas de cada docente, y estas filas las suman de sus asignaturas y actividades. Por
 * eso compararlas tiene sentido: si alguna vez no cuadran, el archivo no se entrega (FR-005), en
 * vez de mandar a SIGA totales distintos de los del documento firmado.
 *
 * <p>No conoce el formato del archivo: lo escribe el puerto {@code ExportadorSiga} (principio I).
 *
 * @param publicacion la versión publicada de la que salen
 * @param filas una por asignatura-paralelo y por actividad, en orden
 */
public record FilasParaSiga(Publicacion publicacion, List<FilaSiga> filas) {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-EC");

    /** Exige la publicación y copia las filas. */
    public FilasParaSiga {
        Objects.requireNonNull(publicacion, "publicacion");
        filas = List.copyOf(filas);
    }

    /**
     * @param publicacion la versión publicada vigente
     * @param cargas el contenido completo de sus docentes
     * @param general el reporte general de la misma publicación
     * @return las filas, en el orden de research §5
     * @throws ContenidoInconsistente si un hash no es el de la publicación, un docente está
     *     repetido, falta o sobra, o una suma no es la del reporte general
     */
    public static FilasParaSiga de(
            Publicacion publicacion, CargasDeLaVersion cargas, ReporteGeneral general) {
        exigirLaMismaVersion(publicacion, cargas, general);
        List<FilaSiga> filas = new ArrayList<>();
        Map<String, HorasPorComponente> sumas = new LinkedHashMap<>();
        for (ContenidoDelDocente carga : cargas.cargas()) {
            DatosDelDocente docente = carga.docente();
            if (sumas.putIfAbsent(docente.identificacion(), HorasPorComponente.NINGUNA) != null) {
                throw new ContenidoInconsistente(
                        "El docente "
                                + docente.identificacion()
                                + " está repetido en el contenido de la versión "
                                + publicacion.version()
                                + " del periodo "
                                + publicacion.periodo()
                                + ".");
            }
            for (FilaDeAsignatura asignatura : carga.asignaturas()) {
                filas.add(FilaSiga.de(publicacion, docente, asignatura));
            }
            for (FilaDeActividad actividad : carga.actividades()) {
                filas.add(FilaSiga.de(publicacion, docente, actividad));
            }
        }
        for (FilaSiga fila : filas) {
            sumas.merge(fila.docente().identificacion(), deUnaFila(fila), HorasPorComponente::mas);
        }
        exigirLosTotales(publicacion, sumas, general);
        filas.sort(orden());
        return new FilasParaSiga(publicacion, filas);
    }

    private static void exigirLaMismaVersion(
            Publicacion publicacion, CargasDeLaVersion cargas, ReporteGeneral general) {
        if (!publicacion.hash().equals(cargas.hash())
                || !publicacion.hash().equals(general.publicacion().hash())) {
            throw new ContenidoInconsistente(
                    "El hash de la publicación "
                            + publicacion.hash()
                            + " no es el del contenido "
                            + cargas.hash()
                            + " o el del reporte general "
                            + general.publicacion().hash()
                            + " (periodo "
                            + publicacion.periodo()
                            + ", versión "
                            + publicacion.version()
                            + ").");
        }
        for (ContenidoDelDocente carga : cargas.cargas()) {
            if (!cargas.hash().equals(carga.hash())) {
                throw new ContenidoInconsistente(
                        "La carga de "
                                + carga.docente().identificacion()
                                + " es de otra versión: "
                                + carga.hash()
                                + ".");
            }
        }
    }

    private static void exigirLosTotales(
            Publicacion publicacion,
            Map<String, HorasPorComponente> sumas,
            ReporteGeneral general) {
        Map<String, HorasPorComponente> delGeneral = new HashMap<>();
        for (FilaDelDocente fila : general.docentes()) {
            delGeneral.put(fila.identificacion(), fila.horas());
        }
        if (!delGeneral.keySet().equals(sumas.keySet())) {
            Set<String> distintos = new TreeSet<>(delGeneral.keySet());
            distintos.addAll(sumas.keySet());
            distintos.removeIf(una -> delGeneral.containsKey(una) && sumas.containsKey(una));
            throw new ContenidoInconsistente(
                    "Los docentes del CSV para SIGA no son los del reporte general de la versión "
                            + publicacion.version()
                            + " del periodo "
                            + publicacion.periodo()
                            + ": "
                            + distintos
                            + ".");
        }
        HorasPorComponente total = HorasPorComponente.NINGUNA;
        for (Map.Entry<String, HorasPorComponente> suma : sumas.entrySet()) {
            HorasPorComponente esperadas = delGeneral.get(suma.getKey());
            for (Componente componente : Componente.values()) {
                if (suma.getValue().de(componente).compareTo(esperadas.de(componente)) != 0) {
                    throw new ContenidoInconsistente(
                            "Las filas del docente "
                                    + suma.getKey()
                                    + " suman "
                                    + suma.getValue().de(componente)
                                    + " en "
                                    + componente
                                    + ", y el reporte general dice "
                                    + esperadas.de(componente)
                                    + " (periodo "
                                    + publicacion.periodo()
                                    + ", versión "
                                    + publicacion.version()
                                    + ").");
                }
            }
            total = total.mas(suma.getValue());
        }
        for (Componente componente : Componente.values()) {
            if (total.de(componente).compareTo(general.totales().de(componente)) != 0) {
                throw new ContenidoInconsistente(
                        "Las filas suman "
                                + total.de(componente)
                                + " en "
                                + componente
                                + ", y el total institucional es "
                                + general.totales().de(componente)
                                + ".");
            }
        }
    }

    private static HorasPorComponente deUnaFila(FilaSiga fila) {
        BigDecimal cero = BigDecimal.ZERO;
        BigDecimal horas = fila.horas();
        return switch (fila.componente()) {
            case CLASE -> new HorasPorComponente(horas, cero, cero, cero, cero);
            case OTRAS_DOCENCIA -> new HorasPorComponente(cero, horas, cero, cero, cero);
            case INVESTIGACION -> new HorasPorComponente(cero, cero, horas, cero, cero);
            case VINCULACION -> new HorasPorComponente(cero, cero, cero, horas, cero);
            case GESTION -> new HorasPorComponente(cero, cero, cero, cero, horas);
        };
    }

    // Un orden total (research §5): dos CSV de las mismas filas son idénticos (FR-007). Un Collator
    // por orden: no se comparte entre hilos.
    private static Comparator<FilaSiga> orden() {
        Collator alfabetico = Collator.getInstance(ESPANOL);
        return Comparator.<FilaSiga, String>comparing(una -> una.docente().nombre(), alfabetico)
                .thenComparing(una -> una.docente().identificacion())
                .thenComparing(FilaSiga::componente)
                .thenComparing(FilaSiga::codigo)
                .thenComparing(FilaSiga::carrera)
                .thenComparing(una -> una.paralelo().orElse(""))
                .thenComparing(una -> una.proyecto().orElse(""))
                .thenComparing(FilaSiga::horas);
    }
}
