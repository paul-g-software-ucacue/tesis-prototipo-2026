package ec.mdd.firma.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.AnulacionDeFirmas;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.puertos.entrada.FirmasAnuladas;
import ec.mdd.firma.puertos.entrada.MotivoDeAnulacion;
import ec.mdd.firma.puertos.salida.AvisoDeFirmasAnuladas;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Anular las firmas por orden de Aprobación (F12-H04, D-106, contracts/firma.md). */
class AnularFirmasServicioTest {

    private static final Instant RECHAZADA = Instant.parse("2026-11-15T16:06:00Z");
    private static final DocumentoFirmable DOCUMENTO =
            new DocumentoFirmable(
                    UUID.fromString("1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f"),
                    "2026-I",
                    1,
                    "CONSOLIDADO",
                    "a".repeat(64),
                    "b".repeat(64),
                    "documentos/" + "b".repeat(64) + ".pdf",
                    2048,
                    "sistema",
                    RECHAZADA.minusSeconds(3600));

    private final RepositorioDocumentos documentos = mock(RepositorioDocumentos.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AvisoDeFirmasAnuladas aviso = mock(AvisoDeFirmasAnuladas.class);
    private boolean dentro;
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    dentro = true;
                    try {
                        return trabajo.get();
                    } finally {
                        dentro = false;
                    }
                }
            };

    private final AnularFirmasServicio servicio =
            new AnularFirmasServicio(documentos, registro, transaccion, aviso);

    @Test
    void dadoElDocumentoDeLaVersion_cuandoSeAnula_entoncesRegistraAnotaYAvisaEnLaTransaccion() {
        when(documentos.documento("2026-I", 1)).thenReturn(Optional.of(DOCUMENTO));
        when(documentos.registrarAnulacion(DOCUMENTO, "RECHAZO_EN_FIRMA", "sistema", RECHAZADA))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(
                                    new AnulacionDeFirmas(
                                            "RECHAZO_EN_FIRMA", 2, "sistema", RECHAZADA));
                        });
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).as("el aviso, con la anulación").isTrue();
                            return null;
                        })
                .when(aviso)
                .avisar(any());

        servicio.anular("2026-I", 1, MotivoDeAnulacion.RECHAZO_EN_FIRMA, RECHAZADA);

        verify(aviso)
                .avisar(
                        new FirmasAnuladas(
                                "2026-I", 1, 2, MotivoDeAnulacion.RECHAZO_EN_FIRMA, RECHAZADA));
        ArgumentCaptor<CambioDeFirma> cambio = ArgumentCaptor.forClass(CambioDeFirma.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioDeFirma.FIRMAS_ANULADAS);
        assertThat(cambio.getValue().usuario()).isEqualTo("sistema");
        assertThat(cambio.getValue().momento()).isEqualTo(RECHAZADA);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1);
        assertThat(cambio.getValue().nuevo())
                .containsEntry("firmasAnuladas", 2)
                .containsEntry("motivo", "RECHAZO_EN_FIRMA");
    }

    @Test
    void dadasLasFirmasYaAnuladas_cuandoSeAnulaOtraVez_entoncesNoAnotaNiAvisa() {
        when(documentos.documento("2026-I", 1)).thenReturn(Optional.of(DOCUMENTO));
        when(documentos.registrarAnulacion(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        servicio.anular("2026-I", 1, MotivoDeAnulacion.RECHAZO_EN_FIRMA, RECHAZADA);

        verify(registro, never()).registrar(any());
        verify(aviso, never()).avisar(any());
    }

    @Test
    void dadaUnaVersionSinDocumento_cuandoSeAnula_entoncesNoHaceNada() {
        when(documentos.documento("2026-I", 1)).thenReturn(Optional.empty());

        servicio.anular("2026-I", 1, MotivoDeAnulacion.RECHAZO_EN_FIRMA, RECHAZADA);

        verify(documentos, never()).registrarAnulacion(any(), any(), any(), any());
        verify(registro, never()).registrar(any());
        verify(aviso, never()).avisar(any());
    }
}
