package ec.mdd.demobeta;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.demobeta.aplicacion.UsoInterno;
import org.junit.jupiter.api.Test;

class UsoInternoTest {
    @Test
    void saluda() {
        assertThat(new UsoInterno().saludo()).isEqualTo("hola");
    }
}
