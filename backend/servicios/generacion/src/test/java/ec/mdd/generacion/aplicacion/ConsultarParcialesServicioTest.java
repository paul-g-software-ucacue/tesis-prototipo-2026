package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ParcialConAvance;
import ec.mdd.generacion.puertos.entrada.ParcialesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Los parciales de un periodo y su avance (RF-24), con los puertos simulados con Mockito. */
class ConsultarParcialesServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Optional<String> TODAS = Optional.empty();
    private static final String DOCENTE = "0109990010";

    private static final AsignaturaParalelo TDS_A = paralelo("POO-204", "A", "TDS", "4", false);
    private static final AsignaturaParalelo TDS_B = paralelo("POO-204", "B", "TDS", "4", false);
    private static final AsignaturaParalelo AGRO_A = paralelo("SUE-101", "A", "AGRO", "4", false);

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final ConsultarParcialesServicio servicio =
            new ConsultarParcialesServicio(datos, distributivos);

    @Test
    void dadasPropuestas_cuandoSeConsulta_entoncesCadaCarreraTieneSuAvance() {
        con(List.of(TDS_A, TDS_B, AGRO_A), version(List.of("AGRO", "TDS"), propuesta(TDS_A)), 40);

        ParcialesDelPeriodo parciales = servicio.parciales(PERIODO, TODAS);

        assertThat(parciales.creado()).isTrue();
        assertThat(parciales.parciales())
                .extracting(uno -> uno.parcial().carrera())
                .containsExactly("AGRO", "TDS");
        ParcialConAvance tds = parciales.parciales().get(1);
        assertThat(tds.parcial().estado()).isEqualTo(EstadoDelParcial.EN_ELABORACION);
        assertThat(tds.avance().ofertadas()).isEqualTo(2);
        assertThat(tds.avance().conPropuesta()).isEqualTo(1);
        assertThat(tds.avance().completo()).isFalse();
        assertThat(tds.propuestas()).hasSize(1);
    }

    @Test
    void dadasTodasLasPropuestas_cuandoSeConsulta_entoncesElParcialEstaCompleto() {
        con(List.of(TDS_A, TDS_B), version(List.of("TDS"), propuesta(TDS_A), propuesta(TDS_B)), 40);

        assertThat(servicio.parciales(PERIODO, TODAS).parciales().getFirst().avance().completo())
                .isTrue();
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSeConsulta_entoncesCreadoEnFalso() {
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(TDS_A)));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        ParcialesDelPeriodo parciales = servicio.parciales(PERIODO, TODAS);

        assertThat(parciales.creado()).isFalse();
        assertThat(parciales.parciales()).isEmpty();
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesPeriodoNoEncontrado() {
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.parciales(PERIODO, TODAS))
                .isInstanceOf(PeriodoNoEncontrado.class);
        verify(distributivos, never()).vigenteDe(any());
    }

    @Test
    void dadaUnaPropuestaDeUnParaleloRetirado_cuandoSeConsulta_entoncesSeMarcaYNoCuenta() {
        AsignaturaParalelo retirado = paralelo("POO-204", "A", "TDS", "4", true);
        con(List.of(retirado, TDS_B), version(List.of("TDS"), propuesta(retirado)), 40);

        ParcialConAvance tds = servicio.parciales(PERIODO, TODAS).parciales().getFirst();

        assertThat(tds.avance().ofertadas()).isEqualTo(1);
        assertThat(tds.avance().conPropuesta()).isZero();
        assertThat(tds.propuestas().getFirst().retirada()).isTrue();
    }

    @Test
    void dadaUnaPropuestaQueYaNoEstaEnLaOferta_cuandoSeConsulta_entoncesSeMarcaRetirada() {
        con(List.of(TDS_B), version(List.of("TDS"), propuesta(TDS_A)), 40);

        PropuestaVista propuesta =
                servicio.parciales(PERIODO, TODAS).parciales().getFirst().propuestas().getFirst();

        assertThat(propuesta.retirada()).isTrue();
        assertThat(propuesta.horasSuficientes()).isTrue();
    }

    @Test
    void dadoUnParaleloNuevo_cuandoSeConsulta_entoncesQuedaPendiente() {
        con(List.of(TDS_A, TDS_B), version(List.of("TDS"), propuesta(TDS_A)), 40);

        assertThat(servicio.parciales(PERIODO, TODAS).parciales().getFirst().avance().completo())
                .isFalse();
    }

    @Test
    void dadaUnaCarreraQueOfertoDespues_cuandoSeConsulta_entoncesApareceSinPropuestas() {
        con(List.of(TDS_A, AGRO_A), version(List.of("TDS")), 40);

        ParcialConAvance agro = servicio.parciales(PERIODO, TODAS).parciales().getFirst();

        assertThat(agro.parcial().carrera()).isEqualTo("AGRO");
        assertThat(agro.parcial().estado()).isEqualTo(EstadoDelParcial.EN_ELABORACION);
        assertThat(agro.avance().ofertadas()).isEqualTo(1);
    }

    @Test
    void dadaUnaCarrera_cuandoSeConsulta_entoncesSoloVeSuParcial() {
        con(List.of(TDS_A, AGRO_A), version(List.of("AGRO", "TDS")), 40);

        assertThat(servicio.parciales(PERIODO, Optional.of("TDS")).parciales())
                .extracting(uno -> uno.parcial().carrera())
                .containsExactly("TDS");
    }

    @Test
    void dadoUnDocenteConHoras_cuandoSeConsulta_entoncesLeAlcanzan() {
        con(List.of(TDS_A), version(List.of("TDS"), propuesta(TDS_A)), 4);

        assertThat(
                        servicio.parciales(PERIODO, TODAS)
                                .parciales()
                                .getFirst()
                                .propuestas()
                                .getFirst()
                                .horasSuficientes())
                .isTrue();
    }

    @Test
    void dadoUnDocenteSinHoras_cuandoSeConsulta_entoncesLoAvisa() {
        con(List.of(TDS_A), version(List.of("TDS"), propuesta(TDS_A)), 3);

        assertThat(
                        servicio.parciales(PERIODO, TODAS)
                                .parciales()
                                .getFirst()
                                .propuestas()
                                .getFirst()
                                .horasSuficientes())
                .isFalse();
    }

    @Test
    void dadoQueDosPropuestasLoPasanDeHoras_cuandoSeConsulta_entoncesLoAvisaEnAmbas() {
        con(List.of(TDS_A, TDS_B), version(List.of("TDS"), propuesta(TDS_A), propuesta(TDS_B)), 6);

        assertThat(servicio.parciales(PERIODO, TODAS).parciales().getFirst().propuestas())
                .extracting(PropuestaVista::horasSuficientes)
                .containsExactly(false, false);
    }

    @Test
    void dadasActividadesDelDocente_cuandoSeConsulta_entoncesCuentanParaElAviso() {
        ActividadNoLectiva gestion =
                new ActividadNoLectiva(
                        "AGRO",
                        DOCENTE,
                        "PEREZ LUIS",
                        "143-k",
                        "Eventos",
                        Componente.GESTION,
                        null,
                        null,
                        new BigDecimal("37"),
                        Instant.parse("2026-11-01T15:00:00Z"),
                        "coordinacion.demo");
        VersionDistributivo conActividad =
                new VersionDistributivo(
                        UUID.randomUUID(),
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(ParcialCarrera.nuevo("TDS")),
                        List.of(propuesta(TDS_A)),
                        List.of(gestion));
        con(List.of(TDS_A), conActividad, 40);

        assertThat(
                        servicio.parciales(PERIODO, TODAS)
                                .parciales()
                                .getFirst()
                                .propuestas()
                                .getFirst()
                                .horasSuficientes())
                .isFalse();
    }

    @Test
    void dadoUnDocenteQueYaNoEstaActivo_cuandoSeConsulta_entoncesNoAvisaDeLasHoras() {
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(TDS_A)));
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(List.of("TDS"), propuesta(TDS_A))));
        when(datos.docentesActivos()).thenReturn(List.of());

        assertThat(
                        servicio.parciales(PERIODO, TODAS)
                                .parciales()
                                .getFirst()
                                .propuestas()
                                .getFirst()
                                .horasSuficientes())
                .isTrue();
    }

    private void con(
            List<AsignaturaParalelo> oferta, VersionDistributivo version, int horasContratadas) {
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(oferta));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(version));
        when(datos.docentesActivos())
                .thenReturn(
                        List.of(
                                new DocenteActivo(
                                        DOCENTE,
                                        "PEREZ LUIS",
                                        Dedicacion.TC,
                                        horasContratadas,
                                        new FormacionDelDocente(
                                                new TituloDeTercerNivel("Título", "06", "061"),
                                                List.of(),
                                                List.of(),
                                                List.of()))));
    }

    private static VersionDistributivo version(List<String> carreras, Propuesta... propuestas) {
        return new VersionDistributivo(
                UUID.randomUUID(),
                1,
                EstadoDeVersion.EDITABLE,
                carreras.stream().map(ParcialCarrera::nuevo).toList(),
                List.of(propuestas));
    }

    private static Propuesta propuesta(AsignaturaParalelo paralelo) {
        return new Propuesta(
                paralelo.asignatura().codigo(),
                paralelo.paralelo(),
                paralelo.carrera(),
                DOCENTE,
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.parse("2026-10-31T15:00:00Z"),
                "coordinacion.demo");
    }

    private static AsignaturaParalelo paralelo(
            String asignatura, String paralelo, String carrera, String horas, boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada(asignatura, "Asignatura", "06", "061"),
                paralelo,
                carrera,
                new BigDecimal(horas),
                false,
                retirada);
    }
}
