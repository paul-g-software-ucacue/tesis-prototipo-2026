package ec.mdd.reporting.dominio;

import java.util.Objects;

/**
 * El CSV para SIGA listo para entregar (F19-H01, D-119).
 *
 * @param contenido sus bytes
 * @param nombre el nombre del archivo, {@code siga-<periodo>-v<version>.csv}
 */
public record ArchivoSiga(byte[] contenido, String nombre) {

    /** Exige los datos y copia los bytes. */
    public ArchivoSiga {
        Objects.requireNonNull(nombre, "nombre");
        contenido = contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido.clone();
    }

    /**
     * @param publicacion la versión publicada de la que sale
     * @return el nombre del archivo de esa versión
     */
    public static String nombreDe(Publicacion publicacion) {
        return "siga-" + publicacion.periodo() + "-v" + publicacion.version() + ".csv";
    }
}
