package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import java.time.Instant;

/**
 * El resultado de confirmar una carga (RF-10).
 *
 * @param resultado {@code APLICADA}, o {@code REPETIDA} si el archivo ya se había aplicado
 * @param nuevos cuántos registró el dueño
 * @param cambiados cuántos corrigió
 * @param bajasSinAplicar cuántos se daban de baja y quedaron como estaban
 * @param confirmadaEl cuándo se aplicó; en una repetida, la primera vez
 */
public record Confirmacion(
        ResultadoDeHistorial resultado,
        int nuevos,
        int cambiados,
        int bajasSinAplicar,
        Instant confirmadaEl) {}
