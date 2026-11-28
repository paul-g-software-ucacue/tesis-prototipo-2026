package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Una Exportación a SIGA (RF-53, F19-H02, D-120): el registro de un CSV para SIGA entregado, con su
 * suma de verificación. No se edita ni se borra (principio VII).
 *
 * <p>El archivo no se guarda: con la versión publicada, que no cambia, y la versión del layout de
 * exportación, que se conserva, se vuelve a generar idéntico, con la misma suma.
 *
 * @param id el identificador del registro
 * @param usuario quién lo exportó
 * @param momento cuándo
 * @param periodo el código del periodo
 * @param version la versión publicada de la que salió
 * @param hashVersion su hash, el de la publicación
 * @param layout la versión del layout de exportación; 0 es el layout por defecto
 * @param nombre el nombre del archivo
 * @param filas las filas del archivo, sin el encabezado
 * @param suma el SHA-256 de los bytes entregados
 */
public record ExportacionSiga(
        UUID id,
        String usuario,
        Instant momento,
        String periodo,
        int version,
        String hashVersion,
        int layout,
        String nombre,
        int filas,
        SumaDeVerificacion suma) {

    private static final Pattern PERIODO = Pattern.compile("^[A-Za-z0-9._-]{1,20}$");
    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");
    private static final int MAXIMO_USUARIO = 64;
    private static final int MAXIMO_NOMBRE = 80;

    /** Exige los datos y sus reglas (data-model.md). */
    public ExportacionSiga {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hashVersion, "hashVersion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(suma, "suma");
        if (usuario.isBlank() || usuario.length() > MAXIMO_USUARIO) {
            throw new IllegalArgumentException("usuario vacío o de más de 64 caracteres");
        }
        if (!PERIODO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("periodo con caracteres o largo no admitidos");
        }
        if (version < 1 || layout < 0 || filas < 0) {
            throw new IllegalArgumentException("versión desde 1, layout y filas desde 0");
        }
        if (!HASH.matcher(hashVersion).matches()) {
            throw new IllegalArgumentException("hash que no es SHA-256 en hexadecimal");
        }
        if (nombre.isBlank() || nombre.length() > MAXIMO_NOMBRE) {
            throw new IllegalArgumentException("nombre vacío o de más de 80 caracteres");
        }
    }

    /**
     * @param publicacion la versión publicada de la que salió
     * @param layout la versión del layout de exportación con que salió
     * @param archivo el archivo entregado
     * @param filas sus filas, sin el encabezado
     * @param quien quién lo exportó
     * @param momento cuándo
     * @return el registro, con la suma de los bytes del archivo
     */
    public static ExportacionSiga de(
            Publicacion publicacion,
            int layout,
            ArchivoSiga archivo,
            int filas,
            String quien,
            Instant momento) {
        return new ExportacionSiga(
                UUID.randomUUID(),
                quien,
                momento,
                publicacion.periodo(),
                publicacion.version(),
                publicacion.hash(),
                layout,
                archivo.nombre(),
                filas,
                SumaDeVerificacion.de(archivo.contenido()));
    }
}
