package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Generación coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresGeneracionTest {

    @Test
    void dadoElCatalogoDeGeneracion_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresGeneracion());
    }

    @Test
    void dadosLosFallosDeLaAfinidad_cuandoSeBuscanSusEntradas_entoncesTienenSuCodigo() {
        assertThat(new ErroresGeneracion().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(PeriodoNoEncontrado.class, "GENERACION_PERIODO_NO_ENCONTRADO", 404),
                        tuple(PeriodoSinAbrir.class, "GENERACION_PERIODO_SIN_ABRIR", 409),
                        tuple(DocenteNoEncontrado.class, "GENERACION_DOCENTE_NO_ENCONTRADO", 404));
    }
}
