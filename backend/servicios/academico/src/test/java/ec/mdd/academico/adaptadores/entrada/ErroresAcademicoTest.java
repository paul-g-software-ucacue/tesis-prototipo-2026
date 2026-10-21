package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.dominio.PeriodoSinVersion;
import ec.mdd.academico.dominio.TransicionNoPermitida;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Académico coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresAcademicoTest {

    @Test
    void dadoElCatalogoDeAcademico_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresAcademico());
    }

    @Test
    void dadoLasReglasDeLosPeriodos_cuandoSeBuscanSusEntradas_entoncesSonLos409() {
        assertThat(new ErroresAcademico().entradas())
                .extracting(EntradaCatalogo::excepcion, entrada -> entrada.definicion().codigo())
                .containsExactlyInAnyOrder(
                        tuple(CodigoRepetido.class, "ACADEMICO_CODIGO_REPETIDO"),
                        tuple(PeriodoCerrado.class, "ACADEMICO_PERIODO_CERRADO"),
                        tuple(TransicionNoPermitida.class, "ACADEMICO_TRANSICION_NO_PERMITIDA"),
                        tuple(PeriodoSinVersion.class, "ACADEMICO_PERIODO_SIN_VERSION"),
                        tuple(PeriodoConOtraVersion.class, "ACADEMICO_PERIODO_CON_VERSION"));
        assertThat(new ErroresAcademico().entradas())
                .allMatch(entrada -> entrada.definicion().estado() == 409);
    }
}
