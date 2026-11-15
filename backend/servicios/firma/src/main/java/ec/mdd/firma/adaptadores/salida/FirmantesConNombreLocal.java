package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import java.util.Map;
import java.util.Set;

/** Esqueleto (F14-H03, T015). */
public final class FirmantesConNombreLocal implements FirmantesDelPeriodo {

    public FirmantesConNombreLocal(FirmantesDelPeriodo delegado, String nombreLocal) {}

    @Override
    public Map<String, FirmanteDelPeriodo> firmantes(String periodo, Set<String> roles) {
        throw new UnsupportedOperationException();
    }
}
