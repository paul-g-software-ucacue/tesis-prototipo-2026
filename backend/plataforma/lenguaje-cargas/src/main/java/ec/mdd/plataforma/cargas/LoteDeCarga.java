package ec.mdd.plataforma.cargas;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * El archivo de una carga que Cargas envía a su servicio dueño (MD-13): la carga, el hash del
 * archivo y sus filas, en el orden del archivo.
 *
 * @param carga el código de la carga, como {@code carreras}
 * @param hash el SHA-256 del archivo, en hexadecimal: la clave de idempotencia
 * @param filas sus filas, sin las vacías
 */
public record LoteDeCarga(String carga, String hash, List<FilaDelLote> filas) {

    private static final Pattern HEXADECIMAL = Pattern.compile("[0-9a-f]{64}");

    /** Valida el hash y copia las filas. */
    public LoteDeCarga {
        Objects.requireNonNull(carga, "carga");
        if (hash == null || !HEXADECIMAL.matcher(hash).matches()) {
            throw new IllegalArgumentException("El hash es un SHA-256 en hexadecimal");
        }
        filas = List.copyOf(filas);
    }
}
