package ec.mdd.arquitectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/**
 * Comprueba que, con la API publicada en {@code puertos.entrada}, Spring Modulith detecta el uso de
 * clases internas entre servicios de un mismo proceso (escenario «Un servicio no usa las clases
 * internas de otro», FR-006, ADR-012).
 */
class ModulosFixturesTest {

    private static final String FIXTURES = "ec.mdd.arquitectura.fixtures.modulos.";

    /** Las fixtures son clases de prueba; por defecto Spring Modulith no las importaría. */
    private static final ImportOption INCLUIR_PRUEBAS = ubicacion -> true;

    @Test
    void dadosDosServiciosQueSeUsanPorSuApiPublicada_cuandoSeVerificanLosModulos_entoncesPasa() {
        ApplicationModules modulos = ApplicationModules.of(FIXTURES + "correcto", INCLUIR_PRUEBAS);

        assertThat(modulos.stream()).hasSize(2);
        assertThatCode(modulos::verify).doesNotThrowAnyException();
    }

    @Test
    void dadoUnServicioQueUsaUnaClaseInternaDeOtro_cuandoSeVerificanLosModulos_entoncesFalla() {
        ApplicationModules modulos =
                ApplicationModules.of(FIXTURES + "usointerno", INCLUIR_PRUEBAS);

        assertThat(modulos.stream()).hasSize(2);
        assertThatThrownBy(modulos::verify)
                .isInstanceOf(Violations.class)
                .hasMessageContaining("ServicioAlfa");
    }
}
