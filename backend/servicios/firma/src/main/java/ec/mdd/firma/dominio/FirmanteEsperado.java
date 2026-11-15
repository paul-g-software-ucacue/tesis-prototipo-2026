package ec.mdd.firma.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Quién debe firmar un paso del documento firmable (F14-H01, D-102): la persona cuyo nombre imprime
 * el PDF, fijada al generarlo. El Responsable de consolidación es una designación de un usuario
 * (ADR-003), así que se reconoce por su usuario; el Vicerrector y el Rector son el docente del
 * cargo vigente, así que se reconocen por su identificación. Tener el rol del paso no basta.
 *
 * <p>Se fija al generar, y no se calcula al consultar, para que firme quien figura en el PDF: si
 * después cambian la designación o el cargo, el turno no pasa a alguien cuyo nombre no está
 * impreso. Rehacer el documento con otros firmantes es de F12-H04.
 *
 * @param orden el orden del paso, desde 1
 * @param rol el rol del paso en la cadena
 * @param usuario el usuario esperado, o {@code null} si se reconoce por la identificación
 * @param identificacion la identificación esperada, o {@code null} si se reconoce por el usuario
 * @param nombre el nombre que el PDF imprime en el paso, de hasta 200 caracteres; con él se compara
 *     el CN de la firma (F14-H03, D-104). Vacío en los documentos generados antes
 */
public record FirmanteEsperado(
        int orden, String rol, String usuario, String identificacion, Optional<String> nombre) {

    // El formato de Docentes: cédula de 10 dígitos o pasaporte de hasta 13 letras o dígitos.
    private static final Pattern IDENTIFICACION = Pattern.compile("^[0-9A-Z]{1,13}$");

    /** Un firmante sin nombre impreso guardado, como los de los documentos anteriores a F14-H03. */
    public FirmanteEsperado(int orden, String rol, String usuario, String identificacion) {
        this(orden, rol, usuario, identificacion, Optional.empty());
    }

    /** Exige el orden, el rol y exactamente una de las dos personas. */
    public FirmanteEsperado {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden empieza en 1");
        }
        Objects.requireNonNull(rol, "rol");
        if ((usuario == null) == (identificacion == null)) {
            throw new IllegalArgumentException("Un firmante es un usuario o una identificación");
        }
        if (usuario != null) {
            usuario = usuario.strip().toLowerCase(Locale.ROOT);
        }
        if (identificacion != null && !IDENTIFICACION.matcher(identificacion).matches()) {
            throw new IllegalArgumentException("La identificación no tiene el formato de Docentes");
        }
        Objects.requireNonNull(nombre, "nombre");
        if (nombre.filter(texto -> texto.isBlank() || texto.length() > 200).isPresent()) {
            throw new IllegalArgumentException("El nombre impreso tiene de 1 a 200 caracteres");
        }
    }

    /** El firmante que se reconoce por su usuario: el Responsable designado. */
    public static FirmanteEsperado porUsuario(int orden, String rol, String usuario) {
        return new FirmanteEsperado(orden, rol, usuario, null);
    }

    /** El firmante que se reconoce por su identificación: el docente de un cargo. */
    public static FirmanteEsperado porIdentificacion(int orden, String rol, String identificacion) {
        return new FirmanteEsperado(orden, rol, null, identificacion);
    }

    /** El Responsable designado, con el nombre que imprime el PDF (F14-H03). */
    public static FirmanteEsperado porUsuario(
            int orden, String rol, String usuario, String nombre) {
        return new FirmanteEsperado(orden, rol, usuario, null, Optional.of(nombre));
    }

    /** El docente de un cargo, con el nombre que imprime el PDF (F14-H03). */
    public static FirmanteEsperado porIdentificacion(
            int orden, String rol, String identificacion, String nombre) {
        return new FirmanteEsperado(orden, rol, null, identificacion, Optional.of(nombre));
    }

    /** Si para reconocerlo hace falta la identificación de quien pide. */
    public boolean porIdentificacion() {
        return identificacion != null;
    }

    /**
     * Si quien pide es este firmante. El rol no cuenta: solo la persona (FR-007).
     *
     * @param usuarioDelSolicitante su usuario, del token
     * @param identificacionDelSolicitante su identificación, si la cuenta la tiene
     * @return verdadero si es la persona esperada
     */
    public boolean reconoce(
            String usuarioDelSolicitante, Optional<String> identificacionDelSolicitante) {
        if (usuario != null) {
            return usuario.equals(usuarioDelSolicitante.strip().toLowerCase(Locale.ROOT));
        }
        return identificacionDelSolicitante.filter(identificacion::equals).isPresent();
    }
}
