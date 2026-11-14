package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El estado de la generación del documento firmable (F13-H02, data-model.md). */
class GeneracionDeDocumentoTest {

    private static final Instant ANTES = Instant.parse("2026-11-11T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-11T15:05:00Z");
    private static final List<PasoDeFirma> CADENA =
            List.of(new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"));

    @Test
    void dadaUnaSolicitudNueva_cuandoSeCrea_entoncesEstaEnCursoANombreDeQuienLaPide() {
        GeneracionDeDocumento generacion = nueva();

        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.EN_CURSO);
        assertThat(generacion.motivo()).isEmpty();
        assertThat(generacion.pedidaPor()).isEqualTo("sistema");
        assertThat(generacion.cadena()).isEqualTo(CADENA);
        assertThat(generacion.rolDeLaEtapa()).isEqualTo("RESPONSABLE_CONSOLIDACION");
    }

    @Test
    void dadaUnaGeneracionEnCurso_cuandoFalla_entoncesQuedaFallidaConSuMotivo() {
        GeneracionDeDocumento generacion = nueva();

        generacion.fallar("Falta el nombre del Rector.", AHORA);

        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(generacion.motivo()).contains("Falta el nombre del Rector.");
        assertThat(generacion.actualizadaEn()).isEqualTo(AHORA);
    }

    @Test
    void dadoUnMotivoLargo_cuandoFalla_entoncesSeCortaA200Caracteres() {
        GeneracionDeDocumento generacion = nueva();

        generacion.fallar("x".repeat(500), AHORA);

        assertThat(generacion.motivo().orElseThrow()).hasSize(200).endsWith("…");
    }

    @Test
    void dadaUnaGeneracionFallida_cuandoSeReintenta_entoncesVuelveAEnCursoANombreDeQuienLaPide() {
        GeneracionDeDocumento generacion = nueva();
        generacion.fallar("Falta el nombre del Rector.", AHORA);

        boolean empezo = generacion.iniciar("consolidacion.demo", AHORA);

        assertThat(empezo).isTrue();
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.EN_CURSO);
        assertThat(generacion.motivo()).isEmpty();
        assertThat(generacion.pedidaPor()).isEqualTo("consolidacion.demo");
    }

    @Test
    void dadaUnaGeneracionGenerada_cuandoSeIniciaOtraVez_entoncesNoCambia() {
        GeneracionDeDocumento generacion = nueva();
        generacion.completar(AHORA);

        boolean empezo = generacion.iniciar("consolidacion.demo", AHORA.plusSeconds(60));

        assertThat(empezo).isFalse();
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.GENERADA);
        assertThat(generacion.pedidaPor()).isEqualTo("sistema");
        assertThat(generacion.actualizadaEn()).isEqualTo(AHORA);
    }

    @Test
    void dadaLaCadena_cuandoSePreguntaQuienReintenta_entoncesSoloElRolDeLaEtapa() {
        GeneracionDeDocumento generacion = nueva();

        assertThat(generacion.puedeReintentar(java.util.Set.of("RESPONSABLE_CONSOLIDACION")))
                .isTrue();
        assertThat(generacion.puedeReintentar(java.util.Set.of("VICERRECTOR", "RECTOR"))).isFalse();
    }

    private static GeneracionDeDocumento nueva() {
        return GeneracionDeDocumento.nueva(
                "2026-I", 1, "a".repeat(64), CADENA, "RESPONSABLE_CONSOLIDACION", "sistema", ANTES);
    }
}
