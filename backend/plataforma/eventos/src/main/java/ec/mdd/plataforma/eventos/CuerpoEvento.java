package ec.mdd.plataforma.eventos;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que viaja por HTTP al receptor: el sobre sin el destino, con la forma de {@code
 * EventoEntreProcesos} en {@code contratos/comun/eventos.yaml}.
 */
record CuerpoEvento(
        UUID idEvento, String tipo, String origen, Instant momento, Map<String, Object> contenido) {

    static CuerpoEvento de(EventoEntreProcesos evento) {
        return new CuerpoEvento(
                evento.idEvento(),
                evento.tipo(),
                evento.origen(),
                evento.momento(),
                evento.contenido());
    }

    @Override
    public String toString() {
        return "CuerpoEvento[idEvento=" + idEvento + ", tipo=" + tipo + "]";
    }
}
