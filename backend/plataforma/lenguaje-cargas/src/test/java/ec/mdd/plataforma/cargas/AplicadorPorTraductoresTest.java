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

                /** Las sedes sí dependen del periodo: solo cuentan las de ese periodo. */
                @Override
                public boolean tieneDatosEn(String periodo) {
                    return datos.values().stream()
                            .anyMatch(sede -> sede.get("periodo").equals(periodo));
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

    @Test
    void tieneDatosEn_porDefectoEsLoMismoQueTieneDatos() {
        unidades.datos.put("U1", Map.of("codigo", "U1", "nombre", "Unidad", "periodo", "2026-I"));

        assertThat(aplicador.tieneDatosEn("unidades", "2026-II")).isTrue();
    }

    @Test
    void tieneDatosEn_deUnaCargaDelPeriodo_esLoQueDiceSuTraductor() {
        sedes.datos.put("S1", Map.of("codigo", "S1", "nombre", "Sede", "periodo", "2026-I"));

        assertThat(aplicador.tieneDatosEn("sedes", "2026-I")).isTrue();
        assertThat(aplicador.tieneDatosEn("sedes", "2026-II")).isFalse();
    }

    @Test
    void tieneDatosEn_deUnaCargaQueNoAtiende_esUnErrorDeProgramacion() {
        assertThatThrownBy(() -> aplicador.tieneDatosEn("docentes", "2026-I"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
