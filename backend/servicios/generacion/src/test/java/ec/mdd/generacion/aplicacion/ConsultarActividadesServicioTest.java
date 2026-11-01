package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ActividadesDeCarrera;
import ec.mdd.generacion.puertos.entrada.ActividadesDelDocente;
import ec.mdd.generacion.puertos.entrada.ActividadesDelPeriodo;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Las actividades por carrera y docente, con su suma (RF-26, D-64). Con Mockito. */
class ConsultarActividadesServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Optional<String> TODAS = Optional.empty();

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final ConsultarActividadesServicio servicio =
            new ConsultarActividadesServicio(datos, distributivos);

    @Test
    void dadoQueNoHayDistributivo_cuandoSeConsulta_entoncesCreadoEnFalso() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        ActividadesDelPeriodo actividades = servicio.actividades(PERIODO, Optional.of("TDS"));

        assertThat(actividades.creado()).isFalse();
        assertThat(actividades.carreras()).isEmpty();
    }

    @Test
    void dadasActividades_cuandoSeConsulta_entoncesVanPorCarreraYDocenteConSuSuma() {
        con(
                List.of("AGRO", "TDS"),
                actividad("TDS", "2", "ZAMORA ANA", "140-b", "4.25"),
                actividad("TDS", "2", "ZAMORA ANA", "140-g", "2.5"),
                actividad("TDS", "1", "ALVAREZ LUIS", "140-b", "3"),
                actividad("AGRO", "1", "ALVAREZ LUIS", "140-g", "1"));

        ActividadesDelPeriodo actividades = servicio.actividades(PERIODO, TODAS);

        assertThat(actividades.creado()).isTrue();
        assertThat(actividades.carreras())
                .extracting(ActividadesDeCarrera::carrera)
                .containsExactly("AGRO", "TDS");
        List<ActividadesDelDocente> tds = actividades.carreras().get(1).docentes();
        assertThat(tds)
                .extracting(ActividadesDelDocente::nombreDocente)
                .containsExactly("ALVAREZ LUIS", "ZAMORA ANA");
        assertThat(tds.get(1).actividades()).hasSize(2);
        assertThat(tds.get(1).totales().otrasDocencia()).isEqualByComparingTo("6.75");
    }

    @Test
    void dadoUnDocenteEnDosCarreras_cuandoSeConsulta_entoncesSusTotalesSumanLasDos() {
        AsignaturaParalelo clase =
                new AsignaturaParalelo(
                        new AsignaturaOfertada("SOC-101", "Asignatura", "06", "061"),
                        "A",
                        "AGRO",
                        new BigDecimal("5"),
                        false,
                        false);
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(clase)));
        Propuesta propuesta =
                new Propuesta(
                        "SOC-101",
                        "A",
                        "AGRO",
                        "1",
                        "ALVAREZ LUIS",
                        new Justificacion(TipoDeJustificacion.DISPONIBILIDAD, null),
                        Instant.parse("2026-10-31T15:00:00Z"),
                        "coordinacion.demo");
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                new VersionDistributivo(
                                        UUID.randomUUID(),
                                        1,
                                        EstadoDeVersion.EDITABLE,
                                        List.of(ParcialCarrera.nuevo("TDS")),
                                        List.of(propuesta),
                                        List.of(
                                                actividad("TDS", "1", "ALVAREZ LUIS", "140-b", "3"),
                                                actividad(
                                                        "AGRO",
                                                        "1",
                                                        "ALVAREZ LUIS",
                                                        "140-g",
                                                        "1")))));

        ActividadesDelDocente docente =
                servicio.actividades(PERIODO, Optional.of("TDS"))
                        .carreras()
                        .getFirst()
                        .docentes()
                        .getFirst();

        assertThat(docente.actividades()).hasSize(1);
        assertThat(docente.totales().clase()).isEqualByComparingTo("5");
        assertThat(docente.totales().otrasDocencia()).isEqualByComparingTo("4");
    }

    @Test
    void dadoUnParcialSinActividades_cuandoSeConsulta_entoncesApareceVacio() {
        con(List.of("TDS"));

        assertThat(servicio.actividades(PERIODO, TODAS).carreras().getFirst().docentes()).isEmpty();
    }

    @Test
    void dadaUnaCarrera_cuandoSeConsulta_entoncesSoloVeLaSuya() {
        con(
                List.of("AGRO", "TDS"),
                actividad("TDS", "1", "ALVAREZ LUIS", "140-b", "3"),
                actividad("AGRO", "2", "ZAMORA ANA", "140-g", "1"));

        assertThat(servicio.actividades(PERIODO, Optional.of("TDS")).carreras())
                .singleElement()
                .satisfies(
                        carrera -> {
                            assertThat(carrera.carrera()).isEqualTo("TDS");
                            assertThat(carrera.docentes()).hasSize(1);
                        });
    }

    @Test
    void dadaLaCarreraDelCoordinadorSinParcial_cuandoSeConsulta_entoncesApareceIgual() {
        con(List.of("AGRO"));

        assertThat(servicio.actividades(PERIODO, Optional.of("TDS")).carreras())
                .extracting(ActividadesDeCarrera::carrera)
                .containsExactly("TDS");
    }

    private void con(List<String> carreras, ActividadNoLectiva... actividades) {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                new VersionDistributivo(
                                        UUID.randomUUID(),
                                        1,
                                        EstadoDeVersion.EDITABLE,
                                        carreras.stream().map(ParcialCarrera::nuevo).toList(),
                                        List.of(),
                                        List.of(actividades))));
    }

    private static ActividadNoLectiva actividad(
            String carrera, String docente, String nombre, String codigo, String horas) {
        return new ActividadNoLectiva(
                carrera,
                docente,
                nombre,
                codigo,
                "Una actividad",
                Componente.OTRAS_DOCENCIA,
                null,
                null,
                new BigDecimal(horas),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
