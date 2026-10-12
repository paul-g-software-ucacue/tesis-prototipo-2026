package ec.mdd.demoalfa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.demoalfa.aplicacion.ServicioDemo;
import org.junit.jupiter.api.Test;

/** Prueba que falla a propósito (escenario 3). */
class PruebaEnRojoTest {
    @Test
    void dadoElServicio_cuandoSaluda_entoncesDiceAdios() {
        assertThat(new ServicioDemo().saludo()).isEqualTo("adiós");
    }
}
