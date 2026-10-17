package ec.mdd.plataforma.errores.nucleo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RegistroDeErroresTest {

    static class ErrorBase extends RuntimeException {}

    static final class ErrorHijo extends ErrorBase {}

    static final class OtroError extends RuntimeException {}

    private final CatalogoDeErrores catalogo =
            () ->
                    List.of(
                            EntradaCatalogo.de(
                                    ErrorBase.class, "PRUEBA_BASE", 409, "Base", "Una base."));

    @Test
    void dadoUnaExcepcionRegistrada_cuandoSeBusca_entoncesDevuelveSuDefinicion() {
        RegistroDeErrores registro = new RegistroDeErrores(List.of(catalogo));

        assertThat(registro.buscar(ErrorBase.class))
                .get()
                .extracting(DefinicionDeCodigo::codigo)
                .isEqualTo("PRUEBA_BASE");
    }

    @Test
    void dadoUnaSubclaseDeUnaRegistrada_cuandoSeBusca_entoncesUsaLaDeLaSuperclase() {
        RegistroDeErrores registro = new RegistroDeErrores(List.of(catalogo));

        assertThat(registro.buscar(ErrorHijo.class))
                .get()
                .extracting(DefinicionDeCodigo::estado)
                .isEqualTo(409);
        assertThat(registro.buscar(OtroError.class)).isEmpty();
    }

    @Test
    void dadoUnaEntradaConEncabezados_cuandoSeBuscaLaExcepcion_entoncesLosCalculaConSusDatos() {
        CatalogoDeErrores conEncabezados =
                () ->
                        List.of(
                                EntradaCatalogo.conEncabezados(
                                        OtroError.class,
                                        new DefinicionDeCodigo("PRUEBA_OTRO", 429, "Otro", "Otro."),
                                        error -> Map.of("Retry-After", "7")));
        RegistroDeErrores registro = new RegistroDeErrores(List.of(catalogo, conEncabezados));

        assertThat(
                        registro.buscarEntrada(OtroError.class)
                                .orElseThrow()
                                .encabezados(new OtroError()))
                .containsExactly(Map.entry("Retry-After", "7"));
        assertThat(
                        registro.buscarEntrada(ErrorHijo.class)
                                .orElseThrow()
                                .encabezados(new ErrorHijo()))
                .isEmpty();
    }

    @Test
    void dadoDosCatalogosConElMismoCodigo_cuandoSeArma_entoncesFallaYDiceCual() {
        CatalogoDeErrores otro =
                () ->
                        List.of(
                                EntradaCatalogo.de(
                                        OtroError.class, "PRUEBA_BASE", 409, "Otro", "Otro."));

        assertThatThrownBy(() -> new RegistroDeErrores(List.of(catalogo, otro)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRUEBA_BASE");
    }

    @Test
    void dadoDosCatalogosConLaMismaExcepcion_cuandoSeArma_entoncesFallaYDiceCual() {
        CatalogoDeErrores otro =
                () ->
                        List.of(
                                EntradaCatalogo.de(
                                        ErrorBase.class, "PRUEBA_OTRO", 409, "Otro", "Otro."));

        assertThatThrownBy(() -> new RegistroDeErrores(List.of(catalogo, otro)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ErrorBase");
    }

    @Test
    void dadoUnCodigoMalFormadoOUnEstadoQueNoEsDeError_cuandoSeDefine_entoncesFalla() {
        assertThatThrownBy(() -> new DefinicionDeCodigo("minusculas", 400, "T", "D"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefinicionDeCodigo("PRUEBA_OK", 200, "T", "D"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefinicionDeCodigo("PRUEBA_OK", 400, " ", "D"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
