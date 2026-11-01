package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los totales de un docente en todas las carreras y unidades (RF-27, D-01). */
class CargaHorariaTest {

    private static final String DOCENTE = "0109990010";
    private static final String OTRO_DOCENTE = "0109990020";

    private static final AsignaturaParalelo DE_TDS = paralelo("BD-301", "A", "TDS", "8", false);
    private static final AsignaturaParalelo DE_AGRO =
            paralelo("SOC-101", "B", "AGRO", "3.25", false);
    private static final AsignaturaParalelo RETIRADO = paralelo("MAT-101", "C", "TDS", "6", true);
    private static final List<AsignaturaParalelo> OFERTA = List.of(DE_TDS, DE_AGRO, RETIRADO);

    @Test
    void dadasAsignacionesEnDosCarreras_cuandoSeCalcula_entoncesSumaLosCincoComponentes() {
        CargaHoraria carga =
                CargaHoraria.calcular(
                        DOCENTE,
                        List.of(propuesta(DE_TDS, DOCENTE), propuesta(DE_AGRO, DOCENTE)),
                        List.of(
                                actividad("TDS", "140-b", Componente.OTRAS_DOCENCIA, "2.5"),
                                actividad("AGRO", "140-g", Componente.OTRAS_DOCENCIA, "1.5"),
                                actividad("TDS", "141-a", Componente.INVESTIGACION, "3"),
                                actividad("AGRO", "142-a", Componente.VINCULACION, "2"),
                                actividad("TDS", "143-k", Componente.GESTION, "1.25")),
                        OFERTA);

        assertThat(carga.clase()).isEqualByComparingTo("11.25");
        assertThat(carga.otrasDocencia()).isEqualByComparingTo("4");
        assertThat(carga.investigacion()).isEqualByComparingTo("3");
        assertThat(carga.vinculacion()).isEqualByComparingTo("2");
        assertThat(carga.gestion()).isEqualByComparingTo("1.25");
        assertThat(carga.asignadas()).isEqualByComparingTo("21.5");
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeCalcula_entoncesNoCuenta() {
        CargaHoraria carga =
                CargaHoraria.calcular(
                        DOCENTE, List.of(propuesta(RETIRADO, DOCENTE)), List.of(), OFERTA);

        assertThat(carga.clase()).isEqualByComparingTo("0");
    }

    @Test
    void dadasHorasQueCambiaron_cuandoSeCalcula_entoncesUsaLasActuales() {
        AsignaturaParalelo cambiado = paralelo("BD-301", "A", "TDS", "5", false);

        CargaHoraria carga =
                CargaHoraria.calcular(
                        DOCENTE, List.of(propuesta(DE_TDS, DOCENTE)), List.of(), List.of(cambiado));

        assertThat(carga.clase()).isEqualByComparingTo("5");
    }

    @Test
    void dadoOtroDocente_cuandoSeCalcula_entoncesNoCuentaLoSuyo() {
        CargaHoraria carga =
                CargaHoraria.calcular(
                        DOCENTE,
                        List.of(propuesta(DE_TDS, OTRO_DOCENTE)),
                        List.of(actividadDe(OTRO_DOCENTE, "TDS", "140-b", Componente.GESTION, "4")),
                        OFERTA);

        assertThat(carga.asignadas()).isEqualByComparingTo("0");
    }

    @Test
    void conMasHorasQueLasContratadas_cuandoSeCalcula_entoncesDiceElExceso() {
        CargaHoraria carga = conGestion("33");

        assertThat(carga.disponibles(20)).isEqualByComparingTo("0");
        assertThat(carga.exceso(20)).isEqualByComparingTo("13");
    }

    @Test
    void conMenosHorasQueLasContratadas_cuandoSeCalcula_entoncesNoHayExceso() {
        CargaHoraria carga = conGestion("7.5");

        assertThat(carga.disponibles(20)).isEqualByComparingTo("12.5");
        assertThat(carga.exceso(20)).isEqualByComparingTo("0");
    }

    @Test
    void conCeroHorasContratadas_cuandoSeCalcula_entoncesLoAsignadoEsExceso() {
        CargaHoraria carga = conGestion("4");

        assertThat(carga.disponibles(0)).isEqualByComparingTo("0");
        assertThat(carga.exceso(0)).isEqualByComparingTo("4");
    }

    @Test
    void dadasClaseYOtras_cuandoSePideLaProporcion_entoncesUsaSoloEsas() {
        CargaHoraria carga =
                CargaHoraria.calcular(
                        DOCENTE,
                        List.of(propuesta(DE_TDS, DOCENTE)),
                        List.of(
                                actividad("TDS", "140-b", Componente.OTRAS_DOCENCIA, "4"),
                                actividad("TDS", "141-a", Componente.INVESTIGACION, "10"),
                                actividad("TDS", "142-a", Componente.VINCULACION, "10"),
                                actividad("TDS", "143-k", Componente.GESTION, "10")),
                        OFERTA);

        assertThat(carga.proporcion()).contains(new Proporcion(67, 33));
    }

    @Test
    void soloConGestion_cuandoSePideLaProporcion_entoncesNoHay() {
        assertThat(conGestion("4").proporcion()).isEmpty();
    }

    @Test
    void conLosMismosDatos_cuandoSeCuenta_entoncesCoincideConLasHorasDeLaSugerencia() {
        List<Propuesta> propuestas =
                List.of(
                        propuesta(DE_TDS, DOCENTE),
                        propuesta(DE_AGRO, DOCENTE),
                        propuesta(RETIRADO, DOCENTE),
                        propuesta(DE_AGRO, OTRO_DOCENTE));
        List<ActividadNoLectiva> actividades =
                List.of(
                        actividad("TDS", "140-b", Componente.OTRAS_DOCENCIA, "2.5"),
                        actividad("AGRO", "143-k", Componente.GESTION, "1.25"));
        AsignaturaParalelo sinPropuesta = paralelo("ART-101", "Z", "TDS", "2", false);

        HorasDelDocente horas =
                HorasDelPeriodo.calcular(propuestas, actividades, OFERTA, sinPropuesta)
                        .get(DOCENTE);

        assertThat(CargaHoraria.calcular(DOCENTE, propuestas, actividades, OFERTA).asignadas())
                .isEqualByComparingTo(horas.asignadas());
    }

    private static CargaHoraria conGestion(String horas) {
        return CargaHoraria.calcular(
                DOCENTE,
                List.of(),
                List.of(actividad("TDS", "143-k", Componente.GESTION, horas)),
                OFERTA);
    }

    private static ActividadNoLectiva actividad(
            String carrera, String codigo, Componente componente, String horas) {
        return actividadDe(DOCENTE, carrera, codigo, componente, horas);
    }

    private static ActividadNoLectiva actividadDe(
            String docente, String carrera, String codigo, Componente componente, String horas) {
        return new ActividadNoLectiva(
                carrera,
                docente,
                "PEREZ LUIS",
                codigo,
                "Una actividad",
                componente,
                null,
                null,
                new BigDecimal(horas),
                Instant.parse("2026-11-01T15:00:00Z"),
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

    private static Propuesta propuesta(AsignaturaParalelo paralelo, String docente) {
        return new Propuesta(
                paralelo.asignatura().codigo(),
                paralelo.paralelo(),
                paralelo.carrera(),
                docente,
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.parse("2026-10-31T15:00:00Z"),
                "coordinacion.demo");
    }
}
