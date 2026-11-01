package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Las horas de cada docente salen de las propuestas y de la oferta de ese momento (RF-25). */
class HorasDelPeriodoTest {

    private static final String DOCENTE = "0109990010";
    private static final String OTRO_DOCENTE = "0109990020";

    private static final AsignaturaParalelo MIRADO = paralelo("POO-204", "A", "TDS", "4", false);
    private static final AsignaturaParalelo DE_TDS = paralelo("BD-301", "A", "TDS", "2.5", false);
    private static final AsignaturaParalelo DE_OTRA =
            paralelo("SOC-101", "B", "AGRO", "3.25", false);

    @Test
    void dadasVariasPropuestas_cuandoSeCalcula_entoncesSumaPorDocenteConDecimales() {
        Map<String, HorasDelDocente> horas =
                HorasDelPeriodo.calcular(
                        List.of(propuesta(DE_TDS, DOCENTE), propuesta(DE_OTRA, DOCENTE)),
                        List.of(MIRADO, DE_TDS, DE_OTRA),
                        MIRADO);

        assertThat(horas.get(DOCENTE).asignadas()).isEqualByComparingTo("5.75");
    }

    @Test
    void dadaUnaPropuestaDeOtraCarrera_cuandoSeCalcula_entoncesVaAparte() {
        HorasDelDocente horas =
                HorasDelPeriodo.calcular(
                                List.of(propuesta(DE_TDS, DOCENTE), propuesta(DE_OTRA, DOCENTE)),
                                List.of(MIRADO, DE_TDS, DE_OTRA),
                                MIRADO)
                        .get(DOCENTE);

        assertThat(horas.enOtrasCarreras()).isEqualByComparingTo("3.25");
    }

    @Test
    void dadosDosDocentes_cuandoSeCalcula_entoncesCadaUnoTieneLasSuyas() {
        Map<String, HorasDelDocente> horas =
                HorasDelPeriodo.calcular(
                        List.of(propuesta(DE_TDS, DOCENTE), propuesta(DE_OTRA, OTRO_DOCENTE)),
                        List.of(MIRADO, DE_TDS, DE_OTRA),
                        MIRADO);

        assertThat(horas.get(DOCENTE).asignadas()).isEqualByComparingTo("2.5");
        assertThat(horas.get(OTRO_DOCENTE).asignadas()).isEqualByComparingTo("3.25");
    }

    @Test
    void dadaLaPropuestaDelParaleloMirado_cuandoSeCalcula_entoncesNoCuentaPorqueSeReemplaza() {
        assertThat(
                        HorasDelPeriodo.calcular(
                                List.of(propuesta(MIRADO, DOCENTE)), List.of(MIRADO), MIRADO))
                .isEmpty();
    }

    @Test
    void dadoElMismoParaleloEnOtraAsignatura_cuandoSeCalcula_entoncesSiCuenta() {
        AsignaturaParalelo mismoParalelo = paralelo("POO-205", "A", "TDS", "4", false);

        assertThat(
                        HorasDelPeriodo.calcular(
                                List.of(propuesta(mismoParalelo, DOCENTE)),
                                List.of(MIRADO, mismoParalelo),
                                MIRADO))
                .containsKey(DOCENTE);
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeCalcula_entoncesSusHorasNoCuentan() {
        AsignaturaParalelo retirado = paralelo("BD-301", "A", "TDS", "2.5", true);

        assertThat(
                        HorasDelPeriodo.calcular(
                                List.of(propuesta(retirado, DOCENTE)),
                                List.of(MIRADO, retirado),
                                MIRADO))
                .isEmpty();
    }

    @Test
    void dadoQueLasHorasSeCorrigieron_cuandoSeCalcula_entoncesUsaLasActualesDeLaOferta() {
        AsignaturaParalelo corregido = paralelo("BD-301", "A", "TDS", "6", false);

        assertThat(
                        HorasDelPeriodo.calcular(
                                        List.of(propuesta(DE_TDS, DOCENTE)),
                                        List.of(MIRADO, corregido),
                                        MIRADO)
                                .get(DOCENTE)
                                .asignadas())
                .isEqualByComparingTo("6");
    }

    @Test
    void dadaUnaPropuestaQueYaNoEstaEnLaOferta_cuandoSeCalcula_entoncesNoCuenta() {
        assertThat(
                        HorasDelPeriodo.calcular(
                                List.of(propuesta(DE_TDS, DOCENTE)), List.of(MIRADO), MIRADO))
                .isEmpty();
    }

    @Test
    void dadoQueNoHayPropuestas_cuandoSeCalcula_entoncesVacio() {
        assertThat(HorasDelPeriodo.calcular(List.of(), List.of(MIRADO), MIRADO)).isEmpty();
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
