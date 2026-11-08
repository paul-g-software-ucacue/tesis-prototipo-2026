package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de la página pedida (data-model.md, FR-004, research §2). */
class PaginaPedidaTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T20:00:00Z");

    @Test
    void dadoSinValores_cuandoSeConstruye_entoncesEsLaPrimeraDe50ConCorteAhora() {
        PaginaPedida pagina = PaginaPedida.de(null, null, null, AHORA);

        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(50);
        assertThat(pagina.corte()).isEqualTo(AHORA);
        assertThat(pagina.desplazamiento()).isZero();
    }

    @Test
    void dadoLaPaginaTresDe25_cuandoSeConstruye_entoncesSaltaLasPrimeras50() {
        PaginaPedida pagina = PaginaPedida.de(3, 25, null, AHORA);

        assertThat(pagina.desplazamiento()).isEqualTo(50L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void dadoUnTamanoEnSusLimites_cuandoSeConstruye_entoncesSeAdmite(int tamano) {
        assertThat(PaginaPedida.de(1, tamano, null, AHORA).tamano()).isEqualTo(tamano);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101, -1})
    void dadoUnTamanoFueraDeSusLimites_cuandoSeConstruye_entoncesFallaEnTamano(int tamano) {
        assertThatThrownBy(() -> PaginaPedida.de(1, tamano, null, AHORA))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("tamano"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void dadoUnaPaginaMenorQueUno_cuandoSeConstruye_entoncesFallaEnPagina(int numero) {
        assertThatThrownBy(() -> PaginaPedida.de(numero, 50, null, AHORA))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("pagina"));
    }

    @Test
    void dadoUnCorteAnterior_cuandoSeConstruye_entoncesLoConserva() {
        Instant corte = AHORA.minusSeconds(60);

        assertThat(PaginaPedida.de(2, 50, corte, AHORA).corte()).isEqualTo(corte);
    }

    @Test
    void dadoUnCorteIgualAAhora_cuandoSeConstruye_entoncesSeAdmite() {
        assertThat(PaginaPedida.de(2, 50, AHORA, AHORA).corte()).isEqualTo(AHORA);
    }

    @Test
    void dadoUnCorteFuturo_cuandoSeConstruye_entoncesFallaEnCorte() {
        assertThatThrownBy(() -> PaginaPedida.de(2, 50, AHORA.plusSeconds(1), AHORA))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("corte"));
    }
}
