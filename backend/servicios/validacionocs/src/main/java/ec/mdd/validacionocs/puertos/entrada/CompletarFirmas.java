package ec.mdd.validacionocs.puertos.entrada;

import java.time.Instant;

/** Esqueleto (F14-H03, T013). */
public interface CompletarFirmas {

    void completar(String periodo, int version, Instant momento);
}
