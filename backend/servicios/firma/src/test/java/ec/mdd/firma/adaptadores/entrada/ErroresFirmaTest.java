package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.GeneracionFallida;
import ec.mdd.firma.dominio.PasoSinNombre;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Los errores de Firma coinciden con contratos/comun/errores.md (ADR-016, F13-H02). */
class ErroresFirmaTest {

    @Test
    void dadoElCatalogoDeFirma_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresFirma());
    }

    @Test
    void dadosLosFallosDeFirma_cuandoSeBuscanSusEntradas_entoncesTienenSuCodigo() {
        assertThat(new ErroresFirma().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(
                                EtapaNoAdmiteGeneracion.class,
                                "FIRMA_ETAPA_NO_ADMITE_GENERACION",
                                409),
                        tuple(GeneracionFallida.class, "FIRMA_GENERACION_FALLIDA", 409),
                        tuple(ArchivoNoDisponible.class, "FIRMA_ARCHIVO_NO_DISPONIBLE", 500),
                        tuple(
                                ArchivoFirmadoRechazado.NoEsLaVersionVigente.class,
                                "FIRMA_NO_ES_LA_VERSION_VIGENTE",
                                422),
                        tuple(
                                ArchivoFirmadoRechazado.SinUnaFirmaNueva.class,
                                "FIRMA_SIN_UNA_FIRMA_NUEVA",
                                422),
                        tuple(ArchivoFirmadoRechazado.NoIntegra.class, "FIRMA_NO_INTEGRA", 422),
                        tuple(
                                ArchivoFirmadoRechazado.SinConfianza.class,
                                "FIRMA_SIN_CONFIANZA",
                                422),
                        tuple(
                                ArchivoFirmadoRechazado.NoVigente.class,
                                "FIRMA_CERTIFICADO_NO_VIGENTE",
                                422),
                        tuple(
                                ArchivoFirmadoRechazado.FueraDeOrden.class,
                                "FIRMA_FUERA_DE_ORDEN",
                                422),
                        tuple(
                                ArchivoFirmadoRechazado.FirmanteEquivocado.class,
                                "FIRMA_FIRMANTE_EQUIVOCADO",
                                422),
                        tuple(PasoSinNombre.class, "FIRMA_PASO_SIN_NOMBRE", 409),
                        tuple(ArchivoVacio.class, "FIRMA_ARCHIVO_VACIO", 400),
                        tuple(ArchivoDemasiadoGrande.class, "FIRMA_ARCHIVO_DEMASIADO_GRANDE", 413));
    }

    @Test
    void dadaUnaGeneracionFallida_cuandoSeTraduce_entoncesLlevaElMotivoEnLosDetalles() {
        EntradaCatalogo entrada =
                new ErroresFirma()
                        .entradas().stream()
                                .filter(una -> una.excepcion() == GeneracionFallida.class)
                                .findFirst()
                                .orElseThrow();

        assertThat(entrada.detalles(new GeneracionFallida("Falta el nombre del Rector.")))
                .containsExactly(new DetalleRespuesta("motivo", "Falta el nombre del Rector."));
    }

    @Test
    void
            dadoUnArchivoFirmadoRechazado_cuandoSeTraduce_entoncesLlevaLasCuatroComprobacionesEnOrden() {
        ResultadoValidacion resultado =
                new ResultadoValidacion(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        "docente.demo",
                        Instant.parse("2026-11-14T10:00:00Z"),
                        "c".repeat(64),
                        2048,
                        ResultadoDeComprobacion.desde(
                                ResultadoDeComprobacion.aprobada(Comprobacion.MISMO_DOCUMENTO),
                                ResultadoDeComprobacion.aprobada(Comprobacion.UNA_FIRMA_NUEVA),
                                ResultadoDeComprobacion.rechazada(
                                        Comprobacion.INTEGRIDAD, "La firma nueva no es íntegra.")),
                        Optional.of(
                                new CertificadoDeLaFirma(
                                        "Firmante Uno Prueba (F1)",
                                        "CN=CA de Prueba MDD Demo",
                                        "1a",
                                        Instant.parse("2026-10-13T10:00:00Z"),
                                        Instant.parse("2029-10-12T10:00:00Z"))));
        ArchivoFirmadoRechazado rechazo = ArchivoFirmadoRechazado.de(resultado);
        EntradaCatalogo entrada =
                new ErroresFirma()
                        .entradas().stream()
                                .filter(una -> una.excepcion().isInstance(rechazo))
                                .findFirst()
                                .orElseThrow();

        assertThat(entrada.definicion().codigo()).isEqualTo("FIRMA_NO_INTEGRA");
        assertThat(entrada.detalles(rechazo))
                .containsExactly(
                        new DetalleRespuesta("MISMO_DOCUMENTO", "Aprobada."),
                        new DetalleRespuesta("UNA_FIRMA_NUEVA", "Aprobada."),
                        new DetalleRespuesta(
                                "INTEGRIDAD", "Rechazada. La firma nueva no es íntegra."),
                        new DetalleRespuesta(
                                "CONFIANZA", "No evaluada. La firma nueva no es íntegra."),
                        new DetalleRespuesta(
                                "VIGENCIA", "No evaluada. La firma nueva no es íntegra."),
                        new DetalleRespuesta("ORDEN", "No evaluada. La firma nueva no es íntegra."),
                        new DetalleRespuesta(
                                "FIRMANTE", "No evaluada. La firma nueva no es íntegra."));
    }
}
