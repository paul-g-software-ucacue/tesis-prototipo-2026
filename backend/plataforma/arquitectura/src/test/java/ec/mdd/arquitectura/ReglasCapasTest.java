package ec.mdd.arquitectura;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/**
 * Comprueba las reglas de las cinco capas contra fixtures que las cumplen o las violan (escenario
 * «La plantilla respeta las cinco capas», FR-004 y FR-005).
 */
class ReglasCapasTest {

    private static final String FIXTURES = "ec.mdd.arquitectura.fixtures.capas.";

    private static JavaClasses importar(String fixture) {
        return new ClassFileImporter().importPackages(FIXTURES + fixture);
    }

    @Test
    void dadoUnServicioQueRespetaLasCapas_cuandoSeVerifica_entoncesPasaAmbasReglas() {
        JavaClasses clases = importar("correcto");

        assertThatCode(() -> ReglasCapas.SENTIDO_DE_LAS_CAPAS.check(clases))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReglasCapas.DOMINIO_SIN_DETALLES.check(clases))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnDominioQueUsaSpring_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(
                        () -> ReglasCapas.DOMINIO_SIN_DETALLES.check(importar("dominioconspring")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("org.springframework");
    }

    @Test
    void dadoUnDominioQueUsaJpa_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(() -> ReglasCapas.DOMINIO_SIN_DETALLES.check(importar("dominioconjpa")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("jakarta.persistence");
    }

    @Test
    void dadoUnDominioQueUsaHttp_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(() -> ReglasCapas.DOMINIO_SIN_DETALLES.check(importar("dominioconhttp")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("java.net.http");
    }

    @Test
    void dadoUnDominioQueUsaUnaLibreriaDePdf_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(() -> ReglasCapas.DOMINIO_SIN_DETALLES.check(importar("dominioconpdf")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("org.apache.pdfbox");
    }

    @Test
    void dadaUnaAplicacionQueUsaUnAdaptador_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(
                        () ->
                                ReglasCapas.SENTIDO_DE_LAS_CAPAS.check(
                                        importar("aplicacionconadaptador")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void dadoUnPuertoQueUsaInfraestructura_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(
                        () ->
                                ReglasCapas.SENTIDO_DE_LAS_CAPAS.check(
                                        importar("puertoconinfraestructura")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void dadoUnServicioRecienCreadoSinClases_cuandoSeVerifica_entoncesPasaAmbasReglas() {
        JavaClasses vacio = importar("inexistente");

        assertThatCode(() -> ReglasCapas.SENTIDO_DE_LAS_CAPAS.check(vacio))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReglasCapas.DOMINIO_SIN_DETALLES.check(vacio))
                .doesNotThrowAnyException();
    }
}
