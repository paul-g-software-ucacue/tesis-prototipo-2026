package ec.mdd.reporting.dominio;

import java.text.Collator;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * El reporte general consolidado de la institución (RF-46, F20-H02, D-117): cada docente de la
 * versión publicada vigente con sus horas por componente y su total, y los totales institucionales.
 * Es un agregado de lectura: no se guarda. El PDF, el CSV y la pantalla salen de él, con el mismo
 * orden y los mismos totales (RF-48).
 *
 * <p>Los totales se suman aquí, en el dominio, y no en Generación ni en cada formato: así se
 * prueban sin infraestructura (principio VIII), el PDF y el CSV no se pueden desalinear, y son
 * siempre la suma exacta de las filas que muestra el documento. El constructor lo exige.
 *
 * @param publicacion la versión publicada de la que sale
 * @param docentes por nombre, en orden alfabético del español, y a igual nombre por identificación;
 *     sin identificaciones repetidas
 * @param totales los totales institucionales: la suma exacta de las filas, por componente
 * @param generadoPor quién lo pidió; solo para el PDF y la pantalla
 * @param generadoEn cuándo; solo para el PDF y la pantalla
 */
public record ReporteGeneral(
        Publicacion publicacion,
        List<FilaDelDocente> docentes,
        HorasPorComponente totales,
        String generadoPor,
        Instant generadoEn) {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-EC");

    /**
     * Exige los datos, ordena las filas y comprueba que los totales sean su suma.
     *
     * @throws IllegalArgumentException si los totales no son la suma de las filas
     */
    public ReporteGeneral {
        Objects.requireNonNull(publicacion, "publicacion");
        Objects.requireNonNull(totales, "totales");
        Objects.requireNonNull(generadoPor, "generadoPor");
        Objects.requireNonNull(generadoEn, "generadoEn");
        docentes = docentes.stream().sorted(porNombre()).toList();
        HorasPorComponente suma = sumar(docentes);
        for (Componente componente : Componente.values()) {
            if (suma.de(componente).compareTo(totales.de(componente)) != 0) {
                throw new IllegalArgumentException(
                        "Los totales de " + componente + " no son la suma de las filas.");
            }
        }
    }

    /**
     * Arma el reporte con el contenido congelado de la versión publicada. Comprueba que el
     * contenido sea el de esa versión, por su hash, y que ningún docente venga dos veces: si no, el
     * documento diría algo distinto de lo publicado o sumaría dos veces las horas de alguien.
     *
     * @param publicacion la versión publicada vigente
     * @param contenido los docentes de la versión, con sus horas
     * @param quien quién lo pide
     * @param momento cuándo
     * @return el reporte
     * @throws ContenidoInconsistente si el hash no coincide o un docente está repetido
     */
    public static ReporteGeneral de(
            Publicacion publicacion, DocentesDeLaVersion contenido, String quien, Instant momento) {
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
        Set<String> vistos = new HashSet<>();
        for (DocentesDeLaVersion.Docente docente : contenido.docentes()) {
            if (!vistos.add(docente.identificacion())) {
                throw new ContenidoInconsistente(
                        "El docente "
                                + docente.identificacion()
                                + " está repetido en el contenido de la versión "
                                + publicacion.version()
                                + " del periodo "
                                + publicacion.periodo()
                                + ".");
            }
        }
        List<FilaDelDocente> filas =
                contenido.docentes().stream()
                        .map(
                                una ->
                                        new FilaDelDocente(
                                                una.identificacion(),
                                                una.nombre(),
                                                una.dedicacion().orElse(null),
                                                una.horas()))
                        .toList();
        return new ReporteGeneral(publicacion, filas, sumar(filas), quien, momento);
    }

    /** Cuántos docentes incluye. */
    public int cantidadDeDocentes() {
        return docentes.size();
    }

    /**
     * El orden alfabético del español, no el de los códigos de los caracteres: «ÁLVAREZ» va antes
     * que «PERALTA». A igual nombre, por identificación. El PDF, el CSV y la pantalla lo comparten.
     */
    public static Comparator<FilaDelDocente> porNombre() {
        // Un Collator por orden: no se comparte entre hilos.
        Collator alfabetico = Collator.getInstance(ESPANOL);
        return Comparator.comparing(FilaDelDocente::nombre, alfabetico)
                .thenComparing(FilaDelDocente::identificacion);
    }

    private static HorasPorComponente sumar(List<FilaDelDocente> filas) {
        HorasPorComponente suma = HorasPorComponente.NINGUNA;
        for (FilaDelDocente fila : filas) {
            suma = suma.mas(fila.horas());
        }
        return suma;
    }
}
