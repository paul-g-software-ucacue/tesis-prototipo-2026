package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.puertos.salida.Transaccion;
import java.util.function.Supplier;

/** Una transacción que solo dice si el trabajo corrió dentro de ella. */
final class TransaccionDePrueba implements Transaccion {

    private boolean dentro;

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        dentro = true;
        try {
            return trabajo.get();
        } finally {
            dentro = false;
        }
    }

    boolean dentro() {
        return dentro;
    }
}
