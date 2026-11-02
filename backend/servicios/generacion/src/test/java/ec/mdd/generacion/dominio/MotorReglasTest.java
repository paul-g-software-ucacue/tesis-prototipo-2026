package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Las reglas duras al guardar una asignación: viola si queda sobre el límite y sube (RF-28). */
class MotorReglasTest {

    private static final ReglasDelPeriodo REGLAS =
            new ReglasDelPeriodo(new BigDecimal("40"), new BigDecimal("16"), Severidad.BLANDA);

    @Test
    void conTreintaYOchoDeCuarenta_cuandoSeAsignanCuatro_entoncesRn02ConLasQueQuedan() {
        assertThat(evaluar(otras("38"), otras("42"), 40, false))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_02, Severidad.DURA, new BigDecimal("2")));
    }

    @Test
    void justoEnElLimite_cuandoSeEvalua_entoncesNoHayInconsistencia() {
        assertThat(evaluar(otras("36"), otras("40"), 40, false)).isEmpty();
    }

    @Test
    void conVeinteContratadas_cuandoPasaDeVeinte_entoncesRn02() {
        assertThat(evaluar(otras("18"), otras("21"), 20, false))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_02, Severidad.DURA, new BigDecimal("2")));
    }

    @Test
    void conUnTopeMenorQueLasContratadas_cuandoLoPasa_entoncesRn02() {
        ReglasDelPeriodo menor =
                new ReglasDelPeriodo(new BigDecimal("38"), new BigDecimal("16"), Severidad.BLANDA);

        assertThat(MotorReglas.evaluar(otras("37"), otras("39"), 40, false, menor))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_02, Severidad.DURA, new BigDecimal("1")));
    }

    @Test
    void conDecimales_cuandoSePasaPorUnCentesimo_entoncesRn02() {
        assertThat(evaluar(otras("39.5"), otras("40.01"), 40, false))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_02, Severidad.DURA, new BigDecimal("0.5")));
    }

    @Test
    void conProyectoYDieciseisDeClase_cuandoSube_entoncesRn13ConLasQueQuedan() {
        assertThat(evaluar(clase("14"), clase("18"), 40, true))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_13, Severidad.DURA, new BigDecimal("2")));
    }

    @Test
    void conProyecto_cuandoLlegaJustoADieciseis_entoncesNoHayInconsistencia() {
        assertThat(evaluar(clase("12"), clase("16"), 40, true)).isEmpty();
    }

    @Test
    void sinProyecto_cuandoPasaDeDieciseisDeClase_entoncesNoHayInconsistencia() {
        assertThat(evaluar(clase("14"), clase("18"), 40, false)).isEmpty();
    }

    @Test
    void conProyectoYElTopeAlcanzado_cuandoSubenOtrasDeDocencia_entoncesNoHayInconsistencia() {
        CargaHoraria antes = carga("16", "0");
        CargaHoraria despues = carga("16", "8");

        assertThat(evaluar(antes, despues, 40, true)).isEmpty();
    }

    @Test
    void cuandoViolaLasDos_entoncesGanaLaDeLasHoras() {
        assertThat(evaluar(clase("15"), clase("41"), 40, true))
                .map(Inconsistencia::regla)
                .contains(ReglaDeAsignacion.RN_02);
    }

    @Test
    void unDocenteExcedido_cuandoReduce_entoncesNoHayInconsistencia() {
        assertThat(evaluar(otras("33"), otras("25"), 20, false)).isEmpty();
    }

    @Test
    void unDocenteExcedido_cuandoRepite_entoncesNoHayInconsistencia() {
        assertThat(evaluar(otras("33"), otras("33"), 20, false)).isEmpty();
    }

    @Test
    void unDocenteExcedido_cuandoAumenta_entoncesRn02SinHorasNegativas() {
        assertThat(evaluar(otras("33"), otras("34"), 20, false))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_02, Severidad.DURA, BigDecimal.ZERO));
    }

    @Test
    void conClaseDeMasDeAntes_cuandoReduce_entoncesNoHayInconsistencia() {
        assertThat(evaluar(clase("20"), clase("18"), 40, true)).isEmpty();
    }

    @Test
    void conClaseDeMasDeAntes_cuandoSube_entoncesRn13SinHorasNegativas() {
        assertThat(evaluar(clase("20"), clase("22"), 40, true))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_13, Severidad.DURA, BigDecimal.ZERO));
    }

    @Test
    void bajoElMinimoConRn19Blanda_cuandoSeEvalua_entoncesEsBlanda() {
        assertThat(afinidad("2", "3.5", false, Severidad.BLANDA))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_19, Severidad.BLANDA, BigDecimal.ZERO));
    }

    @Test
    void bajoElMinimoConRn19Dura_cuandoSeEvalua_entoncesEsDura() {
        assertThat(afinidad("2", "3.5", false, Severidad.DURA))
                .contains(
                        new Inconsistencia(
                                ReglaDeAsignacion.RN_19, Severidad.DURA, BigDecimal.ZERO));
    }

    @Test
    void justoEnElMinimo_cuandoSeEvalua_entoncesNoHayInconsistencia() {
        assertThat(afinidad("3.50", "3.5", false, Severidad.BLANDA)).isEmpty();
    }

    @Test
    void sobreElMinimo_cuandoSeEvalua_entoncesNoHayInconsistencia() {
        assertThat(afinidad("7", "3.5", false, Severidad.DURA)).isEmpty();
    }

    @Test
    void enTroncoComun_cuandoSeEvaluaBajoElMinimo_entoncesEstaExento() {
        assertThat(afinidad("0", "3.5", true, Severidad.BLANDA)).isEmpty();
        assertThat(afinidad("0", "3.5", true, Severidad.DURA)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "INVESTIGACION, true",
        "VINCULACION, true",
        "PRACTICAS_PREPROFESIONALES, false",
        "SERVICIO_COMUNITARIO, false",
    })
    void dadoUnTipoDeProyecto_cuandoSePregunta_entoncesDiceSiActivaElTope(
            String tipo, boolean activa) {
        assertThat(MotorReglas.conProyecto(List.of(tipo))).isEqualTo(activa);
    }

    @Test
    void sinProyectos_cuandoSePregunta_entoncesNoActivaElTope() {
        assertThat(MotorReglas.conProyecto(List.of())).isFalse();
    }

    private static java.util.Optional<Inconsistencia> afinidad(
            String puntaje, String minimo, boolean troncoComun, Severidad severidad) {
        return MotorReglas.evaluarAfinidad(
                new BigDecimal(puntaje),
                new BigDecimal(minimo),
                troncoComun,
                new ReglasDelPeriodo(new BigDecimal("40"), new BigDecimal("16"), severidad));
    }

    private static java.util.Optional<Inconsistencia> evaluar(
            CargaHoraria antes, CargaHoraria despues, int contratadas, boolean conProyecto) {
        return MotorReglas.evaluar(antes, despues, contratadas, conProyecto, REGLAS);
    }

    private static CargaHoraria otras(String horas) {
        return carga("0", horas);
    }

    private static CargaHoraria clase(String horas) {
        return carga(horas, "0");
    }

    private static CargaHoraria carga(String clase, String otras) {
        return new CargaHoraria(
                new BigDecimal(clase),
                new BigDecimal(otras),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
