package ec.mdd.demoalfa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.demoalfa.aplicacion.ServicioDemo;
import org.junit.jupiter.api.Test;

class ServicioDemoTest {
    @Test
    void dadoElServicio_cuandoSaluda_entoncesDiceHola() {
        assertThat(new ServicioDemo().saludo()).isEqualTo("hola");
    }
}
