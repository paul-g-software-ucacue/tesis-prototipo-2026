package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificadorDeCatalogoTest {

    private final Path catalogo = Path.of("src/test/resources/catalogo-de-prueba.md");

    @Test
    void dadoUnaDefinicionIgualASuFila_cuandoSeVerifica_entoncesPasa() {
        DefinicionDeCodigo uno =
                new DefinicionDeCodigo("PRUEBA_UNO", 409, "Uno", "La descripción uno.");

        assertThatCode(() -> VerificadorDeCatalogo.verificar(List.of(uno), catalogo))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnTituloDistinto_cuandoSeVerifica_entoncesFallaYNombraElCodigo() {
        DefinicionDeCodigo uno =
                new DefinicionDeCodigo("PRUEBA_UNO", 409, "Otro", "La descripción uno.");

        assertThatThrownBy(() -> VerificadorDeCatalogo.verificar(List.of(uno), catalogo))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("PRUEBA_UNO")
                .hasMessageContaining("título");
    }

    @Test
    void dadoUnCodigoQueNoEstaEnLaTabla_cuandoSeVerifica_entoncesFalla() {
        DefinicionDeCodigo dos = new DefinicionDeCodigo("PRUEBA_DOS", 409, "Dos", "Dos.");

        assertThatThrownBy(() -> VerificadorDeCatalogo.verificar(List.of(dos), catalogo))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("PRUEBA_DOS: no está");
    }

    @Test
    void dadaUnaFilaSinLasColumnasDeD72_cuandoSeVerifica_entoncesFallaYDiceCuantasFaltan() {
        Path incompleto = Path.of("src/test/resources/catalogo-incompleto.md");

        assertThatThrownBy(() -> VerificadorDeCatalogo.verificarFormato(incompleto))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("PRUEBA_INCOMPLETA: tiene 5 columnas y el catálogo pide 8");
    }

    @Test
    void dadaUnaClasificacionQueNoCuadraConElEstado_cuandoSeVerifica_entoncesFalla() {
        Path malClasificado = Path.of("src/test/resources/catalogo-mal-clasificado.md");

        assertThatThrownBy(() -> VerificadorDeCatalogo.verificarFormato(malClasificado))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("«Negocio» no cuadra con el estado 500");
    }

    @Test
    void dadaUnaClasificacionQueNoEsDeLasCinco_cuandoSeVerifica_entoncesFalla() {
        Path sinClasificacion = Path.of("src/test/resources/catalogo-sin-clasificacion.md");

        assertThatThrownBy(() -> VerificadorDeCatalogo.verificarFormato(sinClasificacion))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("«Entrada» no es una de");
    }

    @Test
    void dadoElCatalogoReal_cuandoSeVerificaSuFormato_entoncesTodasLasFilasEstanCompletas() {
        assertThatCode(VerificadorDeCatalogo::verificarFormato).doesNotThrowAnyException();
    }

    @Test
    void dadoLosCodigosComunes_cuandoSeVerificanContraElCatalogoReal_entoncesCoinciden() {
        assertThatCode(() -> VerificadorDeCatalogo.verificar(CodigosComunes.TODOS))
                .doesNotThrowAnyException();
    }
}
