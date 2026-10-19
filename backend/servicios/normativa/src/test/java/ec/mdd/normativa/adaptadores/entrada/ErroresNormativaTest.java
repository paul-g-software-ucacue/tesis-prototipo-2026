package ec.mdd.normativa.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Normativa coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresNormativaTest {

    @Test
    void dadoElCatalogoDeNormativa_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresNormativa());
    }

    @Test
    void dadoLasReglasDeLasVersiones_cuandoSeBuscanSusEntradas_entoncesSonLos409() {
        assertThat(new ErroresNormativa().entradas())
                .extracting(EntradaCatalogo::excepcion, entrada -> entrada.definicion().codigo())
                .containsExactlyInAnyOrder(
                        tuple(VersionEnUso.class, "NORMATIVA_VERSION_EN_USO"),
                        tuple(CodigoRepetido.class, "NORMATIVA_CODIGO_REPETIDO"),
                        tuple(PeriodoConVersion.class, "NORMATIVA_PERIODO_CON_VERSION"));
        assertThat(new ErroresNormativa().entradas())
                .allMatch(entrada -> entrada.definicion().estado() == 409);
    }
}
