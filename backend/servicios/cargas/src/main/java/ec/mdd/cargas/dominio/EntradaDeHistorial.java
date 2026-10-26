package ec.mdd.cargas.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un intento de confirmar una carga (RF-10, FR-008), sin el contenido del archivo (principio IX).
 *
 * @param carga el código de la carga
 * @param usuario quien confirmó
 * @param momento cuándo
 * @param archivo el nombre del archivo, sin ruta
 * @param hash su SHA-256, en hexadecimal
 * @param resultado cómo terminó
 * @param nuevos cuántos registró el dueño
 * @param cambiados cuántos corrigió
 * @param bajasSinAplicar cuántos se daban de baja y quedaron como estaban
 * @param motivo por qué se rechazó; vacío si no se rechazó
 */
public record EntradaDeHistorial(
        String carga,
        String usuario,
        Instant momento,
        String archivo,
        String hash,
        ResultadoDeHistorial resultado,
        int nuevos,
        int cambiados,
        int bajasSinAplicar,
        Optional<MotivoDeRechazo> motivo) {

    /** Valida que solo un rechazo tenga motivo, y que todo rechazo lo tenga. */
    public EntradaDeHistorial {
        Objects.requireNonNull(carga, "carga");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(archivo, "archivo");
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(motivo, "motivo");
        if ((resultado == ResultadoDeHistorial.RECHAZADA) != motivo.isPresent()) {
            throw new IllegalArgumentException(
                    "Solo un rechazo tiene motivo, y todo rechazo lo tiene");
        }
        if (nuevos < 0 || cambiados < 0 || bajasSinAplicar < 0) {
            throw new IllegalArgumentException("Los totales no pueden ser negativos");
        }
    }
}
