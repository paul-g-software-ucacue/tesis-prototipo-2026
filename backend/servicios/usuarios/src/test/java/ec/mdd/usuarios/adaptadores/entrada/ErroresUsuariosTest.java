package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
import ec.mdd.usuarios.aplicacion.EventoNoReconocido;
import ec.mdd.usuarios.aplicacion.SinIdentificacion;
import org.junit.jupiter.api.Test;

/** Los errores de Usuarios coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresUsuariosTest {

    @Test
    void dadoElCatalogoDeUsuarios_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresUsuarios());
    }

    @Test
    void dadoCredencialesInvalidas_cuandoSeBuscaSuEntrada_entoncesEs401ConSuCodigo() {
        assertThat(new ErroresUsuarios().entradas())
                .filteredOn(entrada -> entrada.excepcion() == CredencialesInvalidas.class)
                .singleElement()
                .satisfies(
                        entrada -> {
                            assertThat(entrada.excepcion()).isEqualTo(CredencialesInvalidas.class);
                            assertThat(entrada.definicion().codigo())
                                    .isEqualTo("USUARIOS_CREDENCIALES_INVALIDAS");
                            assertThat(entrada.definicion().estado()).isEqualTo(401);
                        });
    }

    @Test
    void dadoUnEventoNoReconocido_cuandoSeBuscaSuEntrada_entoncesEsElCodigoComunCon400() {
        assertThat(new ErroresUsuarios().entradas())
                .filteredOn(entrada -> entrada.excepcion() == EventoNoReconocido.class)
                .singleElement()
                .satisfies(
                        entrada -> {
                            assertThat(entrada.definicion())
                                    .isEqualTo(CodigosComunes.EVENTO_NO_RECONOCIDO);
                            assertThat(entrada.definicion().estado()).isEqualTo(400);
                        });
    }

    @Test
    void dadaUnaCuentaSinIdentificacion_cuandoSeBuscaSuEntrada_entoncesEs404ConSuCodigo() {
        assertThat(new ErroresUsuarios().entradas())
                .filteredOn(entrada -> entrada.excepcion() == SinIdentificacion.class)
                .singleElement()
                .satisfies(
                        entrada -> {
                            assertThat(entrada.definicion().codigo())
                                    .isEqualTo("USUARIOS_SIN_IDENTIFICACION");
                            assertThat(entrada.definicion().estado()).isEqualTo(404);
                        });
    }
}
