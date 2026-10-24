package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Las reglas de una asignatura-paralelo (F03-H03, carga 5, research §3), sin dobles: el paralelo,
 * los estudiantes previstos y las horas indicadas.
 */
class AsignaturaParaleloTest {

    static AsignaturaParalelo nocturna() {
        return AsignaturaParalelo.nueva(
                "2026-I",
                "DS-P-II-POO-204",
                "A_NOCTURNA",
                Jornada.NOCTURNA,
                Modalidad.PRESENCIAL,
                null,
                28,
                null);
    }

    @Test
    void dadaUnaAsignaturaParaleloValida_cuandoSeOferta_entoncesQuedaOfertadaSinHorasNiSede() {
        AsignaturaParalelo ofertada =
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "DS-P-II-POO-204",
                        "  A_NOCTURNA ",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        " ",
                        0,
                        null);

        assertThat(ofertada.paralelo()).isEqualTo("A_NOCTURNA");
        assertThat(ofertada.sede()).isNull();
        assertThat(ofertada.horasIndicadas()).isNull();
        assertThat(ofertada.estado()).isEqualTo(EstadoDeAsignaturaParalelo.OFERTADA);
    }

    @Test
    void dadoUnParaleloInvalido_cuandoSeOferta_entoncesSeRechazaEnParalelo() {
        for (String paralelo :
                new String[] {null, "", "a_nocturna", "A NOCTURNA", "A".repeat(21)}) {
            assertThatThrownBy(
                            () ->
                                    AsignaturaParalelo.nueva(
                                            "2026-I",
                                            "DS-P-II-POO-204",
                                            paralelo,
                                            Jornada.NOCTURNA,
                                            Modalidad.PRESENCIAL,
                                            null,
                                            28,
                                            null))
                    .isInstanceOf(DatoInvalido.class)
                    .extracting("campo")
                    .isEqualTo("paralelo");
        }
    }

    @Test
    void dadosEstudiantesFueraDeRango_cuandoSeOferta_entoncesSeRechazanEnEstudiantes() {
        for (int estudiantes : new int[] {-1, 10_000}) {
            assertThatThrownBy(
                            () ->
                                    AsignaturaParalelo.nueva(
                                            "2026-I",
                                            "DS-P-II-POO-204",
                                            "A_NOCTURNA",
                                            Jornada.NOCTURNA,
                                            Modalidad.PRESENCIAL,
                                            null,
                                            estudiantes,
                                            null))
                    .isInstanceOf(DatoInvalido.class)
                    .extracting("campo", "motivo")
                    .containsExactly("estudiantes", "Los estudiantes previstos van de 0 a 9999");
        }
    }

    @Test
    void dadasHorasFueraDeRangoOConTresDecimales_cuandoSeOferta_entoncesSeRechazanEnHoras() {
        for (String horas : new String[] {"0.4", "20.5", "3.125"}) {
            assertThatThrownBy(
                            () ->
                                    AsignaturaParalelo.nueva(
                                            "2026-I",
                                            "DS-P-II-POO-204",
                                            "A_NOCTURNA",
                                            Jornada.NOCTURNA,
                                            Modalidad.PRESENCIAL,
                                            null,
                                            28,
                                            new BigDecimal(horas)))
                    .isInstanceOf(DatoInvalido.class)
                    .extracting("campo")
                    .isEqualTo("horasSemanales");
        }
    }

    @Test
    void dadaUnaAsignaturaParalelo_cuandoSeCorrige_entoncesDevuelveCadaCambioSinTocarSuClave() {
        AsignaturaParalelo.Correccion correccion =
                nocturna()
                        .corregida(
                                Jornada.NOCTURNA,
                                Modalidad.DUAL,
                                "NORTE",
                                30,
                                new BigDecimal("3.0"));

        assertThat(correccion.asignaturaParalelo())
                .extracting("periodo", "asignatura", "paralelo", "modalidad", "sede", "estudiantes")
                .containsExactly(
                        "2026-I", "DS-P-II-POO-204", "A_NOCTURNA", Modalidad.DUAL, "NORTE", 30);
        assertThat(correccion.cambios())
                .extracting("campo", "anterior", "nuevo")
                .containsExactly(
                        tuple("modalidad", "PRESENCIAL", "DUAL"),
                        tuple("sede", "", "NORTE"),
                        tuple("estudiantes", "28", "30"),
                        tuple("horasSemanales", "", "3"));
    }

    @Test
    void dadaUnaAsignaturaParalelo_cuandoSeCorrigeConLosMismosDatos_entoncesNoHayCambios() {
        assertThat(
                        nocturna()
                                .corregida(Jornada.NOCTURNA, Modalidad.PRESENCIAL, null, 28, null)
                                .cambios())
                .isEmpty();
    }

    @Test
    void dadaUnaAsignaturaParalelo_cuandoSeRetira_entoncesSoloCambiaSuEstado() {
        AsignaturaParalelo retirada = nocturna().conEstado(EstadoDeAsignaturaParalelo.RETIRADA);

        assertThat(retirada.estado()).isEqualTo(EstadoDeAsignaturaParalelo.RETIRADA);
        assertThat(retirada.conEstado(EstadoDeAsignaturaParalelo.OFERTADA)).isEqualTo(nocturna());
    }
}
