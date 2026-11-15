package ec.mdd.firma.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmaRegistrada;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.ObservacionInvalida;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.RechazoEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoRegistrado;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.AvisoDeRechazoEnFirma;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El rechazo en firma (F14-H04, D-105): solo el firmante en turno, la observación después del
 * turno, y el registro, la bitácora y el aviso a Aprobación en una transacción.
 */
class RechazarEnFirmaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T10:00:00Z");
    private static final String OBSERVACION = "Falta la carga de un docente de Software.";
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"));
    private static final Solicitante RECTOR = new Solicitante("rectorado.demo", Set.of("RECTOR"));

    private final RepositorioDocumentos documentos = mock(RepositorioDocumentos.class);
    private final AlmacenDeDocumentos almacen = mock(AlmacenDeDocumentos.class);
    private final IdentificacionDeUsuarios identificaciones = mock(IdentificacionDeUsuarios.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AvisoDeRechazoEnFirma aviso = mock(AvisoDeRechazoEnFirma.class);
    private final TransaccionDePrueba transaccion = new TransaccionDePrueba();
    private final List<String> dentroDeLaTransaccion = new ArrayList<>();

    private final RechazarEnFirmaServicio servicio =
            new RechazarEnFirmaServicio(
                    documentos,
                    almacen,
                    identificaciones,
                    registro,
                    transaccion,
                    aviso,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    private final DocumentoFirmable documento =
            new DocumentoFirmable(
                    UUID.randomUUID(),
                    "2026-I",
                    1,
                    DocumentoFirmable.CONSOLIDADO,
                    "a".repeat(64),
                    "b".repeat(64),
                    "documentos/" + "b".repeat(64) + ".pdf",
                    1024,
                    "sistema",
                    AHORA.minusSeconds(3600));

    /** Ejecuta el trabajo y anota si se está dentro de la transacción. */
    private static final class TransaccionDePrueba implements Transaccion {
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
    }

    // F1 aceptada: F2, del Vicerrector, en turno.
    private FirmasDelDocumento enF2() {
        FirmasDelDocumento firmas =
                new FirmasDelDocumento(
                        documento,
                        List.of(
                                new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                                new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                                new PasoDeFirma(3, "Aprobado", "RECTOR")),
                        List.of(
                                FirmanteEsperado.porUsuario(
                                        1,
                                        "RESPONSABLE_CONSOLIDACION",
                                        "docente.demo",
                                        "Docente (demo)"),
                                FirmanteEsperado.porIdentificacion(
                                        2,
                                        "VICERRECTOR",
                                        "0109990028",
                                        "CABRERA VAZQUEZ ANA GABRIELA"),
                                FirmanteEsperado.porIdentificacion(
                                        3,
                                        "RECTOR",
                                        "0109990010",
                                        "PERALTA MALDONADO JUAN ANDRES")),
                        List.of(
                                new FirmaRegistrada(
                                        1,
                                        "c".repeat(64),
                                        2048,
                                        "docente.demo",
                                        AHORA.minusSeconds(600))));
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas));
        when(identificaciones.de("vicerrectorado.demo")).thenReturn(Optional.of("0109990028"));
        when(identificaciones.de("rectorado.demo")).thenReturn(Optional.of("0109990010"));
        return firmas;
    }

    private void anotarTransaccion() {
        doAnswer(
                        invocacion -> {
                            dentroDeLaTransaccion.add("registro:" + transaccion.dentro);
                            return null;
                        })
                .when(registro)
                .registrar(any());
        doAnswer(
                        invocacion -> {
                            dentroDeLaTransaccion.add("aviso:" + transaccion.dentro);
                            return null;
                        })
                .when(aviso)
                .avisar(any());
    }

    @Test
    void dadoElFirmanteEnTurno_cuandoRechazaConObservacion_entoncesSeRegistraSePublicaYSeAvisa() {
        FirmasDelDocumento firmas = enF2();
        when(documentos.registrarRechazo(any(), any())).thenReturn(true);
        anotarTransaccion();

        RechazoRegistrado registrado =
                servicio.rechazar(documento.id(), "  " + OBSERVACION + "  ", VICERRECTOR);

        RechazoDelFirmante esperado =
                new RechazoDelFirmante(2, "vicerrectorado.demo", AHORA, OBSERVACION);
        verify(documentos).registrarRechazo(esperado, firmas);
        assertThat(registrado)
                .isEqualTo(
                        new RechazoRegistrado(
                                documento.id(),
                                "2026-I",
                                1,
                                2,
                                "Revisado por",
                                "vicerrectorado.demo",
                                AHORA,
                                OBSERVACION));
        verify(aviso)
                .avisar(
                        new RechazoEnFirma(
                                "2026-I",
                                1,
                                2,
                                "VICERRECTOR",
                                "vicerrectorado.demo",
                                AHORA,
                                OBSERVACION));
        ArgumentCaptor<CambioDeFirma> cambio = ArgumentCaptor.forClass(CambioDeFirma.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioDeFirma.RECHAZO_EN_FIRMA);
        assertThat(cambio.getValue().usuario()).isEqualTo("vicerrectorado.demo");
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1)
                .containsEntry("orden", 2);
        assertThat(cambio.getValue().nuevo()).containsEntry("observacion", OBSERVACION);
        assertThat(dentroDeLaTransaccion).containsExactly("registro:true", "aviso:true");
    }

    @Test
    void dadoQuienNoEstaEnTurno_cuandoRechaza_entoncesSinTurnoSinMirarLaObservacion() {
        enF2();

        assertThatThrownBy(() -> servicio.rechazar(documento.id(), "", RECTOR))
                .isInstanceOf(SinTurnoDeFirma.class);
        assertThatThrownBy(() -> servicio.rechazar(documento.id(), OBSERVACION, RECTOR))
                .isInstanceOf(SinTurnoDeFirma.class);
        verify(documentos, never()).registrarRechazo(any(), any());
        verifyNoInteractions(registro, aviso);
    }

    @Test
    void dadoUnDocumentoQueNoExiste_cuandoRechaza_entoncesSinTurno() {
        when(documentos.firmas(documento.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.rechazar(documento.id(), OBSERVACION, VICERRECTOR))
                .isInstanceOf(SinTurnoDeFirma.class);
        verifyNoInteractions(registro, aviso);
    }

    @Test
    void dadoElFirmanteEnTurno_cuandoRechazaConUnaObservacionCorta_entoncesNoSeRegistra() {
        enF2();

        assertThatThrownBy(() -> servicio.rechazar(documento.id(), "  corta  ", VICERRECTOR))
                .isInstanceOf(ObservacionInvalida.class);
        verify(documentos, never()).registrarRechazo(any(), any());
        verifyNoInteractions(registro, aviso);
    }

    @Test
    void dadaUnaFirmaRegistradaEntretanto_cuandoRechaza_entoncesSinTurnoSinCambioNiAviso() {
        enF2();
        when(documentos.registrarRechazo(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.rechazar(documento.id(), OBSERVACION, VICERRECTOR))
                .isInstanceOf(SinTurnoDeFirma.class);
        verifyNoInteractions(registro, aviso);
    }
}
