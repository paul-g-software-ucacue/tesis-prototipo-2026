package ec.mdd.plataforma.cargas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * El aplicador de un dueño armado con sus traductores y su transacción: cada dueño lo crea en su
 * configuración, sin repetir el reparto por carga.
 */
public final class AplicadorPorTraductores implements AplicadorDeCargas {

    private final Map<String, TraductorDeCarga<?>> traductores = new LinkedHashMap<>();
    private final Function<Supplier<ResultadoDelLote>, ResultadoDelLote> enTransaccion;

    /**
     * @param traductores uno por carga, sin repetir
     * @param enTransaccion ejecuta el trabajo en una transacción del dueño
     * @throws IllegalArgumentException si dos traductores son de la misma carga
     */
    public AplicadorPorTraductores(
            List<? extends TraductorDeCarga<?>> traductores,
            Function<Supplier<ResultadoDelLote>, ResultadoDelLote> enTransaccion) {
        for (TraductorDeCarga<?> traductor : traductores) {
            if (this.traductores.putIfAbsent(traductor.carga(), traductor) != null) {
                throw new IllegalArgumentException(
                        "La carga " + traductor.carga() + " tiene dos traductores");
            }
        }
        this.enTransaccion = enTransaccion;
    }

    @Override
    public Set<String> cargas() {
        return Set.copyOf(traductores.keySet());
    }

    @Override
    public boolean tieneDatos(String carga) {
        return traductor(carga).tieneDatos();
    }

    @Override
    public boolean tieneDatosEn(String carga, String periodo) {
        return traductor(carga).tieneDatosEn(periodo);
    }

    @Override
    public VistaPreviaDelLote previsualizar(LoteDeCarga lote) {
        return MotorDelLote.previsualizar(traductor(lote), lote);
    }

    @Override
    public ResultadoDelLote aplicar(LoteDeCarga lote, String huella, String quien) {
        TraductorDeCarga<?> traductor = traductor(lote);
        return enTransaccion.apply(() -> MotorDelLote.aplicar(traductor, lote, huella, quien));
    }

    private TraductorDeCarga<?> traductor(LoteDeCarga lote) {
        return traductor(lote.carga());
    }

    private TraductorDeCarga<?> traductor(String carga) {
        TraductorDeCarga<?> traductor = traductores.get(carga);
        if (traductor == null) {
            throw new IllegalArgumentException("Este dueño no atiende la carga " + carga);
        }
        return traductor;
    }
}
