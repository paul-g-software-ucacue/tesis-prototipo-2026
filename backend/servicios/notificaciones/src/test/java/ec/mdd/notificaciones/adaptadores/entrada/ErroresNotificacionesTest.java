package ec.mdd.notificaciones.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.aplicacion.EventoNoReconocido;
import ec.mdd.notificaciones.dominio.NotificacionInvalida;
import ec.mdd.notificaciones.dominio.NotificacionNoEncontrada;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Notificaciones coinciden con contratos/comun/errores.md (ADR-016, F16-H01). */
class ErroresNotificacionesTest {

    @Test
    void dadoElCatalogoDeNotificaciones_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresNotificaciones());
    }

    @Test
    void dadaUnaNotificacionInvalida_cuandoSeBuscaSuEntrada_entoncesEs400ConSuCodigo() {
        EntradaCatalogo entrada = entrada(NotificacionInvalida.class);

        assertThat(entrada.definicion().codigo()).isEqualTo("NOTIFICACIONES_NOTIFICACION_INVALIDA");
        assertThat(entrada.definicion().estado()).isEqualTo(400);
    }

    @Test
    void dadaUnaNotificacionNoEncontrada_cuandoSeBuscaSuEntrada_entoncesEs404ConSuCodigo() {
        EntradaCatalogo entrada = entrada(NotificacionNoEncontrada.class);

        assertThat(entrada.definicion().codigo())
                .isEqualTo("NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA");
        assertThat(entrada.definicion().estado()).isEqualTo(404);
    }

    @Test
    void dadoUnEventoNoReconocido_cuandoSeBuscaSuEntrada_entoncesEsElCodigoComunCon400() {
        assertThat(entrada(EventoNoReconocido.class).definicion())
                .isEqualTo(CodigosComunes.EVENTO_NO_RECONOCIDO);
    }

    private static EntradaCatalogo entrada(Class<?> excepcion) {
        return new ErroresNotificaciones()
                .entradas().stream()
                        .filter(una -> una.excepcion() == excepcion)
                        .findFirst()
                        .orElseThrow();
    }
}
