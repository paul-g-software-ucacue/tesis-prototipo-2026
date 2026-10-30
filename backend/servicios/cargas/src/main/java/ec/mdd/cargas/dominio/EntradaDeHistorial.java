package ec.mdd.cargas.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un intento de confirmar una carga (RF-10, FR-008), sin el contenido del archivo (principio IX), o
 * una confirmación «sin proyectos» de un periodo (F01-H05), que no tiene archivo ni hash.
 *
 * @param carga el código de la carga
 * @param usuario quien confirmó
 * @param momento cuándo
 * @param archivo el nombre del archivo, sin ruta; {@code null} solo en «sin proyectos»
 * @param hash su SHA-256, en hexadecimal; {@code null} solo en «sin proyectos»
 * @param resultado cómo terminó
 * @param nuevos cuántos registró el dueño
 * @param cambiados cuántos corrigió
 * @param bajasSinAplicar cuántos se daban de baja y quedaron como estaban
 * @param motivo por qué se rechazó; vacío si no se rechazó
 * @param periodo el periodo confirmado «sin proyectos»; vacío en los demás
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
        Optional<MotivoDeRechazo> motivo,
        Optional<String> periodo) {

    /**
     * Valida que solo un rechazo tenga motivo, y que todo rechazo lo tenga; y que solo «sin
     * proyectos» tenga periodo, y no tenga archivo ni hash.
     */
    public EntradaDeHistorial {
        Objects.requireNonNull(carga, "carga");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(periodo, "periodo");
        boolean sinProyectos = resultado == ResultadoDeHistorial.SIN_PROYECTOS;
        if (sinProyectos != periodo.isPresent()
                || sinProyectos != (archivo == null)
                || sinProyectos != (hash == null)) {
            throw new IllegalArgumentException(
                    "Solo «sin proyectos» tiene periodo, y solo ella no tiene archivo ni hash");
        }
        if ((resultado == ResultadoDeHistorial.RECHAZADA) != motivo.isPresent()) {
            throw new IllegalArgumentException(
                    "Solo un rechazo tiene motivo, y todo rechazo lo tiene");
        }
        if (nuevos < 0 || cambiados < 0 || bajasSinAplicar < 0) {
            throw new IllegalArgumentException("Los totales no pueden ser negativos");
        }
    }

    /** Un intento de confirmar un archivo, sin periodo. */
    public EntradaDeHistorial(
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
        this(
                carga,
                usuario,
                momento,
                Objects.requireNonNull(archivo, "archivo"),
                Objects.requireNonNull(hash, "hash"),
                resultado,
                nuevos,
                cambiados,
                bajasSinAplicar,
                motivo,
                Optional.empty());
    }

    /** La confirmación «sin proyectos» de un periodo en la carga de proyectos (F01-H05). */
    public static EntradaDeHistorial sinProyectos(
            String carga, String periodo, String usuario, Instant momento) {
        return new EntradaDeHistorial(
                carga,
                usuario,
                momento,
                null,
                null,
                ResultadoDeHistorial.SIN_PROYECTOS,
                0,
                0,
                0,
                Optional.empty(),
                Optional.of(periodo));
    }
}
