package ec.mdd.plataforma.errores.nucleo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentesTest {

    @Test
    void dadoDosIncidentes_cuandoSeGeneranSusIdentificadores_entoncesSonUuidV4Distintos() {
        String primero = Incidentes.nuevoId();
        String segundo = Incidentes.nuevoId();

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(UUID.fromString(primero).version()).isEqualTo(4);
    }
}
