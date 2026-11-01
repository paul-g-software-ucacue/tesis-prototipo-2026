package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.puertos.entrada.ActividadPosible;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Lo que el formulario puede ofrecer para un docente (RN-22, RN-25). Con Mockito. */
class ConsultarActividadesPosiblesServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String DOCENTE = "0109990010";

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
                    "Coordinar",
                    Componente.GESTION,
                    List.of(),
                    List.of("COORDINADOR_CARRERA"));
    private static final ProyectoDelDocente PROYECTO =
            new ProyectoDelDocente("INV-2025-01", "Monitoreo", "INVESTIGACION");

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final DatosParaActividades actividades = mock(DatosParaActividades.class);
    private final ConsultarActividadesPosiblesServicio servicio =
            new ConsultarActividadesPosiblesServicio(datos, actividades);

    @Test
    void dadoElCatalogo_cuandoSeConsulta_entoncesLleganTodasEnSuOrden() {
        con(new ProyectosYCargosDelDocente(List.of(), List.of()));

        assertThat(servicio.posibles(PERIODO, DOCENTE))
                .extracting(uno -> uno.actividad().codigo())
                .containsExactly("140-b", "141-a", "143-d");
    }

    @Test
    void dadoUnDocenteSinProyectosNiCargos_cuandoSeConsulta_entoncesCadaUnaDiceSuMotivo() {
        con(new ProyectosYCargosDelDocente(List.of(), List.of()));

        List<ActividadPosible> posibles = servicio.posibles(PERIODO, DOCENTE);

        assertThat(posibles.get(0).disponible()).isTrue();
        assertThat(posibles.get(0).motivo()).isEmpty();
        assertThat(posibles.get(1).disponible()).isFalse();
        assertThat(posibles.get(1).motivo().orElseThrow()).contains("proyectos");
        assertThat(posibles.get(2).disponible()).isFalse();
        assertThat(posibles.get(2).motivo().orElseThrow()).contains("cargo");
    }

    @Test
    void dadoUnDocenteConProyectoYCargo_cuandoSeConsulta_entoncesEstanDisponibles() {
        con(new ProyectosYCargosDelDocente(List.of(PROYECTO), List.of("COORDINADOR_CARRERA")));

        List<ActividadPosible> posibles = servicio.posibles(PERIODO, DOCENTE);

        assertThat(posibles).allMatch(ActividadPosible::disponible);
        assertThat(posibles.get(1).proyectos()).containsExactly(PROYECTO);
        assertThat(posibles.get(0).proyectos()).isEmpty();
    }

    @Test
    void dadoUnDocenteQueNoEstaActivo_cuandoSeConsulta_entoncesDocenteNoActivo() {
        con(new ProyectosYCargosDelDocente(List.of(), List.of()));
        when(datos.docentesActivos()).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.posibles(PERIODO, DOCENTE))
                .isInstanceOf(DocenteNoActivo.class);
    }

    @Test
    void dadoQueDocentesNoLoEncuentra_cuandoSeConsulta_entoncesDocenteNoActivo() {
        con(new ProyectosYCargosDelDocente(List.of(), List.of()));
        when(actividades.proyectosYCargos(DOCENTE, PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.posibles(PERIODO, DOCENTE))
                .isInstanceOf(DocenteNoActivo.class);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinAbrir() {
        con(new ProyectosYCargosDelDocente(List.of(), List.of()));
        when(actividades.catalogo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.posibles(PERIODO, DOCENTE))
                .isInstanceOf(PeriodoSinAbrir.class);
    }

    private void con(ProyectosYCargosDelDocente delDocente) {
        when(datos.docentesActivos())
                .thenReturn(
                        List.of(
                                new DocenteActivo(
                                        DOCENTE,
                                        "PEREZ LUIS",
                                        Dedicacion.TC,
                                        40,
                                        new FormacionDelDocente(
                                                new TituloDeTercerNivel("Título", "06", "061"),
                                                List.of(),
                                                List.of(),
                                                List.of()))));
        when(actividades.catalogo(PERIODO))
                .thenReturn(Optional.of(List.of(PLANIFICAR, INVESTIGAR, COORDINAR)));
        when(actividades.proyectosYCargos(DOCENTE, PERIODO)).thenReturn(Optional.of(delDocente));
    }
}
