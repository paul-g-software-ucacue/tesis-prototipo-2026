package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Etapa;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Rige la última cargada o, si no hay, la configuración por defecto (FR-001, FR-020). */
class ConsultarConfiguracionServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String PLANTILLA = new PlantillaEnRecursos().texto();
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RepositorioConfiguracion repositorio = mock(RepositorioConfiguracion.class);
    private final ConsultarConfiguracionServicio servicio =
            new ConsultarConfiguracionServicio(
                    datos, new LectorYaml(), repositorio, new PlantillaEnRecursos());

    @BeforeEach
    void preparar() {
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(repositorio.vigente(PERIODO)).thenReturn(Optional.empty());
    }

    @Test
    void sinArchivo_cuandoSeConsulta_entoncesRigeLaDePorDefecto() {
        ConfiguracionVigente vigente = servicio.dePeriodo(PERIODO);

        assertThat(vigente.carga()).isEmpty();
        assertThat(vigente.modificable()).isTrue();
        assertThat(vigente.configuracion().etapas()).hasSize(9);
        assertThat(vigente.configuracion().firmas()).hasSize(3);
    }

    @Test
    void conArchivo_cuandoSeConsulta_entoncesRigeElCargado() {
        String cambiado = PLANTILLA.replace("plazo_dias: 15", "plazo_dias: 20");
        CargaGuardada carga = new CargaGuardada(2, cambiado, "admin.demo", AHORA);
        when(repositorio.vigente(PERIODO)).thenReturn(Optional.of(carga));

        ConfiguracionVigente vigente = servicio.dePeriodo(PERIODO);

        assertThat(vigente.carga()).contains(carga);
        assertThat(vigente.configuracion().etapas())
                .filteredOn(etapa -> etapa.tipo() == TipoDeEtapa.BORRADOR_CARRERA)
                .extracting(Etapa::plazoDias)
                .containsExactly(OptionalInt.of(20));
    }

    @Test
    void dadoUnPeriodoEnCurso_cuandoSeConsulta_entoncesSeVeYNoEsModificable() {
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("VIGENTE")));

        assertThat(servicio.dePeriodo(PERIODO).modificable()).isFalse();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesSeRechaza() {
        when(datos.estado(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.dePeriodo(PERIODO))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }

    @Test
    void dadaLaPlantilla_cuandoSePide_entoncesEsElRecursoTalCual() {
        assertThat(servicio.plantilla()).isEqualTo(PLANTILLA);
    }

    @Test
    void dadoUnArchivoGuardadoQueYaNoEsValido_cuandoSeConsulta_entoncesFallaSinInventarNada() {
        when(repositorio.vigente(PERIODO))
                .thenReturn(Optional.of(new CargaGuardada(1, "etapas: {}\n", "admin.demo", AHORA)));

        assertThatThrownBy(() -> servicio.dePeriodo(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }
}
