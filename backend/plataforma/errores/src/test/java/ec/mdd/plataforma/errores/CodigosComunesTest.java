package ec.mdd.plataforma.errores;

import static org.assertj.core.api.Assertions.assertThatCode;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los códigos comunes del código coinciden con contratos/comun/errores.md (ADR-016). */
class CodigosComunesTest {

    @Test
    void dadoLosCodigosComunes_cuandoSeComparanConElCatalogo_entoncesCoinciden() {
        assertThatCode(() -> VerificadorDeCatalogo.verificar(CodigosComunes.TODOS))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoElCatalogo_cuandoSeVerificaSuFormato_entoncesCadaFilaTieneSusOchoColumnas() {
        assertThatCode(VerificadorDeCatalogo::verificarFormato).doesNotThrowAnyException();
    }
}
