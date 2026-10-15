package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.puertos.entrada.EmitirTokenDeProceso;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Token de proceso para entregar eventos entre procesos (D-49, ADR-018).
 *
 * <p>La clave de cada proceso es un secreto aleatorio del entorno, no la contraseña de una persona:
 * basta con compararla en tiempo constante, sin Argon2 (research §6). Las dos claves pasan antes
 * por SHA-256, para que la comparación no dependa del largo de la clave recibida. Un proceso sin
 * clave, o con una clave vacía, también se compara (contra una huella de relleno) y termina en
 * {@link CredencialesInvalidas}, igual que una clave incorrecta.
 */
public final class EmitirTokenDeProcesoServicio implements EmitirTokenDeProceso {

    private static final byte[] RELLENO = huella("proceso-sin-clave-configurada");

    private final Map<String, byte[]> huellas;
    private final EmisorDeTokens emisor;

    /**
     * @param clavesPorProceso clave configurada de cada proceso; las vacías no se habilitan
     * @param emisor firma el token
     */
    public EmitirTokenDeProcesoServicio(
            Map<String, String> clavesPorProceso, EmisorDeTokens emisor) {
        Map<String, byte[]> habilitados = new TreeMap<>();
        clavesPorProceso.forEach(
                (proceso, clave) -> {
                    if (clave != null && !clave.isBlank()) {
                        habilitados.put(proceso, huella(clave));
                    }
                });
        this.huellas = Map.copyOf(habilitados);
        this.emisor = emisor;
    }

    @Override
    public TokenEmitido emitir(String proceso, String clave) {
        byte[] esperada = proceso == null ? null : huellas.get(proceso);
        boolean coincide =
                MessageDigest.isEqual(
                        esperada == null ? RELLENO : esperada, huella(clave == null ? "" : clave));
        if (esperada == null || !coincide) {
            throw new CredencialesInvalidas();
        }
        return emisor.emitirParaProceso(proceso);
    }

    /** Los procesos que pueden pedir un token: los que tienen una clave no vacía. */
    public Set<String> procesosHabilitados() {
        return new java.util.TreeSet<>(huellas.keySet());
    }

    // SHA-256 de la clave, para comparar en tiempo constante sin depender de su largo.
    static byte[] huella(String clave) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(clave.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
