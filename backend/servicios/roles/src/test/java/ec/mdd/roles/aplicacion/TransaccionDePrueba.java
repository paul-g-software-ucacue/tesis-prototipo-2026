package ec.mdd.roles.aplicacion;

import ec.mdd.roles.puertos.salida.Transaccion;
import java.util.function.Supplier;

/** Ejecuta el trabajo y recuerda si está dentro, para comprobar qué va en la transacción. */
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

    /** Si el trabajo está corriendo ahora. */
    boolean dentro() {
        return dentro;
    }
}
