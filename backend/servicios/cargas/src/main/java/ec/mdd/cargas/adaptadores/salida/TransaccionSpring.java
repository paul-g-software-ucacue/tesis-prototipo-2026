package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.puertos.salida.Transaccion;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionOperations;

/** La transacción de la base con el {@link TransactionOperations} del proceso. */
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
