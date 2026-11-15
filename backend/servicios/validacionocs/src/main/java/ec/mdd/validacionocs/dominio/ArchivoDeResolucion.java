package ec.mdd.validacionocs.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * El PDF de la resolución del OCS, comprobado al recibirlo (F15-H01, D-107), como el informe
 * sustentado de Generación (F10-H02): empieza con {@code %PDF-}, termina con {@code %%EOF} en su
 * último KB y pesa hasta 10 MB. No se abre ni se validan sus firmas (D-19). Validación OCS tiene su
 * propia comprobación: no usa clases de Generación (principio V).
 *
 * @param nombre el nombre recibido, sin ruta, de hasta 150 caracteres
 * @param contenido sus bytes
 * @param sha256 su huella, en hexadecimal
 * @param tamano sus bytes
 */
public record ArchivoDeResolucion(String nombre, byte[] contenido, String sha256, int tamano) {

    /** Los bytes que admite una resolución: 10 MB. */
    public static final int TAMANO_MAXIMO = 10 * 1024 * 1024;

    private static final byte[] INICIO = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIN = "%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final int COLA = 1024;
    private static final int LARGO_DEL_NOMBRE = 150;
    private static final String SIN_NOMBRE = "resolucion.pdf";

    /**
     * Comprueba un archivo recibido.
     *
     * @param nombre el nombre recibido; puede traer una ruta o faltar
     * @param contenido sus bytes
     * @return el archivo comprobado
     * @throws ResolucionNoEsPdf si está vacío o su contenido no es de un PDF
     * @throws ResolucionDemasiadoGrande si pasa de 10 MB
     */
    public static ArchivoDeResolucion de(String nombre, byte[] contenido) {
        if (contenido.length > TAMANO_MAXIMO) {
            throw new ResolucionDemasiadoGrande();
        }
        if (!empiezaBien(contenido) || !terminaBien(contenido)) {
            throw new ResolucionNoEsPdf();
        }
        return new ArchivoDeResolucion(
                limpio(nombre), contenido.clone(), sha256(contenido), contenido.length);
    }

    /** Copia los bytes: nadie de fuera los cambia. */
    @Override
    public byte[] contenido() {
        return contenido.clone();
    }

    /**
     * Su SHA-256, en hexadecimal.
     *
     * @param bytes el contenido
     * @return la huella
     */
    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("La JVM no trae SHA-256", error);
        }
    }

    private static boolean empiezaBien(byte[] contenido) {
        return contenido.length >= INICIO.length
                && Arrays.equals(contenido, 0, INICIO.length, INICIO, 0, INICIO.length);
    }

    // %%EOF en el último KB: algunos generadores agregan saltos o espacios después.
    private static boolean terminaBien(byte[] contenido) {
        int desde = Math.max(0, contenido.length - COLA);
        for (int i = contenido.length - FIN.length; i >= desde; i--) {
            if (Arrays.equals(contenido, i, i + FIN.length, FIN, 0, FIN.length)) {
                return true;
            }
        }
        return false;
    }

    // Sin ruta (ni de Windows ni de Unix) y sin pasar del largo; si no queda nada, uno por defecto.
    private static String limpio(String nombre) {
        if (nombre == null) {
            return SIN_NOMBRE;
        }
        String base =
                nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        base = base.strip();
        if (base.isEmpty()) {
            return SIN_NOMBRE;
        }
        return base.length() > LARGO_DEL_NOMBRE ? base.substring(0, LARGO_DEL_NOMBRE) : base;
    }
}
