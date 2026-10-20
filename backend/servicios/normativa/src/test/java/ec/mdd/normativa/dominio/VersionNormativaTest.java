package ec.mdd.normativa.dominio;

import static ec.mdd.normativa.dominio.FilaCargaHorariaTest.fila;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La versión de normativa y sus reglas (RF-19, MD-08), sin dobles. Desde F04-H02, también sus
 * operaciones: copiarla en una versión nueva, editarla mientras no rija un periodo y asociarla a
 * uno.
 */
class VersionNormativaTest {

    private static final TablaCargaHoraria TABLA =
            new TablaCargaHoraria(List.of(fila(null, "DOCENTE_TC", 22, 18, 24, 18, 0, null)));
    private static final Regla RN_02 =
            new Regla(
                    "RN-02", Severidad.DURA, Map.of("horas_maximas_docente", new BigDecimal("40")));
    private static final Regla RN_01 =
            new Regla(
                    "RN-01",
                    null,
                    parametros(
                            "dedicacion_tiempo_completo", "40", "dedicacion_medio_tiempo", "20"));

    private static Map<String, BigDecimal> parametros(String... pares) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(pares[i], new BigDecimal(pares[i + 1]));
        }
        return mapa;
    }

    private static VersionNormativa version(int numero, List<Regla> reglas) {
        return new VersionNormativa(
                numero,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                TABLA,
                reglas);
    }

    @Test
    void dadoUnaVersion_cuandoSeReconstruye_entoncesTieneSusDatosYReglasPorIdentificador() {
        VersionNormativa version = version(1, List.of(RN_02, RN_01));

        assertThat(version.numero()).isEqualTo(1);
        assertThat(version.tipoIst()).isEqualTo(TipoIst.T2);
        assertThat(version.reglas())
                .extracting(Regla::identificador)
                .containsExactly("RN-01", "RN-02");
        assertThat(version.regla("RN-02")).contains(RN_02);
        assertThat(version.regla("RN-26")).isEmpty();
    }

    @Test
    void dadoUnaReglaSinSeveridad_cuandoSeLee_entoncesSoloTieneParametros() {
        assertThat(RN_01.severidad()).isNull();
        assertThat(RN_01.parametros())
                .containsExactly(
                        Map.entry("dedicacion_tiempo_completo", new BigDecimal("40")),
                        Map.entry("dedicacion_medio_tiempo", new BigDecimal("20")));
        assertThat(RN_01.parametro("dedicacion_medio_tiempo")).contains(new BigDecimal("20"));
    }

    @Test
    void dadoDatosInvalidos_cuandoSeReconstruyeLaVersion_entoncesSeRechaza() {
        assertThatThrownBy(() -> version(0, List.of(RN_01)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> version(1, List.of(RN_01, RN_01)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RN-01");
    }

    @Test
    void dadoUnaReglaRf_cuandoSeCreaLaVersion_entoncesVaDespuesDeLasRn() {
        Regla rf24 = new Regla("RF-24", Severidad.DURA, Map.of());

        assertThat(version(1, List.of(rf24, RN_02, RN_01)).reglas())
                .extracting(Regla::identificador)
                .containsExactly("RN-01", "RN-02", "RF-24");
    }

    @Test
    void dadoReglasInvalidas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> new Regla("RN-1", null, Map.of("x", BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
        // Desde F04-H03 una regla del motor puede no tener parámetros; sin severidad, no.
        assertThatThrownBy(() -> new Regla("RN-05", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // F04-H02: copiar, editar y asociar a un periodo.

    private static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");
    private static final DatosDeVersion REFORMA =
            new DatosDeVersion(
                    "SENESCYT-2025-01", "Reforma de la política 2025", LocalDate.of(2025, 1, 15));

    private static FilaCargaHoraria medioTiempo(int clase) {
        return new FilaCargaHoraria(
                null, "DOCENTE_MT", 20, clase, 8, 12, 8, 0, null, Severidad.DURA, "POL 6", null);
    }

    private static VersionNormativa politica() {
        return new VersionNormativa(
                1,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                new TablaCargaHoraria(
                        List.of(
                                fila(null, "DOCENTE_TC", 22, 18, 24, 18, 0, null),
                                medioTiempo(12))),
                List.of(RN_01, RN_02));
    }

    // Las filas, reglas y parámetros de la versión tal como vienen, con los cambios indicados.
    private static CambiosDeVersion cambios(
            VersionNormativa version, DatosDeVersion datos, int claseMedioTiempo, String maximo) {
        List<HorasDeFila> filas =
                version.tabla().filas().stream()
                        .map(
                                fila ->
                                        fila.perfil().equals("DOCENTE_MT")
                                                ? HorasDeFila.de(fila)
                                                        .conHorasClase(claseMedioTiempo)
                                                : HorasDeFila.de(fila))
                        .toList();
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas()
                .forEach(regla -> parametros.put(regla.identificador(), regla.parametros()));
        parametros.put("RN-02", Map.of("horas_maximas_docente", new BigDecimal(maximo)));
        return new CambiosDeVersion(datos, filas, parametros);
    }

    private static DatosDeVersion datosDe(VersionNormativa version) {
        return new DatosDeVersion(version.codigo(), version.origen(), version.vigenteDesde());
    }

    @Test
    void dadoLaVersionUno_cuandoSeCopia_entoncesLaNuevaTieneSusValoresYSuPropiaCabecera() {
        VersionNormativa uno = politica();

        VersionNormativa.Copia copia = VersionNormativa.copiaDe(uno, 2, REFORMA, AHORA);

        VersionNormativa dos = copia.version();
        assertThat(dos.numero()).isEqualTo(2);
        assertThat(dos.codigo()).isEqualTo("SENESCYT-2025-01");
        assertThat(dos.origen()).isEqualTo("Reforma de la política 2025");
        assertThat(dos.vigenteDesde()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(dos.copiadaDe()).isEqualTo(1);
        assertThat(dos.tipoIst()).isEqualTo(uno.tipoIst());
        assertThat(dos.tabla()).isEqualTo(uno.tabla());
        assertThat(dos.reglas()).isEqualTo(uno.reglas());
        assertThat(dos.enUso()).isFalse();
        assertThat(dos.periodos()).isEmpty();
        assertThat(copia.evento())
                .isEqualTo(new VersionNormativaCreada(2, "SENESCYT-2025-01", 1, AHORA));
    }

    @Test
    void dadoLaVersionUno_cuandoSeCopiaConElMismoNumero_entoncesSeRechaza() {
        assertThatThrownBy(() -> VersionNormativa.copiaDe(politica(), 1, REFORMA, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnaVersionSinPeriodo_cuandoSeCambianHorasYParametro_entoncesDevuelveLosCambios() {
        VersionNormativa dos = VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();
        DatosDeVersion otraCabecera =
                new DatosDeVersion("SENESCYT-2025-02", "Reforma 2025", LocalDate.of(2025, 2, 1));

        VersionNormativa.Edicion edicion = dos.editada(cambios(dos, otraCabecera, 10, "38"));

        assertThat(edicion.version().tabla().filaPara("DOCENTE_MT", TipoIst.T2))
                .get()
                .extracting(FilaCargaHoraria::horasClase)
                .isEqualTo(10);
        assertThat(edicion.version().regla("RN-02").get().parametro("horas_maximas_docente"))
                .contains(new BigDecimal("38"));
        assertThat(edicion.version().codigo()).isEqualTo("SENESCYT-2025-02");
        assertThat(edicion.version().copiadaDe()).isEqualTo(1);
        assertThat(edicion.cambios())
                .containsExactly(
                        new CambioDeValor("codigo", "SENESCYT-2025-01", "SENESCYT-2025-02"),
                        new CambioDeValor("origen", "Reforma de la política 2025", "Reforma 2025"),
                        new CambioDeValor("vigenteDesde", "2025-01-15", "2025-02-01"),
                        new CambioDeValor(
                                "tablaCargaHoraria[DOCENTE_MT/TODOS].horasClase", "12", "10"),
                        new CambioDeValor("reglas[RN-02].horas_maximas_docente", "40", "38"));
        // La versión original no cambia: es inmutable.
        assertThat(dos.tabla().filaPara("DOCENTE_MT", TipoIst.T2).get().horasClase()).isEqualTo(12);
    }

    @Test
    void dadoUnaEdicionSinCambios_cuandoSeAplica_entoncesNoHayCambiosAunqueCambieLaEscala() {
        VersionNormativa dos = VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();

        VersionNormativa.Edicion edicion = dos.editada(cambios(dos, REFORMA, 12, "40.00"));

        assertThat(edicion.cambios()).isEmpty();
        assertThat(edicion.version()).isEqualTo(dos);
    }

    @Test
    void dadoHorasDeClaseFueraDeRango_cuandoSeEdita_entoncesFilaInvalidaConPerfilTipoYMotivo() {
        VersionNormativa dos = VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();

        assertThatThrownBy(() -> dos.editada(cambios(dos, REFORMA, 13, "40")))
                .isInstanceOfSatisfying(
                        FilaInvalida.class,
                        error -> {
                            assertThat(error.perfil()).isEqualTo("DOCENTE_MT");
                            assertThat(error.aplicaA()).isNull();
                            assertThat(error.motivo())
                                    .isEqualTo(
                                            "Las horas de clase (13) no están entre el mínimo y"
                                                    + " el máximo (8 a 12)");
                        });
    }

    @Test
    void dadoUnaFilaOUnParametroDeMasODeMenos_cuandoSeEdita_entoncesCambioNoPermitido() {
        VersionNormativa dos = VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();
        CambiosDeVersion completos = cambios(dos, REFORMA, 12, "40");

        CambiosDeVersion sinUnaFila =
                new CambiosDeVersion(
                        REFORMA, completos.filas().subList(0, 1), completos.parametros());
        assertThatThrownBy(() -> dos.editada(sinUnaFila))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error ->
                                assertThat(error.campo())
                                        .isEqualTo("tablaCargaHoraria[DOCENTE_MT/TODOS]"));

        List<HorasDeFila> conOtra = new java.util.ArrayList<>(completos.filas());
        conOtra.add(HorasDeFila.de(fila(TipoIst.T1, "GESTOR", 16, 16, 16, 16, 0, null)));
        assertThatThrownBy(
                        () ->
                                dos.editada(
                                        new CambiosDeVersion(
                                                REFORMA, conOtra, completos.parametros())))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error ->
                                assertThat(error.campo())
                                        .isEqualTo("tablaCargaHoraria[GESTOR/T1]"));

        Map<String, Map<String, BigDecimal>> otroParametro =
                new LinkedHashMap<>(completos.parametros());
        otroParametro.put("RN-02", Map.of("horas_minimas_docente", BigDecimal.TEN));
        assertThatThrownBy(
                        () ->
                                dos.editada(
                                        new CambiosDeVersion(
                                                REFORMA, completos.filas(), otroParametro)))
                .isInstanceOf(CambioNoPermitido.class)
                .hasMessageContaining("RN-02");

        Map<String, Map<String, BigDecimal>> sinRegla = new LinkedHashMap<>(completos.parametros());
        sinRegla.remove("RN-01");
        assertThatThrownBy(
                        () ->
                                dos.editada(
                                        new CambiosDeVersion(REFORMA, completos.filas(), sinRegla)))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error -> assertThat(error.campo()).isEqualTo("reglas[RN-01]"));
    }

    @Test
    void dadoUnaVersionSinPeriodo_cuandoSeAsocia_entoncesQuedaEnUsoYYaNoSeEdita() {
        VersionNormativa uno = politica();

        VersionNormativa asociada = uno.asociadaA("2025-1", AHORA);

        assertThat(asociada.enUso()).isTrue();
        assertThat(asociada.periodos()).containsExactly("2025-1");
        assertThat(asociada.asignaciones())
                .containsExactly(new AsignacionNormativa("2025-1", AHORA));
        assertThat(asociada.asociadaA("2025-1", AHORA.plusSeconds(60))).isEqualTo(asociada);
        assertThat(asociada.asociadaA("2025-2", AHORA).periodos())
                .containsExactly("2025-1", "2025-2");
        assertThatThrownBy(() -> asociada.editada(cambios(asociada, datosDe(asociada), 10, "40")))
                .isInstanceOfSatisfying(
                        VersionEnUso.class, error -> assertThat(error.numero()).isEqualTo(1));
        assertThat(uno.enUso()).isFalse();
    }

    @Test
    void dadoUnCodigoDePeriodoMalFormado_cuandoSeAsocia_entoncesSeRechaza() {
        assertThatThrownBy(() -> politica().asociadaA("2025 1", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> politica().asociadaA("x".repeat(21), AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnaCabeceraIncompleta_cuandoSeCreaSusDatos_entoncesSeRechaza() {
        assertThatThrownBy(() -> new DatosDeVersion(" ", "Reforma", LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DatosDeVersion("X", "Reforma", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void dadoUnParametroNegativoConTresDecimalesODemasiadoGrande_cuandoSeEdita_entoncesSeRechaza() {
        VersionNormativa dos = VersionNormativa.copiaDe(politica(), 2, REFORMA, AHORA).version();

        for (String valor : List.of("-1", "38.125", "100000000")) {
            assertThatThrownBy(() -> dos.editada(cambios(dos, REFORMA, 12, valor)))
                    .isInstanceOfSatisfying(
                            CambioNoPermitido.class,
                            error ->
                                    assertThat(error.campo())
                                            .isEqualTo("reglas[RN-02].horas_maximas_docente"));
        }
        assertThat(dos.editada(cambios(dos, REFORMA, 12, "38.50")).cambios())
                .containsExactly(
                        new CambioDeValor("reglas[RN-02].horas_maximas_docente", "40", "38.5"));
    }

    // F04-H03: los pesos y el mínimo de afinidad.

    private static final Regla RN_19 =
            new Regla("RN-19", Severidad.BLANDA, Map.of("minimo_afinidad", new BigDecimal("2")));

    private static VersionNormativa conAfinidad() {
        return VersionNormativa.copiaDe(
                        new VersionNormativa(
                                1,
                                "SENESCYT-2024-03",
                                "Política SENESCYT 2024",
                                LocalDate.of(2024, 3, 11),
                                TipoIst.T2,
                                new TablaCargaHoraria(List.of(medioTiempo(12))),
                                List.of(
                                        RN_02,
                                        RN_19,
                                        new Regla(
                                                "RN-24",
                                                null,
                                                PesosAfinidadTest.pesos(3, 2, 2, 1)))),
                        2,
                        REFORMA,
                        AHORA)
                .version();
    }

    // La versión tal como viene, con un parámetro cambiado.
    private static CambiosDeVersion conParametro(
            VersionNormativa version, String regla, String nombre, String valor) {
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas()
                .forEach(
                        una ->
                                parametros.put(
                                        una.identificador(),
                                        new LinkedHashMap<>(una.parametros())));
        parametros.get(regla).put(nombre, new BigDecimal(valor));
        return new CambiosDeVersion(
                datosDe(version),
                version.tabla().filas().stream().map(HorasDeFila::de).toList(),
                parametros);
    }

    @Test
    void dadoUnaVersionConRn19YRn24_cuandoSeConsultaLaAfinidad_entoncesDaLosPesosYElMinimo() {
        VersionNormativa dos = conAfinidad();

        assertThat(dos.pesosAfinidad())
                .hasValueSatisfying(
                        pesos -> {
                            assertThat(pesos.cuartoNivelCampoEspecifico())
                                    .isEqualByComparingTo("3");
                            assertThat(pesos.topeCursos()).isEqualByComparingTo("3");
                        });
        assertThat(dos.minimoAfinidad())
                .hasValueSatisfying(minimo -> assertThat(minimo.valor()).isEqualByComparingTo("2"));
    }

    @Test
    void dadoUnaVersionSinRn19NiRn24_cuandoSeConsultaLaAfinidad_entoncesNoHay() {
        assertThat(politica().pesosAfinidad()).isEmpty();
        assertThat(politica().minimoAfinidad()).isEmpty();
    }

    @Test
    void dadoElCampoAmplioMayorQueElEspecifico_cuandoSeEdita_entoncesSeRechazaYNoCambia() {
        VersionNormativa dos = conAfinidad();

        assertThatThrownBy(
                        () ->
                                dos.editada(
                                        conParametro(
                                                dos, "RN-24", "cuarto_nivel_campo_amplio", "4")))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error ->
                                assertThat(error.campo())
                                        .isEqualTo("reglas[RN-24].cuarto_nivel_campo_amplio"));
        assertThat(dos.pesosAfinidad().orElseThrow().cuartoNivelCampoAmplio())
                .isEqualByComparingTo("2");
    }

    @Test
    void dadoElMinimo_cuandoSeCambia_entoncesQuedaAnotadoConSuValorAnterior() {
        VersionNormativa dos = conAfinidad();

        VersionNormativa.Edicion edicion =
                dos.editada(conParametro(dos, "RN-19", "minimo_afinidad", "3"));

        assertThat(edicion.cambios())
                .containsExactly(new CambioDeValor("reglas[RN-19].minimo_afinidad", "2", "3"));
        assertThat(edicion.version().minimoAfinidad().orElseThrow().valor())
                .isEqualByComparingTo("3");
    }

    @Test
    void dadoUnPesoNegativo_cuandoSeEdita_entoncesSeRechazaComoCualquierParametro() {
        VersionNormativa dos = conAfinidad();

        assertThatThrownBy(() -> dos.editada(conParametro(dos, "RN-24", "curso_relacionado", "-1")))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error ->
                                assertThat(error.motivo())
                                        .isEqualTo("El valor no puede ser negativo"));
    }

    // F04-H03: la severidad de las reglas (MD-08).

    // La versión tal como viene, sin cambios de valores, con las severidades indicadas.
    private static CambiosDeVersion conSeveridades(
            VersionNormativa version, Map<String, Severidad> severidades) {
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas().forEach(una -> parametros.put(una.identificador(), una.parametros()));
        return new CambiosDeVersion(
                datosDe(version),
                version.tabla().filas().stream().map(HorasDeFila::de).toList(),
                parametros,
                severidades);
    }

    @Test
    void dadoUnaReglaBlanda_cuandoSePasaADura_entoncesCambiaYQuedaAnotada() {
        VersionNormativa dos = conAfinidad();

        VersionNormativa.Edicion edicion =
                dos.editada(conSeveridades(dos, Map.of("RN-19", Severidad.DURA)));

        assertThat(edicion.cambios())
                .containsExactly(new CambioDeValor("reglas[RN-19].severidad", "BLANDA", "DURA"));
        assertThat(edicion.version().regla("RN-19").orElseThrow().severidad())
                .isEqualTo(Severidad.DURA);
        assertThat(edicion.version().regla("RN-19").orElseThrow().parametros())
                .isEqualTo(RN_19.parametros());
        assertThat(dos.regla("RN-19").orElseThrow().severidad()).isEqualTo(Severidad.BLANDA);
    }

    @Test
    void dadoSinSeveridadPedida_cuandoSeEdita_entoncesNoCambia() {
        VersionNormativa dos = conAfinidad();

        assertThat(dos.editada(conSeveridades(dos, Map.of())).cambios()).isEmpty();
    }

    @Test
    void dadoLaMismaSeveridad_cuandoSeEdita_entoncesNoHayCambios() {
        VersionNormativa dos = conAfinidad();

        assertThat(dos.editada(conSeveridades(dos, Map.of("RN-02", Severidad.DURA))).cambios())
                .isEmpty();
    }

    @Test
    void dadoUnaReglaQueSoloDaParametros_cuandoSeLeDaSeveridad_entoncesSeRechaza() {
        VersionNormativa dos = conAfinidad();

        assertThatThrownBy(() -> dos.editada(conSeveridades(dos, Map.of("RN-24", Severidad.DURA))))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error -> {
                            assertThat(error.campo()).isEqualTo("reglas[RN-24].severidad");
                            assertThat(error.motivo())
                                    .isEqualTo("Esta regla solo da parámetros: no tiene severidad");
                        });
    }

    @Test
    void dadoUnaSeveridadDeUnaReglaQueNoEsta_cuandoSeEdita_entoncesSeRechaza() {
        VersionNormativa dos = conAfinidad();

        assertThatThrownBy(() -> dos.editada(conSeveridades(dos, Map.of("RN-17", Severidad.DURA))))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error -> assertThat(error.campo()).isEqualTo("reglas[RN-17]"));
    }

    @Test
    void dadoUnaVersionEnUso_cuandoSeCambiaUnaSeveridad_entoncesSeRechaza() {
        VersionNormativa enUso = conAfinidad().asociadaA("2025-1", AHORA);

        assertThatThrownBy(
                        () -> enUso.editada(conSeveridades(enUso, Map.of("RN-19", Severidad.DURA))))
                .isInstanceOf(VersionEnUso.class);
    }
}
