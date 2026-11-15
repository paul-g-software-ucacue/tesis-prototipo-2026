package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;

/** Esqueleto (F14-H03, T013). */
public final class CompletarFirmasServicio implements CompletarFirmas {

    public CompletarFirmasServicio(
            RepositorioProceso procesos, RegistroDeCambios registro, Transaccion transaccion) {}

    @Override
    public void completar(String periodo, int version, Instant momento) {
        throw new UnsupportedOperationException();
    }
}
