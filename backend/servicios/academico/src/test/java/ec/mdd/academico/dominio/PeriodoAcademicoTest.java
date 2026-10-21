package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El periodo académico, sus datos y su corrección (RF-13, FR-006, research §1), sin dobles. */
class PeriodoAcademicoTest {

    static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    static final LocalDate FIN = LocalDate.of(2027, 2, 28);

    static PeriodoAcademico periodo(EstadoPeriodo estado) {
        return new PeriodoAcademico("2026-2", "Periodo 2026-2", INICIO, FIN, estado);
    }

    @Test
    void dadoDatosValidos_cuandoSeRegistra_entoncesEmpiezaEnPlanificacion() {
        PeriodoAcademico nuevo = PeriodoAcademico.nuevo("2026-2", "Periodo 2026-2", INICIO, FIN);

        assertThat(nuevo).isEqualTo(periodo(EstadoPeriodo.PLANIFICACION));
    }

    @Test
    void dadoUnNombreConEspacios_cuandoSeRegistra_entoncesQuedaSinEllos() {
        assertThat(PeriodoAcademico.nuevo("2026-2", "  Periodo 2026-2 ", INICIO, FIN).nombre())
                .isEqualTo("Periodo 2026-2");
    }

    @Test
    void dadoUnCodigoConOtroFormato_cuandoSeRegistra_entoncesDatoInvalidoEnCodigo() {
        for (String codigo : new String[] {"2026 2", "", "periodo/2026", "x".repeat(21)}) {
            assertThatThrownBy(() -> PeriodoAcademico.nuevo(codigo, "Periodo", INICIO, FIN))
                    .isInstanceOf(DatoInvalido.class)
                    .extracting("campo", "motivo")
                    .containsExactly(
                            "codigo",
                            "El código solo admite letras, números, punto, guion y guion bajo,"
                                    + " hasta 20 caracteres");
        }
    }

    @Test
    void dadoUnNombreVacioOLargo_cuandoSeRegistra_entoncesDatoInvalidoEnNombre() {
        assertThatThrownBy(() -> PeriodoAcademico.nuevo("2026-2", "   ", INICIO, FIN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo", "motivo")
                .containsExactly("nombre", "El nombre no puede estar en blanco");
        assertThatThrownBy(() -> PeriodoAcademico.nuevo("2026-2", "n".repeat(121), INICIO, FIN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo", "motivo")
                .containsExactly("nombre", "El nombre no puede tener más de 120 caracteres");
        assertThat(PeriodoAcademico.nuevo("2026-2", "n".repeat(120), INICIO, FIN).nombre())
                .hasSize(120);
    }

    @Test
    void dadoUnFinQueNoEsPosteriorAlInicio_cuandoSeRegistra_entoncesDatoInvalidoEnFin() {
        assertThatThrownBy(
                        () ->
                                PeriodoAcademico.nuevo(
                                        "2026-2",
                                        "Periodo",
                                        LocalDate.of(2026, 3, 1),
                                        LocalDate.of(2026, 1, 1)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo", "motivo")
                .containsExactly(
                        "fin",
                        "La fecha de fin (2026-01-01) debe ser posterior a la de inicio"
                                + " (2026-03-01)");
        assertThatThrownBy(() -> PeriodoAcademico.nuevo("2026-2", "Periodo", INICIO, INICIO))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("fin");
    }

    @Test
    void dadoUnPeriodoAbierto_cuandoSeCorrige_entoncesCambiaSusDatosYDiceQueCambio() {
        PeriodoAcademico.Correccion correccion =
                periodo(EstadoPeriodo.VIGENTE)
                        .corregido("Periodo académico 2026-2", INICIO, LocalDate.of(2027, 3, 15));

        assertThat(correccion.periodo())
                .isEqualTo(
                        new PeriodoAcademico(
                                "2026-2",
                                "Periodo académico 2026-2",
                                INICIO,
                                LocalDate.of(2027, 3, 15),
                                EstadoPeriodo.VIGENTE));
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor("nombre", "Periodo 2026-2", "Periodo académico 2026-2"),
                        new CambioDeValor("fin", "2027-02-28", "2027-03-15"));
    }

    @Test
    void dadoLosMismosDatos_cuandoSeCorrige_entoncesNoHayCambios() {
        PeriodoAcademico.Correccion correccion =
                periodo(EstadoPeriodo.PLANIFICACION).corregido(" Periodo 2026-2 ", INICIO, FIN);

        assertThat(correccion.periodo()).isEqualTo(periodo(EstadoPeriodo.PLANIFICACION));
        assertThat(correccion.cambios()).isEmpty();
    }

    @Test
    void dadoDatosInvalidos_cuandoSeCorrige_entoncesDatoInvalido() {
        assertThatThrownBy(
                        () ->
                                periodo(EstadoPeriodo.PLANIFICACION)
                                        .corregido("Periodo", FIN, INICIO))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("fin");
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeCorrige_entoncesPeriodoCerrado() {
        assertThatThrownBy(
                        () -> periodo(EstadoPeriodo.CERRADO).corregido("Otro nombre", INICIO, FIN))
                .isInstanceOf(PeriodoCerrado.class)
                .hasMessageContaining("2026-2");
    }

    @Test
    void dadoCadaParDeEstados_cuandoSeCambia_entoncesSoloAvanzaUnPaso() {
        for (EstadoPeriodo origen : EstadoPeriodo.values()) {
            for (EstadoPeriodo destino : EstadoPeriodo.values()) {
                PeriodoAcademico periodo = periodo(origen);
                if (destino == origen) {
                    assertThat(periodo.conEstado(destino, true)).isEqualTo(periodo);
                } else if (origen == EstadoPeriodo.CERRADO) {
                    assertThatThrownBy(() -> periodo.conEstado(destino, true))
                            .isInstanceOf(PeriodoCerrado.class);
                } else if (Optional.of(destino).equals(origen.siguiente())) {
                    assertThat(periodo.conEstado(destino, true)).isEqualTo(periodo(destino));
                } else {
                    assertThatThrownBy(() -> periodo.conEstado(destino, true))
                            .isInstanceOf(TransicionNoPermitida.class)
                            .extracting("actual", "destino")
                            .containsExactly(origen, destino);
                }
            }
        }
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSalePlanificacion_entoncesPeriodoSinVersion() {
        assertThatThrownBy(
                        () ->
                                periodo(EstadoPeriodo.PLANIFICACION)
                                        .conEstado(EstadoPeriodo.EN_APROBACION, false))
                .isInstanceOf(PeriodoSinVersion.class)
                .hasMessageContaining("2026-2");
        assertThat(
                        periodo(EstadoPeriodo.PLANIFICACION)
                                .conEstado(EstadoPeriodo.PLANIFICACION, false))
                .isEqualTo(periodo(EstadoPeriodo.PLANIFICACION));
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSaltaAVigente_entoncesTransicionNoPermitida() {
        assertThatThrownBy(
                        () ->
                                periodo(EstadoPeriodo.PLANIFICACION)
                                        .conEstado(EstadoPeriodo.VIGENTE, false))
                .isInstanceOf(TransicionNoPermitida.class);
    }
}
