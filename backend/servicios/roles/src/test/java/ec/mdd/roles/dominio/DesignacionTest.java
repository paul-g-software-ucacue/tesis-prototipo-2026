package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Las reglas de la designación del Responsable de consolidación (D-13, ADR-003, FR-004 a FR-006),
 * sin dobles.
 */
class DesignacionTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    private static final String VICERRECTOR = "vicerrectorado.demo";
    private static final PeriodoConocido ABIERTO =
            new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION);
    private static final List<String> DOCENTE = List.of("DOCENTE");

    @Test
    void dadoUnDocenteYUnPeriodoAbierto_cuandoSeDesigna_entoncesQuedaConQuienYCuando() {
        Designacion designacion =
                Designacion.de(
                        ABIERTO, " Docente.Demo ", VICERRECTOR, DOCENTE, Optional.empty(), AHORA);

        assertThat(designacion)
                .isEqualTo(new Designacion("2026-2", "docente.demo", VICERRECTOR, AHORA));
    }

    @Test
    void dadoUnPeriodoEnVigenteONoCerrado_cuandoSeDesigna_entoncesSeAcepta() {
        for (EstadoConocido estado :
                new EstadoConocido[] {EstadoConocido.EN_APROBACION, EstadoConocido.VIGENTE}) {
            assertThat(
                            Designacion.de(
                                    new PeriodoConocido("2026-2", estado),
                                    "docente.demo",
                                    VICERRECTOR,
                                    DOCENTE,
                                    Optional.empty(),
                                    AHORA))
                    .isNotNull();
        }
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeDesigna_entoncesPeriodoCerrado() {
        assertThatThrownBy(
                        () ->
                                Designacion.de(
                                        new PeriodoConocido("2026-1", EstadoConocido.CERRADO),
                                        "docente.demo",
                                        VICERRECTOR,
                                        DOCENTE,
                                        Optional.empty(),
                                        AHORA))
                .isInstanceOf(PeriodoCerrado.class)
                .hasMessageContaining("2026-1");
    }

    @Test
    void dadoUnUsuarioSinElRolDocente_cuandoSeDesigna_entoncesDesignacionInvalida() {
        assertThatThrownBy(
                        () ->
                                Designacion.de(
                                        ABIERTO,
                                        "rectorado.demo",
                                        VICERRECTOR,
                                        List.of("RECTOR"),
                                        Optional.empty(),
                                        AHORA))
                .isInstanceOf(DesignacionInvalida.class)
                .extracting("campo", "motivo")
                .containsExactly(
                        "usuario", "Solo se puede designar a un usuario con el rol Docente");
    }

    @Test
    void dadoQuienDesigna_cuandoSeDesignaASiMismo_entoncesDesignacionInvalida() {
        assertThatThrownBy(
                        () ->
                                Designacion.de(
                                        ABIERTO,
                                        "VICERRECTORADO.DEMO",
                                        VICERRECTOR,
                                        List.of("DOCENTE", "VICERRECTOR"),
                                        Optional.empty(),
                                        AHORA))
                .isInstanceOf(DesignacionInvalida.class)
                .extracting("campo", "motivo")
                .containsExactly(
                        "usuario",
                        "No puedes designarte a ti mismo: la consolidación y la revisión van en"
                                + " manos distintas");
    }

    @Test
    void dadoElMismoResponsable_cuandoSeDesignaOtraVez_entoncesQuedaLaAnteriorSinCambios() {
        Designacion anterior =
                new Designacion(
                        "2026-2", "docente.demo", "otro.vicerrector", AHORA.minusSeconds(60));

        assertThat(
                        Designacion.de(
                                ABIERTO,
                                "docente.demo",
                                VICERRECTOR,
                                DOCENTE,
                                Optional.of(anterior),
                                AHORA))
                .isSameAs(anterior);
    }

    @Test
    void dadoOtroResponsable_cuandoSeDesigna_entoncesLoReemplaza() {
        Designacion anterior =
                new Designacion("2026-2", "docente.coordinador.demo", VICERRECTOR, AHORA);

        assertThat(
                        Designacion.de(
                                        ABIERTO,
                                        "docente.demo",
                                        VICERRECTOR,
                                        DOCENTE,
                                        Optional.of(anterior),
                                        AHORA)
                                .usuario())
                .isEqualTo("docente.demo");
    }
}
