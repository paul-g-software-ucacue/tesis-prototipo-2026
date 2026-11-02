package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La sugerencia de docentes para una asignatura-paralelo (RF-25, RN-19). El mínimo y los pesos no
 * son los de por defecto, para que un valor escrito en el código no pase la prueba (principio VI).
 */
class SugerenciaTest {

    private static final String PERIODO = "2026-I";
    private static final String ESPECIFICO = "061";

    private static final PesosAfinidad PESOS =
            new PesosAfinidad(
                    new BigDecimal("7"),
                    new BigDecimal("4"),
                    new BigDecimal("5"),
                    new BigDecimal("3"),
                    new BigDecimal("2.5"),
                    new BigDecimal("1.5"),
                    new BigDecimal("4"));

    private static final PesosDelPeriodo CON_MINIMO_3 =
            new PesosDelPeriodo(2, PESOS, new BigDecimal("3"));

    private static final AsignaturaOfertada ASIGNATURA =
            new AsignaturaOfertada("POO-204", "Programación", "06", ESPECIFICO);

    private static final AsignaturaParalelo DE_4_HORAS = paralelo(new BigDecimal("4"), false);

    // Puntaje 7: cuarto nivel en el campo específico. El tercer nivel es ajeno.
    private static final FormacionDelDocente DE_7 = formacion("091", ESPECIFICO);
    // Puntaje 5: tercer nivel en el campo específico.
    private static final FormacionDelDocente DE_5 = formacion(ESPECIFICO);

    // Puntaje 3: tercer nivel solo en el campo amplio.
    private static final FormacionDelDocente DE_3 = formacion("062");
    private static final ReglasDelPeriodo REGLAS =
            new ReglasDelPeriodo(new BigDecimal("40"), new BigDecimal("16"));

    // Puntaje 0: nada coincide.
    private static final FormacionDelDocente DE_0 = formacion("091");

    @Test
    void dadosDocentesBajoElMinimo_cuandoSeArma_entoncesVanAparteYEnElMismoOrden() {
        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        DE_4_HORAS,
                        new PesosDelPeriodo(2, PESOS, new BigDecimal("5")),
                        List.of(
                                docente("A", 40, DE_0),
                                docente("B", 40, DE_3),
                                docente("C", 40, DE_5)),
                        Map.of("ID-B", new HorasDelDocente(new BigDecimal("6"), BigDecimal.ZERO)));

        assertThat(sugerencia.candidatos())
                .extracting(Candidato::identificacion)
                .containsExactly("ID-C");
        assertThat(sugerencia.bajoElMinimo())
                .extracting(Candidato::identificacion)
                .containsExactly("ID-B", "ID-A");
        assertThat(sugerencia.bajoElMinimo().getFirst().horasDisponibles())
                .isEqualByComparingTo("34");
    }

    @Test
    void dadosDocentesBajoElMinimo_cuandoSeArma_entoncesLaCuentaEsElTamanoDeLaLista() {
        Sugerencia sugerencia = armar(docente("A", 40, DE_0), docente("B", 40, DE_0));

        assertThat(sugerencia.fueraPorElMinimo()).isEqualTo(sugerencia.bajoElMinimo().size());
        assertThat(sugerencia.fueraPorElMinimo()).isEqualTo(2);
    }

    @Test
    void dadoTroncoComun_cuandoSeArma_entoncesNadieQuedaBajoElMinimo() {
        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        paralelo(new BigDecimal("4"), true),
                        CON_MINIMO_3,
                        List.of(docente("A", 40, DE_0)),
                        Map.of());

        assertThat(sugerencia.candidatos()).hasSize(1);
        assertThat(sugerencia.bajoElMinimo()).isEmpty();
    }

    @Test
    void dadosVariosDocentes_cuandoSeArma_entoncesVanDelMayorPuntajeAlMenor() {
        Sugerencia sugerencia =
                armar(docente("B", 40, DE_5), docente("A", 40, DE_5), docente("C", 40, DE_7));

        assertThat(nombres(sugerencia)).containsExactly("C", "A", "B");
    }

    @Test
    void dadoUnEmpateDePuntaje_cuandoSeArma_entoncesVaPrimeroQuienTieneMasHoras() {
        Sugerencia sugerencia =
                armar(docente("A", 20, DE_5), docente("B", 40, DE_5), docente("C", 30, DE_5));

        assertThat(nombres(sugerencia)).containsExactly("B", "C", "A");
    }

    @Test
    void dadoUnEmpateDePuntajeYHoras_cuandoSeArma_entoncesVanPorNombreEnEspanol() {
        Sugerencia sugerencia =
                armar(
                        docente("ZAMBRANO", 40, DE_5),
                        docente("ÑAUPA", 40, DE_5),
                        docente("NÚÑEZ", 40, DE_5),
                        docente("ÁLVAREZ", 40, DE_5),
                        docente("BRAVO", 40, DE_5));

        assertThat(nombres(sugerencia))
                .containsExactly("ÁLVAREZ", "BRAVO", "NÚÑEZ", "ÑAUPA", "ZAMBRANO");
    }

    @Test
    void dadosPuntajesIgualesConOtraEscala_cuandoSeArma_entoncesEmpatan() {
        PesosDelPeriodo pesos =
                new PesosDelPeriodo(
                        2,
                        new PesosAfinidad(
                                new BigDecimal("5.00"),
                                new BigDecimal("4"),
                                new BigDecimal("5"),
                                new BigDecimal("3"),
                                new BigDecimal("2.5"),
                                new BigDecimal("1.5"),
                                new BigDecimal("4")),
                        new BigDecimal("3"));

        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        DE_4_HORAS,
                        pesos,
                        List.of(docente("B", 40, DE_7), docente("A", 40, DE_5)),
                        Map.of());

        assertThat(nombres(sugerencia)).containsExactly("A", "B");
    }

    @Test
    void dadoElMejorPuntajeSinHorasParaElParalelo_cuandoSeArma_entoncesVaDespuesDeLosDemas() {
        Sugerencia sugerencia = armar(docente("A", 3, DE_7), docente("B", 40, DE_5));

        assertThat(nombres(sugerencia)).containsExactly("B", "A");
        assertThat(sugerencia.candidatos().getLast().tieneHorasPara(DE_4_HORAS.horasSemanales()))
                .isFalse();
    }

    @Test
    void dadoQuienTieneJustoLasHorasDelParalelo_cuandoSeArma_entoncesVaEnElPrimerGrupo() {
        Sugerencia sugerencia = armar(docente("A", 4, DE_7), docente("B", 40, DE_5));

        assertThat(nombres(sugerencia)).containsExactly("A", "B");
    }

    @Test
    void dadoUnParaleloConDecimales_cuandoSeArma_entoncesLasHorasNoSeRedondean() {
        AsignaturaParalelo paralelo = paralelo(new BigDecimal("2.5"), false);
        DocenteActivo justo = docente("JUSTO", 5, DE_7);
        DocenteActivo corto = docente("CORTO", 5, DE_7);

        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        paralelo,
                        CON_MINIMO_3,
                        List.of(corto, justo),
                        Map.of(
                                justo.identificacion(),
                                horas("2.5", "0"),
                                corto.identificacion(),
                                horas("2.51", "0")));

        assertThat(nombres(sugerencia)).containsExactly("JUSTO", "CORTO");
    }

    @Test
    void dadosDosParalelosDeLaMismaAsignatura_cuandoSeArman_entoncesPuedenCambiarDeOrden() {
        List<DocenteActivo> docentes = List.of(docente("A", 3, DE_7), docente("B", 40, DE_5));

        assertThat(
                        nombres(
                                armar(
                                        PERIODO,
                                        paralelo(new BigDecimal("2"), false),
                                        CON_MINIMO_3,
                                        docentes,
                                        Map.of())))
                .containsExactly("A", "B");
        assertThat(
                        nombres(
                                armar(
                                        PERIODO,
                                        paralelo(new BigDecimal("4"), false),
                                        CON_MINIMO_3,
                                        docentes,
                                        Map.of())))
                .containsExactly("B", "A");
    }

    @Test
    void dadoUnCandidato_cuandoSeArma_entoncesTraeSuPuntajeConElDesglose() {
        Candidato candidato = armar(docente("A", 40, DE_7)).candidatos().getFirst();

        assertThat(candidato.puntaje().total()).isEqualByComparingTo("7");
        assertThat(candidato.puntaje().desglose())
                .singleElement()
                .satisfies(aporte -> assertThat(aporte.nombre()).isEqualTo("Cuarto nivel 061"));
    }

    @Test
    void dadoQueNoHayDocentes_cuandoSeArma_entoncesNoHayCandidatos() {
        Sugerencia sugerencia = armar();

        assertThat(sugerencia.candidatos()).isEmpty();
        assertThat(sugerencia.fueraPorElMinimo()).isZero();
    }

    @Test
    void dadoUnDocenteBajoElMinimo_cuandoSeArma_entoncesNoApareceYSeCuenta() {
        Sugerencia sugerencia = armar(docente("A", 40, DE_5), docente("B", 40, DE_0));

        assertThat(nombres(sugerencia)).containsExactly("A");
        assertThat(sugerencia.fueraPorElMinimo()).isEqualTo(1);
    }

    @Test
    void dadoUnPuntajeIgualAlMinimo_cuandoSeArma_entoncesAparece() {
        Sugerencia sugerencia = armar(docente("A", 40, DE_3));

        assertThat(nombres(sugerencia)).containsExactly("A");
        assertThat(sugerencia.fueraPorElMinimo()).isZero();
    }

    @Test
    void dadoUnMinimoConOtraEscala_cuandoSeArma_entoncesElPuntajeIgualAparece() {
        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        DE_4_HORAS,
                        new PesosDelPeriodo(2, PESOS, new BigDecimal("3.00")),
                        List.of(docente("A", 40, DE_3)),
                        Map.of());

        assertThat(nombres(sugerencia)).containsExactly("A");
    }

    @Test
    void dadoTroncoComun_cuandoSeArma_entoncesAparecenTodosTambienConCeroPuntos() {
        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        paralelo(new BigDecimal("4"), true),
                        CON_MINIMO_3,
                        List.of(docente("A", 40, DE_0), docente("B", 40, DE_5)),
                        Map.of());

        assertThat(nombres(sugerencia)).containsExactly("B", "A");
        assertThat(sugerencia.fueraPorElMinimo()).isZero();
    }

    @Test
    void dadoQueNadieAlcanzaElMinimo_cuandoSeArma_entoncesSinCandidatosYLosCuenta() {
        Sugerencia sugerencia = armar(docente("A", 40, DE_0), docente("B", 40, DE_0));

        assertThat(sugerencia.candidatos()).isEmpty();
        assertThat(sugerencia.fueraPorElMinimo()).isEqualTo(2);
    }

    @Test
    void dadoUnDocenteBajoElMinimoYSinHoras_cuandoSeArma_entoncesNoApareceYSeCuenta() {
        Sugerencia sugerencia = armar(docente("A", 1, DE_0));

        assertThat(sugerencia.candidatos()).isEmpty();
        assertThat(sugerencia.fueraPorElMinimo()).isEqualTo(1);
    }

    @Test
    void dadosDosMinimos_cuandoSeArman_entoncesLasListasCambian() {
        List<DocenteActivo> docentes = List.of(docente("A", 40, DE_7), docente("B", 40, DE_5));

        assertThat(
                        nombres(
                                armar(
                                        PERIODO,
                                        DE_4_HORAS,
                                        new PesosDelPeriodo(2, PESOS, new BigDecimal("6")),
                                        docentes,
                                        Map.of())))
                .containsExactly("A");
        assertThat(nombres(armar(docentes.toArray(DocenteActivo[]::new))))
                .containsExactly("A", "B");
    }

    @Test
    void dadoUnDocenteConHorasEnOtraCarrera_cuandoSeArma_entoncesSeVenYRestan() {
        DocenteActivo docente = docente("A", 40, DE_7);

        Candidato candidato =
                armar(
                                PERIODO,
                                DE_4_HORAS,
                                CON_MINIMO_3,
                                List.of(docente),
                                Map.of(docente.identificacion(), horas("16", "10")))
                        .candidatos()
                        .getFirst();

        assertThat(candidato.horasDisponibles()).isEqualByComparingTo("24");
        assertThat(candidato.horas().enOtrasCarreras()).isEqualByComparingTo("10");
    }

    @Test
    void dadoUnDocenteSinHorasRegistradas_cuandoSeArma_entoncesTieneCeroHoras() {
        Candidato candidato = armar(docente("A", 20, DE_7)).candidatos().getFirst();

        assertThat(candidato.horasDisponibles()).isEqualByComparingTo("20");
        assertThat(candidato.horas()).isEqualTo(HorasDelDocente.NINGUNA);
    }

    @Test
    void dadasHorasAsignadas_cuandoSeArma_entoncesCambiaSuLugarEnLaLista() {
        DocenteActivo ocupado = docente("A", 40, DE_7);

        Sugerencia sugerencia =
                armar(
                        PERIODO,
                        DE_4_HORAS,
                        CON_MINIMO_3,
                        List.of(ocupado, docente("B", 40, DE_5)),
                        Map.of(ocupado.identificacion(), horas("38", "38")));

        assertThat(nombres(sugerencia)).containsExactly("B", "A");
    }

    private static Sugerencia armar(DocenteActivo... docentes) {
        return armar(PERIODO, DE_4_HORAS, CON_MINIMO_3, List.of(docentes), Map.of());
    }

    private static List<String> nombres(Sugerencia sugerencia) {
        return sugerencia.candidatos().stream().map(Candidato::nombre).toList();
    }

    private static AsignaturaParalelo paralelo(BigDecimal horas, boolean troncoComun) {
        return new AsignaturaParalelo(ASIGNATURA, "A", "TDS", horas, troncoComun, false);
    }

    private static HorasDelDocente horas(String asignadas, String enOtrasCarreras) {
        return new HorasDelDocente(new BigDecimal(asignadas), new BigDecimal(enOtrasCarreras));
    }

    private static DocenteActivo docente(
            String nombre, int horasContratadas, FormacionDelDocente formacion) {
        return new DocenteActivo(
                "ID-" + nombre, nombre, Dedicacion.TC, horasContratadas, formacion);
    }

    private static FormacionDelDocente formacion(String tercerNivel, String... cuartosNiveles) {
        return new FormacionDelDocente(
                new TituloDeTercerNivel("Tercer nivel", tercerNivel.substring(0, 2), tercerNivel),
                List.of(cuartosNiveles).stream()
                        .map(campo -> new ElementoDeFormacion("Cuarto nivel " + campo, campo))
                        .toList(),
                List.of(),
                List.of());
    }

    @Test
    void dadoUnMotivo_cuandoSeArma_entoncesElCandidatoLoLleva() {
        Sugerencia sugerencia =
                Sugerencia.armar(
                        PERIODO,
                        DE_4_HORAS,
                        new PesosDelPeriodo(2, PESOS, BigDecimal.ZERO),
                        List.of(docente("A", 40, DE_5), docente("B", 40, DE_3)),
                        Map.of(),
                        REGLAS,
                        docente ->
                                docente.identificacion().equals("ID-A")
                                        ? Optional.of(ReglaDeAsignacion.RN_13)
                                        : Optional.empty());

        assertThat(sugerencia.candidatos())
                .extracting(Candidato::identificacion, Candidato::motivo)
                .containsExactly(tuple("ID-B", null), tuple("ID-A", ReglaDeAsignacion.RN_13));
    }

    @Test
    void conUnTopeMenorQueLasContratadas_cuandoSeArma_entoncesLasDisponiblesLoRespetan() {
        Sugerencia sugerencia =
                Sugerencia.armar(
                        PERIODO,
                        DE_4_HORAS,
                        new PesosDelPeriodo(2, PESOS, BigDecimal.ZERO),
                        List.of(docente("A", 40, DE_5)),
                        Map.of(),
                        new ReglasDelPeriodo(new BigDecimal("38"), new BigDecimal("16")),
                        docente -> Optional.empty());

        assertThat(sugerencia.candidatos().getFirst().horasDisponibles())
                .isEqualByComparingTo("38");
    }

    private static Sugerencia armar(
            String periodo,
            AsignaturaParalelo paralelo,
            PesosDelPeriodo pesos,
            List<DocenteActivo> docentes,
            Map<String, HorasDelDocente> horas) {
        return Sugerencia.armar(
                periodo, paralelo, pesos, docentes, horas, REGLAS, docente -> Optional.empty());
    }
}
