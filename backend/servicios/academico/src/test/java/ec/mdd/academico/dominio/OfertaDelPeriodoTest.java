package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.CarreraTest.invalido;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.asignatura;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.carrera;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Las reglas de la oferta del periodo (F03-H03, RF-15, RN-18, research §3), sin dobles, con las 165
 * asignaturas-paralelo de la carga 05 de prueba como caso grande.
 */
class OfertaDelPeriodoTest {

    private static final String DS = "550613A01-P-0301";

    static PeriodoAcademico periodo(EstadoPeriodo estado) {
        return new PeriodoAcademico(
                "2026-I",
                "Periodo 2026-I",
                LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 8, 7),
                estado);
    }

    /** DS vigente con dos asignaturas de 3 horas (una inactiva), y CONT no vigente con una. */
    static DatosMaestros datos() {
        DatosMaestros datos = DatosMaestrosDePrueba.vacios();
        datos = datos.con(datos.registrarCarrera(carrera(DS, "DS")));
        datos = datos.con(datos.registrarCarrera(carrera("CONT", "CONT")));
        datos = datos.con(datos.registrarMalla(Malla.nueva("DS-2019", DS)));
        datos = datos.con(datos.registrarMalla(Malla.nueva("CONT-2019", "CONT")));
        datos = datos.con(datos.registrarAsignatura(asignatura("DS-1", DS, "DS-2019", 1)));
        datos = datos.con(datos.registrarAsignatura(asignatura("DS-2", DS, "DS-2019", 2)));
        datos = datos.con(datos.registrarAsignatura(asignatura("CONT-1", "CONT", "CONT-2019", 1)));
        datos =
                datos.con(
                        datos.cambiarEstadoAsignatura(
                                datos.asignatura("DS-2").orElseThrow(), false));
        Carrera cont = datos.carrera("CONT").orElseThrow();
        return datos.con(cont.conEstado(EstadoDeCarrera.NO_VIGENTE));
    }

    static OfertaDelPeriodo vacia(EstadoPeriodo estado) {
        return new OfertaDelPeriodo(periodo(estado), List.of(), datos());
    }

    static AsignaturaParalelo en(String asignatura, String paralelo) {
        return AsignaturaParalelo.nueva(
                "2026-I",
                asignatura,
                paralelo,
                Jornada.NOCTURNA,
                Modalidad.PRESENCIAL,
                null,
                28,
                null);
    }

    @Test
    void dadoUnPeriodoFueraDePlanificacion_cuandoSeCambiaLaOferta_entoncesLanzaOfertaCerrada() {
        AsignaturaParalelo ofertada = en("DS-1", "A_NOCTURNA");
        for (EstadoPeriodo estado :
                List.of(
                        EstadoPeriodo.EN_APROBACION,
                        EstadoPeriodo.VIGENTE,
                        EstadoPeriodo.CERRADO)) {
            OfertaDelPeriodo oferta =
                    new OfertaDelPeriodo(periodo(estado), List.of(ofertada), datos());

            assertThat(oferta.modificable()).isFalse();
            assertThatThrownBy(() -> oferta.ofertar(en("DS-1", "B_NOCTURNA")))
                    .isInstanceOf(OfertaCerrada.class);
            assertThatThrownBy(
                            () ->
                                    oferta.corregir(
                                            ofertada,
                                            Jornada.NOCTURNA,
                                            Modalidad.DUAL,
                                            null,
                                            1,
                                            null))
                    .isInstanceOf(OfertaCerrada.class);
            assertThatThrownBy(
                            () ->
                                    oferta.cambiarEstado(
                                            ofertada, EstadoDeAsignaturaParalelo.RETIRADA))
                    .isInstanceOf(OfertaCerrada.class);
        }
    }

    @Test
    void dadaUnaAsignaturaQueNoSePuedeOfertar_cuandoSeOferta_entoncesSeRechazaEnAsignatura() {
        OfertaDelPeriodo oferta = vacia(EstadoPeriodo.PLANIFICACION);

        invalido(() -> oferta.ofertar(en("NADA", "A_NOCTURNA")), "asignatura");
        invalido(() -> oferta.ofertar(en("DS-2", "A_NOCTURNA")), "asignatura");
        invalido(() -> oferta.ofertar(en("CONT-1", "A_NOCTURNA")), "asignatura");
    }

    @Test
    void dadaUnaMallaDerogada_cuandoSeOfertaSuAsignaturaActiva_entoncesSeOferta() {
        DatosMaestros datos = datos();
        datos = datos.con(datos.malla("DS-2019").orElseThrow().conEstado(EstadoDeMalla.DEROGADA));
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(periodo(EstadoPeriodo.PLANIFICACION), List.of(), datos);

        assertThat(oferta.ofertar(en("DS-1", "A_NOCTURNA")).asignatura()).isEqualTo("DS-1");
    }

    @Test
    void dadaUnaJornadaUnaSedeOUnasHorasQueNoCumplen_cuandoSeOferta_entoncesSeRechazanEnSuCampo() {
        OfertaDelPeriodo oferta = vacia(EstadoPeriodo.PLANIFICACION);

        invalido(
                () ->
                        oferta.ofertar(
                                AsignaturaParalelo.nueva(
                                        "2026-I",
                                        "DS-1",
                                        "A_MATUTINA",
                                        Jornada.MATUTINA,
                                        Modalidad.PRESENCIAL,
                                        null,
                                        28,
                                        null)),
                "jornada");
        for (String sede : List.of("NADA", "RECT", "NORTE")) {
            invalido(
                    () ->
                            oferta.ofertar(
                                    AsignaturaParalelo.nueva(
                                            "2026-I",
                                            "DS-1",
                                            "A_NOCTURNA",
                                            Jornada.NOCTURNA,
                                            Modalidad.PRESENCIAL,
                                            sede,
                                            28,
                                            null)),
                    "sede");
        }
        invalido(
                () ->
                        oferta.ofertar(
                                AsignaturaParalelo.nueva(
                                        "2026-I",
                                        "DS-1",
                                        "A_NOCTURNA",
                                        Jornada.NOCTURNA,
                                        Modalidad.PRESENCIAL,
                                        null,
                                        28,
                                        new BigDecimal("3.5"))),
                "horasSemanales");
    }

    @Test
    void
            dadaLaMismaAsignaturaYParalelo_cuandoSeOfertaOtraVez_entoncesLanzaRepetidaAunqueEsteRetirada() {
        AsignaturaParalelo retirada =
                en("DS-1", "A_NOCTURNA").conEstado(EstadoDeAsignaturaParalelo.RETIRADA);
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(
                        periodo(EstadoPeriodo.PLANIFICACION), List.of(retirada), datos());

        assertThatThrownBy(() -> oferta.ofertar(en("DS-1", "A_NOCTURNA")))
                .isInstanceOf(AsignaturaParaleloRepetida.class);
        assertThat(oferta.ofertar(en("DS-1", "B_NOCTURNA")).paralelo()).isEqualTo("B_NOCTURNA");
    }

    @Test
    void
            dadaUnaAsignaturaParaleloSinHorasNiSede_cuandoSeConsulta_entoncesSonLasDeLaAsignaturaYLaCarrera() {
        AsignaturaParalelo sinDatos = en("DS-1", "A_NOCTURNA");
        AsignaturaParalelo conDatos =
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "DS-1",
                        "B_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        "MATRIZ",
                        28,
                        new BigDecimal("2"));
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(
                        periodo(EstadoPeriodo.PLANIFICACION), List.of(sinDatos, conDatos), datos());

        assertThat(oferta.horasDe(sinDatos)).isEqualByComparingTo("3");
        assertThat(oferta.sedeDe(sinDatos)).isEqualTo("MATRIZ");
        assertThat(oferta.horasDe(conDatos)).isEqualByComparingTo("2");
    }

    @Test
    void dadaUnaAsignaturaDe4HorasEn3Paralelos_cuandoSeConsulta_entoncesSuma12YConUnoRetirado8() {
        DatosMaestros datos = DatosMaestrosDePrueba.cargados();
        // CONT-P-III-CC-301 tiene 4 horas y va en A_NOCTURNA y B_NOCTURNA en la carga 05.
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(periodo(EstadoPeriodo.PLANIFICACION), List.of(), datos);
        for (String paralelo : List.of("A_NOCTURNA", "B_NOCTURNA", "C_NOCTURNA")) {
            oferta = oferta.con(oferta.ofertar(en("CONT-P-III-CC-301", paralelo)));
        }

        assertThat(horasDe(oferta, "CONT-P-III-CC-301")).isEqualByComparingTo("12");
        AsignaturaParalelo c =
                oferta.asignaturaParalelo("CONT-P-III-CC-301", "C_NOCTURNA").orElseThrow();
        OfertaDelPeriodo sinC =
                oferta.con(oferta.cambiarEstado(c, EstadoDeAsignaturaParalelo.RETIRADA));
        assertThat(horasDe(sinC, "CONT-P-III-CC-301")).isEqualByComparingTo("8");
        assertThat(sinC.horasACubrir().asignaturas().getFirst().paralelos()).isEqualTo(2);
        AsignaturaParalelo retirada =
                sinC.asignaturaParalelo("CONT-P-III-CC-301", "C_NOCTURNA").orElseThrow();
        OfertaDelPeriodo otraVez =
                sinC.con(sinC.cambiarEstado(retirada, EstadoDeAsignaturaParalelo.OFERTADA));
        assertThat(horasDe(otraVez, "CONT-P-III-CC-301")).isEqualByComparingTo("12");
    }

    @Test
    void dadaLaCarga05DePrueba_cuandoSeOfertaEntera_entoncesSuma660HorasConElTotalDeCadaCarrera() {
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(
                        periodo(EstadoPeriodo.PLANIFICACION),
                        List.of(),
                        DatosMaestrosDePrueba.cargados());
        for (AsignaturaParalelo asignaturaParalelo : DatosMaestrosDePrueba.oferta("2026-I")) {
            oferta = oferta.con(oferta.ofertar(asignaturaParalelo));
        }

        HorasACubrir horas = oferta.horasACubrir();
        assertThat(oferta.asignaturasParalelo()).hasSize(165);
        assertThat(horas.total()).isEqualByComparingTo("660");
        Map<String, BigDecimal> porCarrera =
                horas.carreras().stream()
                        .collect(
                                Collectors.toMap(
                                        HorasACubrir.DeCarrera::carrera,
                                        HorasACubrir.DeCarrera::total));
        assertThat(porCarrera)
                .hasSize(7)
                .hasEntrySatisfying(
                        "550411C02-P-0301", total -> assertThat(total).isEqualByComparingTo("160"))
                .hasEntrySatisfying(DS, total -> assertThat(total).isEqualByComparingTo("80"))
                .hasEntrySatisfying(
                        "550713B01-P-0301", total -> assertThat(total).isEqualByComparingTo("140"))
                .hasEntrySatisfying(
                        "550715F02-P-0301", total -> assertThat(total).isEqualByComparingTo("140"))
                .hasEntrySatisfying(
                        "550732E01-D-0301", total -> assertThat(total).isEqualByComparingTo("20"))
                .hasEntrySatisfying(
                        "550811F01-P-0301", total -> assertThat(total).isEqualByComparingTo("100"))
                .hasEntrySatisfying(
                        "551021B01-D-0301", total -> assertThat(total).isEqualByComparingTo("20"));
    }

    @Test
    void dadasHorasIndicadasQueQuedaronPorEncimaDeLaAsignatura_cuandoSeConsulta_entoncesSeMarca() {
        DatosMaestros datos = datos();
        AsignaturaParalelo dos =
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "DS-1",
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        28,
                        new BigDecimal("3"));
        Asignatura ds1 = datos.asignatura("DS-1").orElseThrow();
        DatosMaestros corregidos =
                datos.con(
                        ds1.corregida(
                                        ds1.nombre(),
                                        ds1.ciclo(),
                                        ds1.unidadOrganizacionCurricular(),
                                        new BigDecimal("2"),
                                        ds1.horasPracticasSemanales(),
                                        ds1.creditos(),
                                        ds1.troncoComun(),
                                        ds1.campo(),
                                        ds1.palabrasClave())
                                .asignatura());

        OfertaDelPeriodo antes =
                new OfertaDelPeriodo(periodo(EstadoPeriodo.PLANIFICACION), List.of(dos), datos);
        OfertaDelPeriodo despues =
                new OfertaDelPeriodo(
                        periodo(EstadoPeriodo.PLANIFICACION), List.of(dos), corregidos);

        assertThat(antes.porEncimaDeLaAsignatura(dos)).isFalse();
        assertThat(despues.porEncimaDeLaAsignatura(dos)).isTrue();
        invalido(
                () ->
                        despues.corregir(
                                dos,
                                Jornada.NOCTURNA,
                                Modalidad.PRESENCIAL,
                                null,
                                30,
                                new BigDecimal("3")),
                "horasSemanales");
    }

    @Test
    void
            dadaUnaRetirada_cuandoSeVuelveAOfertarConSuAsignaturaInactiva_entoncesSeRechazaEnAsignatura() {
        DatosMaestros datos = datos();
        AsignaturaParalelo retirada =
                en("DS-1", "A_NOCTURNA").conEstado(EstadoDeAsignaturaParalelo.RETIRADA);
        DatosMaestros sinDs1 =
                datos.con(
                        datos.cambiarEstadoAsignatura(
                                datos.asignatura("DS-1").orElseThrow(), false));
        OfertaDelPeriodo oferta =
                new OfertaDelPeriodo(
                        periodo(EstadoPeriodo.PLANIFICACION), List.of(retirada), sinDs1);

        invalido(
                () -> oferta.cambiarEstado(retirada, EstadoDeAsignaturaParalelo.OFERTADA),
                "asignatura");
        AsignaturaParalelo ofertada = retirada.conEstado(EstadoDeAsignaturaParalelo.OFERTADA);
        assertThat(
                        new OfertaDelPeriodo(
                                        periodo(EstadoPeriodo.PLANIFICACION),
                                        List.of(ofertada),
                                        sinDs1)
                                .cambiarEstado(ofertada, EstadoDeAsignaturaParalelo.RETIRADA)
                                .estado())
                .isEqualTo(EstadoDeAsignaturaParalelo.RETIRADA);
    }

    private static BigDecimal horasDe(OfertaDelPeriodo oferta, String asignatura) {
        return oferta.horasACubrir().asignaturas().stream()
                .filter(horas -> horas.asignatura().equals(asignatura))
                .findFirst()
                .orElseThrow()
                .total();
    }
}
