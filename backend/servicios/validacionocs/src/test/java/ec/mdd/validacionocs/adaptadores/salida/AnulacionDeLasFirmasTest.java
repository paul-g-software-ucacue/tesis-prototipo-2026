package ec.mdd.validacionocs.adaptadores.salida;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ec.mdd.firma.puertos.entrada.AnularFirmas;
import ec.mdd.firma.puertos.entrada.MotivoDeAnulacion;
import ec.mdd.validacionocs.puertos.salida.AvisoDeAnulacion.FirmasPorAnular;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** La orden de anular las firmas llega a Firma (F12-H04, D-106, contracts/validacionocs.md). */
class AnulacionDeLasFirmasTest {

    private static final Instant RECHAZADA = Instant.parse("2026-11-15T16:06:00Z");

    private final AnularFirmas firma = mock(AnularFirmas.class);
    private final AnulacionDeLasFirmas oyente = new AnulacionDeLasFirmas(firma);

    @Test
    void dadaLaOrdenDeAnular_cuandoLlega_entoncesFirmaAnulaLasFirmasDeEsaVersion() {
        oyente.alPedirseLaAnulacion(new FirmasPorAnular("2026-I", 1, RECHAZADA));

        verify(firma).anular("2026-I", 1, MotivoDeAnulacion.RECHAZO_EN_FIRMA, RECHAZADA);
    }
}
