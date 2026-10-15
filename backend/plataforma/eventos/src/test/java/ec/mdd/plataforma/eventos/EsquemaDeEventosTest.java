package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EsquemaDeEventosTest {

    @ParameterizedTest
    @ValueSource(strings = {"eventos_distributivo", "eventos_prueba"})
    void dadoUnEsquemaDeEventos_cuandoSeValida_entoncesPasa(String esquema) {
        assertThat(EsquemaDeEventos.validar(esquema)).isEqualTo(esquema);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {"public", "generacion", "eventos_", "eventos_Distributivo", "eventos;x"})
    void dadoUnEsquemaQueNoEsDeEventos_cuandoSeValida_entoncesFallaConUnMensajeClaro(
            String esquema) {
        assertThatThrownBy(() -> EsquemaDeEventos.validar(esquema))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.modulith.events.jdbc.schema")
                .hasMessageContaining("eventos_<proceso>");
    }
}
