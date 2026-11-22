package ec.mdd.reporting.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.dominio.SinPublicacion;
import org.junit.jupiter.api.Test;

/** Los errores de Reporting coinciden con contratos/comun/errores.md (principio XIII, D-119). */
class ErroresReportingTest {

    @Test
    void dadoElCatalogoDeReporting_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresReporting());
    }

    @Test
    void dadosLosFallosDeReporting_cuandoSeBuscanSusEntradas_entoncesTienenSuCodigo() {
        assertThat(new ErroresReporting().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(SinPublicacion.class, "REPORTING_SIN_PUBLICACION", 409),
                        tuple(
                                DocenteSinDistributivo.class,
                                "REPORTING_DOCENTE_SIN_DISTRIBUTIVO",
                                404),
                        tuple(LayoutInvalido.class, "REPORTING_LAYOUT_INVALIDO", 400),
                        tuple(ArchivoIlegible.class, "REPORTING_ARCHIVO_ILEGIBLE", 400),
                        tuple(
                                ArchivoDemasiadoGrande.class,
                                "REPORTING_ARCHIVO_DEMASIADO_GRANDE",
                                413),
                        tuple(
                                ContenidoInconsistente.class,
                                "REPORTING_CONTENIDO_INCONSISTENTE",
                                500));
    }
}
