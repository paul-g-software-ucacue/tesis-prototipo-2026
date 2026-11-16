package ec.mdd.validacionocs.puertos.salida;

/**
 * Los PDF de las resoluciones del OCS, en el volumen de archivos del proceso (F15-H01, D-88,
 * D-107), nombrados por su huella. Nunca se reescriben ni se borran.
 */
public interface AlmacenDeResoluciones {

    /**
     * Guarda el PDF. Es idempotente: si ya está, no lo reescribe.
     *
     * @param sha256 su huella, en hexadecimal
     * @param contenido sus bytes
     * @throws IllegalArgumentException si la huella no es un SHA-256 en hexadecimal
     */
    void guardar(String sha256, byte[] contenido);

    /**
     * Lee el PDF y comprueba que coincide con su huella.
     *
     * @param sha256 su huella
     * @return sus bytes
     * @throws ec.mdd.validacionocs.puertos.entrada.ResolucionIlegible si falta o no coincide
     * @throws IllegalArgumentException si la huella no es un SHA-256 en hexadecimal
     */
    byte[] leer(String sha256);
}
