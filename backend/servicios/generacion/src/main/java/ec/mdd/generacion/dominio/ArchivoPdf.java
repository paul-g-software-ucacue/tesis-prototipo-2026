package ec.mdd.generacion.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Un archivo que se comprobó que es un PDF, por su contenido y no por su nombre (RF-32). No se abre
 * ni se interpreta: mira cómo empieza y cómo termina.
 *
 * @param nombre el nombre para mostrar: nunca decide dónde se guarda
 * @param contenido sus bytes
 * @param huella su SHA-256, en hexadecimal
 */
public record ArchivoPdf(String nombre, byte[] contenido, String huella) {

    /** Los bytes que admite un informe: 10 MB. */
    public static final int TAMANO_MAXIMO = 10 * 1024 * 1024;

    private static final byte[] INICIO = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIN = "%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final int COLA = 1024;
    private static final int LARGO_DEL_NOMBRE = 150;
    private static final String SIN_NOMBRE = "informe.pdf";

    /**
     * Comprueba un archivo recibido.
     *
     * @param nombre el nombre recibido; puede traer una ruta o faltar
     * @param contenido sus bytes
     * @return el archivo comprobado
     * @throws InformeNoEsPdf si está vacío o su contenido no es de un PDF
     * @throws InformeDemasiadoGrande si pasa de 10 MB
     */
    public static ArchivoPdf de(String nombre, byte[] contenido) {
        if (contenido.length > TAMANO_MAXIMO) {
            throw new InformeDemasiadoGrande();
        }
        if (!empiezaBien(contenido) || !terminaBien(contenido)) {
            throw new InformeNoEsPdf();
        }
        return new ArchivoPdf(limpio(nombre), contenido.clone(), sha256(contenido));
    }

    /** Copia los bytes: nadie de fuera los cambia. */
    @Override
    public byte[] contenido() {
        return contenido.clone();
    }

    /** Su SHA-256, en hexadecimal. */
    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("Java siempre trae SHA-256", error);
        }
    }

    private static boolean empiezaBien(byte[] contenido) {
        return contenido.length >= INICIO.length
                && Arrays.equals(contenido, 0, INICIO.length, INICIO, 0, INICIO.length);
    }

    private static boolean terminaBien(byte[] contenido) {
        int desde = Math.max(0, contenido.length - COLA);
        for (int i = contenido.length - FIN.length; i >= desde; i--) {
            if (Arrays.equals(contenido, i, i + FIN.length, FIN, 0, FIN.length)) {
                return true;
            }
        }
        return false;
    }

    private static String limpio(String nombre) {
        if (nombre == null) {
            return SIN_NOMBRE;
        }
        String sinRuta =
                nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        String imprimible = sinRuta.replaceAll("\\p{Cntrl}", "").strip();
        if (imprimible.isEmpty()) {
            return SIN_NOMBRE;
        }
        return imprimible.substring(0, Math.min(imprimible.length(), LARGO_DEL_NOMBRE));
    }
}
