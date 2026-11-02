package ec.mdd.generacion.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * El informe del Responsable de consolidación que acompaña al consolidado (RF-32).
 *
 * @param nombre el nombre recibido, solo para mostrarlo
 * @param tamano sus bytes
 * @param huella su SHA-256, en hexadecimal
 * @param clave dónde está en el almacén de archivos
 * @param por quién lo adjuntó
 * @param en cuándo
 */
public record InformeSustentado(
        String nombre, int tamano, String huella, String clave, String por, Instant en) {

    /** Exige los datos. */
    public InformeSustentado {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(huella, "huella");
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(por, "por");
        Objects.requireNonNull(en, "en");
    }
}
