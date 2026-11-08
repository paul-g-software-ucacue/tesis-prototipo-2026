package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas del filtro de la bitácora (data-model.md, FR-002, FR-003, FR-010, research §3). */
class FiltroDeBitacoraTest {

    private static final LocalDate DIA = LocalDate.parse("2026-11-07");

    @Test
    void dadoNingunFiltro_cuandoSeConstruye_entoncesNoAcotaNada() {
        FiltroDeBitacora filtro = FiltroDeBitacora.de(null, null, null, null, null, null, null);

        assertThat(filtro.desde()).isEmpty();
        assertThat(filtro.hasta()).isEmpty();
        assertThat(filtro.usuario()).isEmpty();
        assertThat(filtro.servicio()).isEmpty();
        assertThat(filtro.accion()).isEmpty();
        assertThat(filtro.identificador()).isEmpty();
        assertThat(filtro.periodo()).isEmpty();
    }

    @Test
    void dadoTodosLosFiltros_cuandoSeConstruye_entoncesLosGuardaTalCual() {
        FiltroDeBitacora filtro =
                FiltroDeBitacora.de(
                        DIA,
                        DIA,
                        "coordinacion.demo",
                        "generacion",
                        "ACTIVIDAD_GUARDADA",
                        "140-b",
                        "2026-I");

        assertThat(filtro.usuario()).contains("coordinacion.demo");
        assertThat(filtro.servicio()).contains("generacion");
        assertThat(filtro.accion()).contains("ACTIVIDAD_GUARDADA");
        assertThat(filtro.identificador()).contains("140-b");
        assertThat(filtro.periodo()).contains("2026-I");
    }

    @Test
    void dadoUnDia_cuandoSeConvierte_entoncesEsElDiaCompletoEnHoraDeEcuador() {
        FiltroDeBitacora filtro = FiltroDeBitacora.de(DIA, DIA, null, null, null, null, null);

        assertThat(filtro.desde()).contains(Instant.parse("2026-11-07T05:00:00Z"));
        assertThat(filtro.hasta()).contains(Instant.parse("2026-11-08T05:00:00Z"));
    }

    @Test
    void dadoSoloDesde_cuandoSeConvierte_entoncesNoTieneLimiteSuperior() {
        FiltroDeBitacora filtro = FiltroDeBitacora.de(DIA, null, null, null, null, null, null);

        assertThat(filtro.desde()).isPresent();
        assertThat(filtro.hasta()).isEmpty();
    }

    @Test
    void dadoSoloHasta_cuandoSeConvierte_entoncesNoTieneLimiteInferior() {
        FiltroDeBitacora filtro = FiltroDeBitacora.de(null, DIA, null, null, null, null, null);

        assertThat(filtro.desde()).isEmpty();
        assertThat(filtro.hasta()).contains(Instant.parse("2026-11-08T05:00:00Z"));
    }

    @Test
    void dadoHastaAntesDeDesde_cuandoSeConstruye_entoncesFallaEnHasta() {
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        DIA, DIA.minusDays(1), null, null, null, null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> {
                            assertThat(error.campo()).isEqualTo("hasta");
                            assertThat(error.motivo())
                                    .isEqualTo(
                                            "La fecha hasta no puede ser anterior a la fecha"
                                                    + " desde.");
                        });
    }

    @Test
    void dadoUsuarioDeUnoYDe64_cuandoSeConstruye_entoncesSeAdmite() {
        assertThat(FiltroDeBitacora.de(null, null, "a", null, null, null, null).usuario())
                .contains("a");
        assertThat(
                        FiltroDeBitacora.de(null, null, "u".repeat(64), null, null, null, null)
                                .usuario())
                .isPresent();
    }

    @Test
    void dadoUsuarioVacioODe65_cuandoSeConstruye_entoncesFallaEnUsuario() {
        assertThatThrownBy(() -> FiltroDeBitacora.de(null, null, "", null, null, null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("usuario"));
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        null, null, "u".repeat(65), null, null, null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("usuario"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "generacion",
                "academico",
                "docentes",
                "normativa",
                "roles",
                "validacionocs"
            })
    void dadoUnoDeLosSeisServicios_cuandoSeConstruye_entoncesSeAdmite(String servicio) {
        assertThat(FiltroDeBitacora.de(null, null, null, servicio, null, null, null).servicio())
                .contains(servicio);
    }

    @Test
    void dadoOtroServicio_cuandoSeConstruye_entoncesFallaEnServicio() {
        assertThatThrownBy(
                        () -> FiltroDeBitacora.de(null, null, null, "usuarios", null, null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("servicio"));
    }

    @Test
    void dadoAccionSinServicio_cuandoSeConstruye_entoncesFallaEnAccion() {
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        null, null, null, null, "ACTIVIDAD_GUARDADA", null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("accion"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "actividad_guardada", "ACTIVIDAD-GUARDADA", "1ACCION"})
    void dadoUnaAccionSinLaForma_cuandoSeConstruye_entoncesFallaEnAccion(String accion) {
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        null, null, null, "generacion", accion, null, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("accion"));
    }

    @Test
    void dadoUnaAccionDe40_cuandoSeConstruye_entoncesSeAdmite() {
        String accion = "A" + "B".repeat(39);

        assertThat(FiltroDeBitacora.de(null, null, null, "generacion", accion, null, null).accion())
                .contains(accion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"140-b", "0102030405", "DOC/0102030405", "palabra_clave.1", "3"})
    void dadoUnIdentificadorValido_cuandoSeConstruye_entoncesSeAdmite(String identificador) {
        assertThat(
                        FiltroDeBitacora.de(null, null, null, null, null, identificador, null)
                                .identificador())
                .contains(identificador);
    }

    @Test
    void dadoUnIdentificadorDe64_cuandoSeConstruye_entoncesSeAdmite() {
        assertThat(
                        FiltroDeBitacora.de(null, null, null, null, null, "a".repeat(64), null)
                                .identificador())
                .isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "con espacio", "tildé", "ñandú", "a'b", "a%b"})
    void
            dadoUnIdentificadorConCaracteresNoPermitidos_cuandoSeConstruye_entoncesFallaEnIdentificador(
                    String identificador) {
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        null, null, null, null, null, identificador, null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("identificador"));
    }

    @Test
    void dadoUnIdentificadorDe65_cuandoSeConstruye_entoncesFallaEnIdentificador() {
        assertThatThrownBy(
                        () ->
                                FiltroDeBitacora.de(
                                        null, null, null, null, null, "a".repeat(65), null))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("identificador"));
    }

    @Test
    void dadoUnPeriodoDe20_cuandoSeConstruye_entoncesSeAdmite() {
        assertThat(
                        FiltroDeBitacora.de(null, null, null, null, null, null, "p".repeat(20))
                                .periodo())
                .isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2026 I", "2026/I", "ppppppppppppppppppppp"})
    void dadoUnPeriodoSinLaForma_cuandoSeConstruye_entoncesFallaEnPeriodo(String periodo) {
        assertThatThrownBy(() -> FiltroDeBitacora.de(null, null, null, null, null, null, periodo))
                .isInstanceOfSatisfying(
                        FiltroDeBitacoraInvalido.class,
                        error -> assertThat(error.campo()).isEqualTo("periodo"));
    }
}
