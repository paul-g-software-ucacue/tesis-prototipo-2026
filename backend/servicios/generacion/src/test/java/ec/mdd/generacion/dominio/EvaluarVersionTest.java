package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El motor evalúa la versión completa, con las cifras de la normativa (RF-29). */
class EvaluarVersionTest {

    private static final String PERIODO = "2026-I";
    private static final String TDS = "TDS";
    private static final String ELE = "ELE";

    private static final Map<String, Severidad> SEVERIDADES =
            Map.of(
                    "RF-24", Severidad.DURA,
                    "RN-02", Severidad.DURA,
                    "RN-05", Severidad.BLANDA,
                    "RN-10", Severidad.BLANDA,
                    "RN-12", Severidad.DURA,
                    "RN-17", Severidad.DURA,
                    "RN-18", Severidad.DURA);

    private final List<AsignaturaParalelo> oferta = new ArrayList<>();
    private final List<DocenteActivo> docentes = new ArrayList<>();
    private final List<CargoVigente> cargos = new ArrayList<>(gestores(9));
    private final List<Propuesta> propuestas = new ArrayList<>();
    private final List<ActividadNoLectiva> actividades = new ArrayList<>();

    @Test
    void dadoUnDocenteSinClase_cuandoSeEvalua_entoncesRn17() {
        docentes.add(docente("A", 40));

        assertThat(deTipo(TipoDeInconsistencia.DOCENTE_SIN_CLASE))
                .extracting(una -> una.objeto().clave(), InconsistenciaGlobal::severidad)
                .containsExactly(tuple("A", Severidad.DURA));
    }

    @Test
    void dadoUnDocenteConClase_cuandoSeEvalua_entoncesNoHayRn17() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "4", "4", false);
        proponer("POO", "A", "A");

        assertThat(deTipo(TipoDeInconsistencia.DOCENTE_SIN_CLASE)).isEmpty();
    }

    @Test
    void dadoUnDocenteConClaseSoloEnUnRetirado_cuandoSeEvalua_entoncesRn17() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "4", "4", true);
        proponer("POO", "A", "A");

        assertThat(deTipo(TipoDeInconsistencia.DOCENTE_SIN_CLASE)).hasSize(1);
    }

    @Test
    void dadoUnParaleloSinPropuesta_cuandoSeEvalua_entoncesRf24ConSusHoras() {
        ofertar(TDS, "POO", "A", "4", "4", false);

        InconsistenciaGlobal una = deTipo(TipoDeInconsistencia.PARALELO_SIN_PROPUESTA).getFirst();

        assertThat(una.objeto().clave()).isEqualTo("POO/A");
        assertThat(una.objeto().nombre()).isEqualTo("Asignatura POO · A");
        assertThat(una.severidad()).isEqualTo(Severidad.DURA);
        assertThat(una.datos().cantidad()).isEqualByComparingTo("4");
    }

    @Test
    void dadoUnParaleloConPropuesta_cuandoSeEvalua_entoncesNoHayRf24() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "4", "4", false);
        proponer("POO", "A", "A");

        assertThat(deTipo(TipoDeInconsistencia.PARALELO_SIN_PROPUESTA)).isEmpty();
    }

    @Test
    void dadoUnParaleloRetiradoSinPropuesta_cuandoSeEvalua_entoncesNoHayRf24() {
        ofertar(TDS, "POO", "A", "4", "4", true);

        assertThat(deTipo(TipoDeInconsistencia.PARALELO_SIN_PROPUESTA)).isEmpty();
    }

    @Test
    void dadaUnaCarreraSinCoordinador_cuandoSeEvalua_entoncesRn12ConCero() {
        ofertar(TDS, "POO", "A", "4", "4", false);

        InconsistenciaGlobal una =
                deTipo(TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA).getFirst();

        assertThat(una.objeto().clave()).isEqualTo(TDS);
        assertThat(una.datos().cantidad()).isEqualByComparingTo("0");
    }

    @Test
    void dadaUnaCarreraConUnCoordinador_cuandoSeEvalua_entoncesNoHayRn12() {
        ofertar(TDS, "POO", "A", "4", "4", false);
        cargos.add(new CargoVigente("C1", "COORDINADOR_CARRERA", TDS));

        assertThat(deTipo(TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA)).isEmpty();
    }

    @Test
    void dadaUnaCarreraConDosCoordinadores_cuandoSeEvalua_entoncesRn12ConDos() {
        ofertar(TDS, "POO", "A", "4", "4", false);
        cargos.add(new CargoVigente("C1", "COORDINADOR_CARRERA", TDS));
        cargos.add(new CargoVigente("C2", "COORDINADOR_CARRERA", TDS));

        assertThat(deTipo(TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA).getFirst().datos())
                .extracting(DatosDeLaInconsistencia::cantidad)
                .isEqualTo(BigDecimal.TWO);
    }

    @Test
    void dadaUnaCarreraSoloConUnRetirado_cuandoSeEvalua_entoncesNoHayRn12() {
        ofertar(TDS, "POO", "A", "4", "4", true);

        assertThat(deTipo(TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA)).isEmpty();
    }

    @Test
    void dadoElCoordinadorDeOtraCarrera_cuandoSeEvalua_entoncesNoCuentaParaEsta() {
        ofertar(TDS, "POO", "A", "4", "4", false);
        cargos.add(new CargoVigente("C1", "COORDINADOR_CARRERA", ELE));

        assertThat(deTipo(TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA)).hasSize(1);
    }

    @Test
    void dadosOchoYOnceGestores_cuandoSeEvalua_entoncesNoHayRn10() {
        cargos.clear();
        cargos.addAll(gestores(8));
        assertThat(deTipo(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO)).isEmpty();

        cargos.clear();
        cargos.addAll(gestores(11));
        assertThat(deTipo(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO)).isEmpty();
    }

    @Test
    void dadosSieteYDoceGestores_cuandoSeEvalua_entoncesRn10BlandaConElRango() {
        cargos.clear();
        cargos.addAll(gestores(7));
        InconsistenciaGlobal pocos =
                deTipo(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO).getFirst();

        assertThat(pocos.severidad()).isEqualTo(Severidad.BLANDA);
        assertThat(pocos.objeto().clave()).isEqualTo(PERIODO);
        assertThat(pocos.datos().cantidad()).isEqualByComparingTo("7");
        assertThat(pocos.datos().minimo()).isEqualByComparingTo("8");
        assertThat(pocos.datos().maximo()).isEqualByComparingTo("11");

        cargos.clear();
        cargos.addAll(gestores(12));
        assertThat(deTipo(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO)).hasSize(1);
    }

    @Test
    void dadosOtrosCargos_cuandoSeCuentanLosGestores_entoncesNoCuentan() {
        cargos.clear();
        cargos.addAll(gestores(8));
        cargos.add(new CargoVigente("R", "RECTOR", null));
        cargos.add(new CargoVigente("M", "MIEMBRO_COMISION", null));
        cargos.add(new CargoVigente("C", "COORDINADOR_CARRERA", TDS));
        cargos.add(new CargoVigente("S", "COORDINADOR_SEDE", null));

        assertThat(deTipo(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO)).isEmpty();
    }

    @Test
    void dadosParalelosSobreLaMalla_cuandoSeEvalua_entoncesUnaRn18PorCarrera() {
        ofertar(TDS, "POO", "A", "6", "4", false);
        ofertar(TDS, "BDD", "B", "5", "3", false);
        ofertar(TDS, "RED", "C", "3", "3", false);
        ofertar(ELE, "CIR", "A", "2", "4", false);

        List<InconsistenciaGlobal> horas = deTipo(TipoDeInconsistencia.HORAS_SOBRE_LA_MALLA);

        assertThat(horas).hasSize(1);
        assertThat(horas.getFirst().objeto().clave()).isEqualTo(TDS);
        assertThat(horas.getFirst().severidad()).isEqualTo(Severidad.DURA);
        assertThat(horas.getFirst().datos().lista())
                .containsExactly("Asignatura BDD · B", "Asignatura POO · A");
    }

    @Test
    void dadoUnRetiradoSobreLaMalla_cuandoSeEvalua_entoncesNoHayRn18() {
        ofertar(TDS, "POO", "A", "6", "4", true);

        assertThat(deTipo(TipoDeInconsistencia.HORAS_SOBRE_LA_MALLA)).isEmpty();
    }

    @Test
    void dadoUnDocenteSobreSuLimite_cuandoSeEvalua_entoncesSobrecargaSumandoLasCarreras() {
        docentes.add(docente("A", 20));
        ofertar(TDS, "POO", "A", "12", "12", false);
        ofertar(ELE, "CIR", "A", "9", "9", false);
        proponer("POO", "A", "A");
        proponer("CIR", "A", "A");

        InconsistenciaGlobal una = deTipo(TipoDeInconsistencia.SOBRECARGA).getFirst();

        assertThat(una.objeto().clave()).isEqualTo("A");
        assertThat(una.severidad()).isEqualTo(Severidad.DURA);
        assertThat(una.datos().cantidad()).isEqualByComparingTo("21");
        assertThat(una.datos().maximo()).isEqualByComparingTo("20");
    }

    @Test
    void dadoUnDocenteJustoEnSuLimite_cuandoSeEvalua_entoncesNoHaySobrecarga() {
        docentes.add(docente("A", 20));
        ofertar(TDS, "POO", "A", "20", "20", false);
        proponer("POO", "A", "A");

        assertThat(deTipo(TipoDeInconsistencia.SOBRECARGA)).isEmpty();
    }

    @Test
    void dadasOtrasSeveridades_cuandoSeEvalua_entoncesMandaLaVersion() {
        Map<String, Severidad> otras = new java.util.HashMap<>(SEVERIDADES);
        otras.put("RN-10", Severidad.DURA);
        cargos.clear();

        assertThat(
                        MotorReglas.evaluarVersion(datos(), reglas(otras, "5")).stream()
                                .filter(
                                        una ->
                                                una.tipo()
                                                        == TipoDeInconsistencia
                                                                .GESTORES_FUERA_DE_RANGO)
                                .map(InconsistenciaGlobal::severidad))
                .containsExactly(Severidad.DURA);
    }

    @Test
    void sinUnaSeveridad_cuandoSeArmanLasReglas_entoncesFalla() {
        Map<String, Severidad> faltan = new java.util.HashMap<>(SEVERIDADES);
        faltan.remove("RN-17");

        assertThatThrownBy(() -> reglas(faltan, "5")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadosLosLimitesDelMargen_cuandoSeEvalua_entoncesCincuentaYCincoYSesentaYCincoCumplen() {
        assertThat(conProporcion("11", "9")).isEmpty();
        assertThat(conProporcion("13", "7")).isEmpty();
    }

    @Test
    void justoFueraDelMargen_cuandoSeEvalua_entoncesRn05ConPorcentajesMetaYMargen() {
        List<InconsistenciaGlobal> arriba = conProporcion("13.2", "6.8");

        assertThat(arriba).hasSize(1);
        assertThat(arriba.getFirst().severidad()).isEqualTo(Severidad.BLANDA);
        assertThat(arriba.getFirst().objeto().clave()).isEqualTo("A");
        assertThat(arriba.getFirst().datos().cantidad()).isEqualByComparingTo("66");
        assertThat(arriba.getFirst().datos().referencia()).isEqualByComparingTo("60");
        assertThat(arriba.getFirst().datos().minimo()).isEqualByComparingTo("55");
        assertThat(arriba.getFirst().datos().maximo()).isEqualByComparingTo("65");
        assertThat(conProporcion("10.8", "9.2")).hasSize(1);
    }

    @Test
    void conOtroMargen_cuandoSeEvalua_entoncesLoUsa() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "13.2", "13.2", false);
        proponer("POO", "A", "A");
        actividad("A", "140-b", Componente.OTRAS_DOCENCIA, "6.8");

        assertThat(evaluar(reglas(SEVERIDADES, "6"), TipoDeInconsistencia.PROPORCION)).isEmpty();
    }

    @Test
    void conOtraMeta_cuandoSeEvalua_entoncesLaUsa() {
        ReglasDelPanel otraMeta =
                new ReglasDelPanel(
                        SEVERIDADES,
                        new BigDecimal("40"),
                        new BigDecimal("70"),
                        new BigDecimal("30"),
                        new BigDecimal("5"),
                        8,
                        11,
                        List.of("140-b", "140-g"));
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "60", "60", false);
        proponer("POO", "A", "A");
        actividad("A", "140-b", Componente.OTRAS_DOCENCIA, "40");

        assertThat(evaluar(otraMeta, TipoDeInconsistencia.PROPORCION)).hasSize(1);
    }

    @Test
    void sinHorasDeDocencia_cuandoSeEvalua_entoncesNoHayRn05() {
        docentes.add(docente("A", 40));
        actividad("A", "143-k", Componente.GESTION, "10");

        assertThat(deTipo(TipoDeInconsistencia.PROPORCION)).isEmpty();
        assertThat(deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS)).isEmpty();
    }

    @Test
    void conClaseSinLasDosActividades_cuandoSeEvalua_entoncesRn05ConLasQueFaltan() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "6", "6", false);
        proponer("POO", "A", "A");

        InconsistenciaGlobal una = deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS).getFirst();

        assertThat(una.severidad()).isEqualTo(Severidad.BLANDA);
        assertThat(una.datos().lista()).containsExactly("140-b", "140-g");
    }

    @Test
    void conUnaDeLasDos_cuandoSeEvalua_entoncesDiceLaQueFalta() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "6", "6", false);
        proponer("POO", "A", "A");
        actividad("A", "140-g", Componente.OTRAS_DOCENCIA, "2");

        assertThat(deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS).getFirst().datos().lista())
                .containsExactly("140-b");
    }

    @Test
    void conLasDos_cuandoSeEvalua_entoncesNoHayInconsistenciaDeActividades() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "6", "6", false);
        proponer("POO", "A", "A");
        actividad("A", "140-b", Componente.OTRAS_DOCENCIA, "2");
        actividad("A", "140-g", Componente.OTRAS_DOCENCIA, "2");

        assertThat(deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS)).isEmpty();
    }

    @Test
    void sinClase_cuandoSeEvalua_entoncesNoSeRevisanLasActividades() {
        docentes.add(docente("A", 40));
        actividad("A", "140-c", Componente.OTRAS_DOCENCIA, "2");

        assertThat(deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS)).isEmpty();
    }

    @Test
    void fueraDelMargenYSinActividades_cuandoSeEvalua_entoncesSonDosInconsistencias() {
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", "6", "6", false);
        proponer("POO", "A", "A");

        assertThat(deTipo(TipoDeInconsistencia.PROPORCION)).hasSize(1);
        assertThat(deTipo(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS)).hasSize(1);
    }

    private List<InconsistenciaGlobal> conProporcion(String clase, String otras) {
        docentes.clear();
        oferta.clear();
        propuestas.clear();
        actividades.clear();
        docentes.add(docente("A", 40));
        ofertar(TDS, "POO", "A", clase, clase, false);
        proponer("POO", "A", "A");
        actividad("A", "140-b", Componente.OTRAS_DOCENCIA, otras);
        return deTipo(TipoDeInconsistencia.PROPORCION);
    }

    private List<InconsistenciaGlobal> evaluar(ReglasDelPanel reglas, TipoDeInconsistencia tipo) {
        return MotorReglas.evaluarVersion(datos(), reglas).stream()
                .filter(una -> una.tipo() == tipo)
                .toList();
    }

    private void actividad(String docente, String codigo, Componente componente, String horas) {
        actividades.add(
                new ActividadNoLectiva(
                        TDS,
                        docente,
                        "Docente " + docente,
                        codigo,
                        "Actividad " + codigo,
                        componente,
                        null,
                        null,
                        new BigDecimal(horas),
                        Instant.parse("2026-11-02T15:00:00Z"),
                        "coordinacion.demo"));
    }

    private List<InconsistenciaGlobal> deTipo(TipoDeInconsistencia tipo) {
        return MotorReglas.evaluarVersion(datos(), reglas(SEVERIDADES, "5")).stream()
                .filter(una -> una.tipo() == tipo)
                .toList();
    }

    private DatosDelPeriodo datos() {
        return new DatosDelPeriodo(
                PERIODO,
                new VersionDistributivo(
                        UUID.randomUUID(),
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(),
                        propuestas,
                        actividades),
                oferta,
                docentes,
                cargos);
    }

    private static ReglasDelPanel reglas(Map<String, Severidad> severidades, String margen) {
        return new ReglasDelPanel(
                severidades,
                new BigDecimal("40"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                new BigDecimal(margen),
                8,
                11,
                List.of("140-b", "140-g"));
    }

    private void ofertar(
            String carrera,
            String asignatura,
            String paralelo,
            String horas,
            String deLaMalla,
            boolean retirada) {
        oferta.add(
                new AsignaturaParalelo(
                        new AsignaturaOfertada(asignatura, "Asignatura " + asignatura, "06", "061"),
                        paralelo,
                        carrera,
                        new BigDecimal(horas),
                        new BigDecimal(deLaMalla),
                        false,
                        retirada,
                        false));
    }

    private void proponer(String asignatura, String paralelo, String docente) {
        propuestas.add(
                new Propuesta(
                        asignatura,
                        paralelo,
                        TDS,
                        docente,
                        "Docente " + docente,
                        new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                        Instant.parse("2026-11-02T15:00:00Z"),
                        "coordinacion.demo"));
    }

    private static List<CargoVigente> gestores(int cuantos) {
        List<CargoVigente> gestores = new ArrayList<>();
        for (int i = 0; i < cuantos; i++) {
            gestores.add(new CargoVigente("G" + i, "G%02d_GESTOR".formatted(6 + i), null));
        }
        return gestores;
    }

    private static DocenteActivo docente(String identificacion, int contratadas) {
        return new DocenteActivo(
                identificacion,
                "Docente " + identificacion,
                Dedicacion.TC,
                contratadas,
                new FormacionDelDocente(
                        new TituloDeTercerNivel("Tercer nivel", "06", "061"),
                        List.of(),
                        List.of(),
                        List.of()));
    }
}
