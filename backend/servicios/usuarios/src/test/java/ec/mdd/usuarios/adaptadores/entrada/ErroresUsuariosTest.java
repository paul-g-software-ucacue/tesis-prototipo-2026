package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
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
                .singleElement()
                .satisfies(
                        entrada -> {
                            assertThat(entrada.excepcion()).isEqualTo(CredencialesInvalidas.class);
                            assertThat(entrada.definicion().codigo())
                                    .isEqualTo("USUARIOS_CREDENCIALES_INVALIDAS");
                            assertThat(entrada.definicion().estado()).isEqualTo(401);
                        });
    }
}
