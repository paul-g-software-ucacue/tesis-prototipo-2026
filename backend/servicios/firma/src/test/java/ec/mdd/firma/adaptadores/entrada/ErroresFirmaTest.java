package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.GeneracionFallida;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
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
                        tuple(GeneracionFallida.class, "FIRMA_GENERACION_FALLIDA", 409));
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
}
