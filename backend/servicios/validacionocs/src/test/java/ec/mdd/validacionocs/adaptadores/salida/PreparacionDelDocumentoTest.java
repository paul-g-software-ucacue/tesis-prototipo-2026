package ec.mdd.validacionocs.adaptadores.salida;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ec.mdd.firma.puertos.entrada.PasoDeLaCadena;
import ec.mdd.firma.puertos.entrada.PrepararDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.SolicitudDeDocumento;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Aprobación le entrega la cadena a Firma al entrar en firma (MD-05, F13-H02). */
class PreparacionDelDocumentoTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-11T15:00:00Z");

    private final PrepararDocumentoFirmable firma = mock(PrepararDocumentoFirmable.class);
    private final PreparacionDelDocumento oyente = new PreparacionDelDocumento(firma);

    @Test
    void dadoElAvisoDeFirma_cuandoLlega_entoncesLeEntregaLaCadenaAFirma() {
        oyente.alEntrarEnFirma(
                new AvisoDeFirma.DistributivoEnFirma(
                        "2026-I",
                        2,
                        "e".repeat(64),
                        List.of(
                                new AvisoDeFirma.Paso(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                                new AvisoDeFirma.Paso(2, "Revisado", "VICERRECTOR")),
                        "RESPONSABLE_CONSOLIDACION",
                        MOMENTO));

        verify(firma)
                .preparar(
                        new SolicitudDeDocumento(
                                "2026-I",
                                2,
                                "e".repeat(64),
                                List.of(
                                        new PasoDeLaCadena(
                                                1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                                        new PasoDeLaCadena(2, "Revisado", "VICERRECTOR")),
                                "RESPONSABLE_CONSOLIDACION",
                                MOMENTO));
    }
}
