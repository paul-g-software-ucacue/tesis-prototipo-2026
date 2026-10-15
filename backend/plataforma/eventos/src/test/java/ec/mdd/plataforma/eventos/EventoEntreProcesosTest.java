package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EventoEntreProcesosTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-14T15:04:05Z"), ZoneOffset.UTC);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void dadoUnEventoNuevo_cuandoSeCrea_entoncesTieneUnIdentificadorV4YSusDatos() {
        EventoEntreProcesos evento = nuevo();

        assertThat(evento.idEvento().version()).isEqualTo(4);
        assertThat(evento.momento()).isEqualTo(Instant.parse("2026-10-14T15:04:05Z"));
        assertThat(nuevo().idEvento()).isNotEqualTo(evento.idEvento());
    }

    @Test
    void dadoUnTipoOUnOrigenMalFormado_cuandoSeCrea_entoncesFalla() {
        assertThatThrownBy(
                        () ->
                                EventoEntreProcesos.nuevo(
                                        "EventoDePrueba",
                                        "generacion",
                                        "usuarios",
                                        Map.of(),
                                        reloj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo");
        assertThatThrownBy(
                        () ->
                                EventoEntreProcesos.nuevo(
                                        "generacion.EventoDePrueba",
                                        "Generacion",
                                        "usuarios",
                                        Map.of(),
                                        reloj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origen");
        assertThatThrownBy(
                        () ->
                                EventoEntreProcesos.nuevo(
                                        "generacion.EventoDePrueba",
                                        "generacion",
                                        "",
                                        Map.of(),
                                        reloj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destino");
    }

    @Test
    void dadoElSobre_cuandoSeGuardaYSeLeeComoEnElRegistro_entoncesConservaElDestino() {
        EventoEntreProcesos evento = nuevo();

        String guardado = json.writeValueAsString(evento);
        EventoEntreProcesos leido = json.readValue(guardado, EventoEntreProcesos.class);

        assertThat(leido).isEqualTo(evento);
        assertThat(leido.destino()).isEqualTo("usuarios");
    }

    @Test
    void dadoElCuerpoHttp_cuandoSeValidaContraElContrato_entoncesNoLlevaDestinoYCumple() {
        String cuerpo = json.writeValueAsString(CuerpoEvento.de(nuevo()));

        assertThat(cuerpo)
                .doesNotContain("destino")
                .contains("\"momento\":\"2026-10-14T15:04:05Z\"");
        ValidadorDeContrato.para("usuarios")
                .validarPeticion("POST", "/interno/usuarios/eventos", cuerpo);
    }

    @Test
    void dadoElSobre_cuandoSeConvierteEnTexto_entoncesNoMuestraElContenido() {
        assertThat(nuevo().toString()).contains("generacion.EventoDePrueba").doesNotContain("nota");
    }

    private EventoEntreProcesos nuevo() {
        return EventoEntreProcesos.nuevo(
                "generacion.EventoDePrueba",
                "generacion",
                "usuarios",
                Map.of("nota", "evento de prueba B00-H05"),
                reloj);
    }
}
