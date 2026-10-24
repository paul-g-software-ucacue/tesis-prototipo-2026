package ec.mdd.cargas.puertos.entrada;

/**
 * El archivo que se sube a una carga. No se guarda: se lee, se valida y se descarta (F01-H02,
 * aclaración 3).
 *
 * @param nombre el nombre del archivo, sin ruta y hasta 255 caracteres
 * @param contenido sus bytes
 */
public record ArchivoSubido(String nombre, byte[] contenido) {

    /** Lo más largo que se devuelve del nombre. */
    public static final int LARGO_MAXIMO = 255;

    /** Quita la ruta del nombre y lo recorta. */
    public ArchivoSubido {
        nombre = sinRuta(nombre == null ? "" : nombre);
    }

    /**
     * El nombre sin la ruta que pueda mandar el navegador y recortado a {@value #LARGO_MAXIMO}
     * caracteres, conservando la extensión.
     */
    public static String sinRuta(String nombre) {
        String solo =
                nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        if (solo.length() <= LARGO_MAXIMO) {
            return solo;
        }
        int punto = solo.lastIndexOf('.');
        String extension = punto > 0 && solo.length() - punto <= 10 ? solo.substring(punto) : "";
        return solo.substring(0, LARGO_MAXIMO - extension.length()) + extension;
    }
}
