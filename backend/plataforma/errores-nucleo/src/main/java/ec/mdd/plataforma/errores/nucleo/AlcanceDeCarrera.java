package ec.mdd.plataforma.errores.nucleo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Qué carreras ve un usuario sobre la oferta y lo que depende de ella (D-64, RF-03). Decide con los
 * nombres de sus autoridades, que {@code AutoridadesDelToken} arma en cada proceso: los servicios
 * no conocen el token.
 *
 * <p>Mira los tres permisos que dan acceso a la oferta (D-70), también para la sugerencia y la
 * afinidad: esas se abren con «Docentes: lectura», que el coordinador tiene sobre todo. Con alguno
 * de los tres sobre todo, ve todas. Con todos los suyos solo sobre lo propio, solo su carrera. Sin
 * ninguno, como Talento Humano, no es un coordinador y ve todas.
 */
public final class AlcanceDeCarrera {

    /** Prefijo de la autoridad de un permiso con alcance «lo propio». */
    public static final String PREFIJO_PROPIO = "ALCANCE_PROPIO_";

    /** Prefijo de la autoridad con la carrera del usuario. */
    public static final String PREFIJO_CARRERA = "CARRERA_";

    private static final String PREFIJO_PERMISO = "PERMISO_";

    private static final List<String> PERMISOS_DE_LA_OFERTA =
            List.of("OFERTA:LECTURA", "DISTRIBUTIVO:LECTURA", "CATALOGOS:LECTURA");

    private AlcanceDeCarrera() {}

    /**
     * La carrera a la que se limita el usuario.
     *
     * @param autoridades los nombres de sus autoridades
     * @return vacío si ve todas; su carrera si solo ve lo propio
     * @throws CarreraSinIndicar si solo ve lo propio y no tiene carrera
     */
    public static Optional<String> propia(Collection<String> autoridades) {
        Set<String> suyas = Set.copyOf(autoridades);
        List<String> deLaOferta =
                PERMISOS_DE_LA_OFERTA.stream()
                        .filter(permiso -> suyas.contains(PREFIJO_PERMISO + permiso))
                        .toList();
        boolean soloLoPropio =
                !deLaOferta.isEmpty()
                        && deLaOferta.stream()
                                .allMatch(permiso -> suyas.contains(PREFIJO_PROPIO + permiso));
        if (!soloLoPropio) {
            return Optional.empty();
        }
        return Optional.of(
                suyas.stream()
                        .filter(autoridad -> autoridad.startsWith(PREFIJO_CARRERA))
                        .map(autoridad -> autoridad.substring(PREFIJO_CARRERA.length()))
                        .findFirst()
                        .orElseThrow(CarreraSinIndicar::new));
    }
}
