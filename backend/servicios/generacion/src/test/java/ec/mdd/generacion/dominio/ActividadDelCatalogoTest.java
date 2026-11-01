package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Lo que exige cada actividad del catálogo (RN-22, RN-25, D-79), sin dobles. */
class ActividadDelCatalogoTest {

    private static final ProyectoDelDocente DE_INVESTIGACION =
            new ProyectoDelDocente("INV-2025-01", "Monitoreo", "INVESTIGACION");
    private static final ProyectoDelDocente DE_VINCULACION =
            new ProyectoDelDocente("VIN-2025-01", "Huertos", "VINCULACION");

    private static final ProyectosYCargosDelDocente SIN_NADA =
            new ProyectosYCargosDelDocente(List.of(), List.of());
    private static final ProyectosYCargosDelDocente INVESTIGADOR =
            new ProyectosYCargosDelDocente(List.of(DE_INVESTIGACION, DE_VINCULACION), List.of());
    private static final ProyectosYCargosDelDocente COORDINADOR =
            new ProyectosYCargosDelDocente(List.of(), List.of("COORDINADOR_CARRERA"));
    private static final ProyectosYCargosDelDocente VICERRECTOR =
            new ProyectosYCargosDelDocente(List.of(), List.of("VICERRECTOR"));

    private static final ActividadDelCatalogo PLANIFICAR =
            new ActividadDelCatalogo(
                    "140-b", "Planificar", Componente.OTRAS_DOCENCIA, List.of(), List.of());
    private static final ActividadDelCatalogo INVESTIGAR =
            new ActividadDelCatalogo(
                    "141-a",
                    "Investigar",
                    Componente.INVESTIGACION,
                    List.of("INVESTIGACION"),
                    List.of());
    private static final ActividadDelCatalogo COORDINAR =
            new ActividadDelCatalogo(
                    "143-d",
                    "Coordinar carreras",
                    Componente.GESTION,
                    List.of(),
                    List.of("COORDINADOR_CARRERA"));
    private static final ActividadDelCatalogo AUTORIDAD =
            new ActividadDelCatalogo(
                    "143-a",
                    "Rector o vicerrector",
                    Componente.GESTION,
                    List.of(),
                    List.of("RECTOR", "VICERRECTOR"));

    @Test
    void dadaUnaActividadSinExigencias_cuandoSeMira_entoncesCualquierDocenteLaPuedeTener() {
        assertThat(PLANIFICAR.exigeProyecto()).isFalse();
        assertThat(PLANIFICAR.motivoParaNoAsignar(SIN_NADA)).isEmpty();
        assertThat(PLANIFICAR.exigir(null, SIN_NADA)).isEmpty();
    }

    @Test
    void dadaUnaActividadSinExigencias_cuandoLlegaUnProyecto_entoncesLoRechaza() {
        assertThatThrownBy(() -> PLANIFICAR.exigir("INV-2025-01", INVESTIGADOR))
                .isInstanceOf(ProyectoNoValido.class);
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoElDocenteLoTiene_entoncesLoDevuelve() {
        assertThat(INVESTIGAR.exigeProyecto()).isTrue();
        assertThat(INVESTIGAR.exigir("INV-2025-01", INVESTIGADOR)).contains(DE_INVESTIGACION);
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoSeBuscanLosPosibles_entoncesSoloLosDeSuTipo() {
        assertThat(INVESTIGAR.proyectosPosibles(INVESTIGADOR)).containsExactly(DE_INVESTIGACION);
        assertThat(INVESTIGAR.motivoParaNoAsignar(INVESTIGADOR)).isEmpty();
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoNoLlegaProyecto_entoncesLoRechaza() {
        assertThatThrownBy(() -> INVESTIGAR.exigir(null, INVESTIGADOR))
                .isInstanceOf(ProyectoNoValido.class);
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoEsDeOtroTipo_entoncesLoRechaza() {
        assertThatThrownBy(() -> INVESTIGAR.exigir("VIN-2025-01", INVESTIGADOR))
                .isInstanceOf(ProyectoNoValido.class);
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoNoEsDelDocente_entoncesLoRechaza() {
        assertThatThrownBy(() -> INVESTIGAR.exigir("INV-2025-99", INVESTIGADOR))
                .isInstanceOf(ProyectoNoValido.class);
    }

    @Test
    void dadoUnDocenteSinProyectos_cuandoSeMira_entoncesDiceQueNoTieneNinguno() {
        assertThat(INVESTIGAR.proyectosPosibles(SIN_NADA)).isEmpty();
        assertThat(INVESTIGAR.motivoParaNoAsignar(SIN_NADA))
                .contains("El docente no tiene proyectos asignables de este tipo en el periodo.");
    }

    @Test
    void dadaUnaActividadConCargo_cuandoElDocenteLoTiene_entoncesSePuede() {
        assertThat(COORDINAR.exigeProyecto()).isFalse();
        assertThat(COORDINAR.motivoParaNoAsignar(COORDINADOR)).isEmpty();
        assertThat(COORDINAR.exigir(null, COORDINADOR)).isEmpty();
    }

    @Test
    void dadaUnaActividadConCargo_cuandoElDocenteNoLoTiene_entoncesLoDiceYLoRechaza() {
        assertThat(COORDINAR.motivoParaNoAsignar(SIN_NADA))
                .contains("El docente no tiene vigente el cargo que exige esta actividad.");
        assertThatThrownBy(() -> COORDINAR.exigir(null, SIN_NADA))
                .isInstanceOf(CargoNoVigente.class);
    }

    @Test
    void dadaUnaActividadConVariosCargos_cuandoElDocenteTieneUno_entoncesBasta() {
        assertThat(AUTORIDAD.exigir(null, VICERRECTOR)).isEmpty();
        assertThat(AUTORIDAD.motivoParaNoAsignar(VICERRECTOR)).isEmpty();
    }
}
