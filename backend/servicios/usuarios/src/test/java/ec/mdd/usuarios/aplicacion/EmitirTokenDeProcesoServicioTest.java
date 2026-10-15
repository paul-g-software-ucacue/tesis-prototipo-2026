package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Token de proceso con el emisor simulado (principio VIII: Mockito solo aquí; D-49). */
class EmitirTokenDeProcesoServicioTest {

    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final TokenEmitido token =
            new TokenEmitido("token", Instant.parse("2026-10-14T12:00:00Z"));
    private final EmitirTokenDeProcesoServicio servicio =
            new EmitirTokenDeProcesoServicio(Map.of("distributivo", "clave-del-proceso"), emisor);

    @Test
    void dadoLaClaveCorrecta_cuandoPideElToken_entoncesLoEmiteParaElProceso() {
        when(emisor.emitirParaProceso("distributivo")).thenReturn(token);

        assertThat(servicio.emitir("distributivo", "clave-del-proceso")).isEqualTo(token);
    }

    @Test
    void dadoUnaClaveIncorrecta_cuandoPideElToken_entoncesCredencialesInvalidas() {
        assertThatThrownBy(() -> servicio.emitir("distributivo", "otra"))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThatThrownBy(() -> servicio.emitir("distributivo", "clave-del-proceso-y-mas"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(emisor, never()).emitirParaProceso(anyString());
    }

    @Test
    void dadoUnProcesoSinClave_cuandoPideElToken_entoncesLaMismaRespuesta() {
        assertThatThrownBy(() -> servicio.emitir("salida", "clave-del-proceso"))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThatThrownBy(() -> servicio.emitir(null, null))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(emisor, never()).emitirParaProceso(anyString());
    }

    @Test
    void dadoUnaClaveConfiguradaVacia_cuandoSeArmaElServicio_entoncesElProcesoNoQuedaHabilitado() {
        EmitirTokenDeProcesoServicio sinClave =
                new EmitirTokenDeProcesoServicio(Map.of("distributivo", " "), emisor);

        assertThatThrownBy(() -> sinClave.emitir("distributivo", " "))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThat(sinClave.procesosHabilitados()).isEmpty();
    }

    @Test
    void dadoDosClavesDeLargoDistinto_cuandoSeComparan_entoncesSeComparaSuHash() {
        assertThat(EmitirTokenDeProcesoServicio.huella("a")).hasSize(32);
        assertThat(EmitirTokenDeProcesoServicio.huella("una clave mucho más larga")).hasSize(32);
    }
}
