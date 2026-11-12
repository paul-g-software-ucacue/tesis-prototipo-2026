package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.Transaccion;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionOperations;

/** La transacción de Firma sobre la plantilla de transacciones del proceso (F13-H02). */
public final class TransaccionSpring implements Transaccion {

    private final TransactionOperations transacciones;

    /** Transacción sobre la plantilla del proceso. */
    public TransaccionSpring(TransactionOperations transacciones) {
        this.transacciones = transacciones;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return transacciones.execute(estado -> trabajo.get());
    }
}
