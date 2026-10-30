package ec.mdd.plataforma.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** El aplicador de un dueño reparte cada pedido al traductor de su carga (ADR-022, F01-H05). */
class AplicadorPorTraductoresTest {

    private final MotorDelLoteTest.Unidades unidades = new MotorDelLoteTest.Unidades();
    private final MotorDelLoteTest.Unidades sedes =
            new MotorDelLoteTest.Unidades() {
                @Override
                public String carga() {
                    return "sedes";
                }
            };
    private final AplicadorPorTraductores aplicador =
            new AplicadorPorTraductores(List.of(unidades, sedes), Supplier::get);

    @Test
    void tieneDatos_esLoQueDiceElTraductorDeEsaCarga() {
        unidades.datos.put("U1", Map.of("codigo", "U1", "nombre", "Unidad", "periodo", "2026-I"));

        assertThat(aplicador.tieneDatos("unidades")).isTrue();
        assertThat(aplicador.tieneDatos("sedes")).isFalse();
    }

    @Test
    void tieneDatos_deUnaCargaQueNoAtiende_esUnErrorDeProgramacion() {
        assertThatThrownBy(() -> aplicador.tieneDatos("docentes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("docentes");
    }
}
