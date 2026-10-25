package ec.mdd.docentes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las reglas de un proyecto aprobado (F03-H05, RF-18, carga 9), sin dobles: cada campo con sus
 * casos válidos, inválidos y en el límite (principio XIV, aclaraciones 5 a 7).
 */
class ProyectoTest {

    private static final String DIRECTOR = "0109990200";
    private static final String INVESTIGADOR = "0109990226";

    /** El periodo 2026-I de la carga 03 de prueba. */
    private static final LocalDate INICIO_I = LocalDate.of(2026, 4, 6);

    private static final LocalDate FIN_I = LocalDate.of(2026, 8, 7);

    @Test
    void dadoUnProyectoCompleto_cuandoSeCrea_entoncesGuardaSusDatosSinEspaciosDeMas() {
        Proyecto proyecto = proyecto(datos -> datos);

        assertThat(proyecto.nombre()).isEqualTo("Prototipo de monitoreo IoT");
        assertThat(proyecto.horasAprobadas()).isEqualByComparingTo("24");
        assertThat(proyecto.participantes()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inv-2025-01", "INV 2025", "INV_2025", ""})
    void dadoUnCodigoConOtrosCaracteres_cuandoSeCrea_entoncesSeRechazaEnCodigo(String codigo) {
        assertThatThrownBy(() -> proyecto(datos -> datos.codigo(codigo)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("codigo");
    }

    @Test
    void dadoElCodigo_cuandoTiene30O31Caracteres_entoncesSoloPasaEl30() {
        assertThatCode(() -> proyecto(datos -> datos.codigo("A".repeat(30))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> proyecto(datos -> datos.codigo("A".repeat(31))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("codigo");
    }

    @Test
    void dadoElNombre_cuandoEstaVacioOPasaDe250_entoncesSeRechaza() {
        assertThatThrownBy(() -> proyecto(datos -> datos.nombre(" ")))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("nombre");
        assertThatCode(() -> proyecto(datos -> datos.nombre("N".repeat(250))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> proyecto(datos -> datos.nombre("N".repeat(251))))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("250 caracteres");
    }

    @Test
    void dadasLasCarreras_cuandoFaltanSeRepitenOPasanDe20_entoncesSeRechazanConSuPosicion() {
        assertThatThrownBy(() -> proyecto(datos -> datos.carreras(List.of())))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carreras");
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.carreras(
                                                        List.of(
                                                                "550613A01-P-0301",
                                                                "550613A01-P-0301"))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carreras[1]");
        List<String> veinte = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            veinte.add("C" + i);
        }
        assertThatCode(() -> proyecto(datos -> datos.carreras(veinte.subList(0, 20))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> proyecto(datos -> datos.carreras(veinte)))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("20 carreras");
    }

    @Test
    void dadaUnaCarreraQueNoExiste_cuandoSeVerifica_entoncesSeRechazaConSuPosicion() {
        Proyecto proyecto =
                proyecto(datos -> datos.carreras(List.of("550613A01-P-0301", "NO-EXISTE")));

        assertThatThrownBy(() -> CargasDePrueba.referencias().verificar(proyecto))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carreras[1]");
    }

    @Test
    void dadaLaResolucion_cuandoEstaVaciaOPasaDe60_entoncesSeRechaza() {
        assertThatThrownBy(() -> proyecto(datos -> datos.resolucion("")))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("resolucionOcs");
        assertThatCode(() -> proyecto(datos -> datos.resolucion("R".repeat(60))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> proyecto(datos -> datos.resolucion("R".repeat(61))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("resolucionOcs");
    }

    @Test
    void dadaLaVigencia_cuandoSuFinNoEsPosteriorAlInicio_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.vigencia(
                                                        LocalDate.of(2026, 4, 1),
                                                        LocalDate.of(2026, 4, 1))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("vigenciaFin");
    }

    @Test
    void
            dadosElDirectorYLosParticipantes_cuandoNoSonDocentesRegistrados_entoncesSeRechazanEnSuCampo() {
        Proyecto proyecto = proyecto(datos -> datos);

        assertThatCode(() -> proyecto.verificarDocentes(docente -> true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> proyecto.verificarDocentes(docente -> !docente.equals(DIRECTOR)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("director");
        assertThatThrownBy(
                        () -> proyecto.verificarDocentes(docente -> !docente.equals(INVESTIGADOR)))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageNotContaining(INVESTIGADOR)
                .extracting("campo")
                .isEqualTo("participantes[1].docente");
    }

    @Test
    void dadosLosParticipantes_cuandoFaltanOSeRepiten_entoncesSeRechazan() {
        assertThatThrownBy(() -> proyecto(datos -> datos.participantes(List.of())))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("participantes");
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.participantes(
                                                        List.of(
                                                                uno(DIRECTOR, "10"),
                                                                uno(DIRECTOR, "4")))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("participantes[1].docente");
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.participantes(
                                                        List.of(
                                                                new Participante(
                                                                        DIRECTOR,
                                                                        null,
                                                                        BigDecimal.TEN)))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("participantes[0].rol");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "2.55", "2.50", "99.99", "999.9", "24"})
    void dadasUnasHorasDeHastaDosDecimalesY5Caracteres_cuandoSeCrea_entoncesPasan(String horas) {
        Proyecto proyecto =
                proyecto(
                        datos ->
                                datos.aprobadas(horas)
                                        .participantes(List.of(uno(DIRECTOR, horas))));

        assertThat(proyecto.horasAprobadas()).isEqualByComparingTo(horas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2.555", "100.25", "1000", "99999"})
    void dadasUnasHorasFueraDeLaRegla_cuandoSeCrea_entoncesSeRechazanEnSuCampo(String horas) {
        assertThatThrownBy(() -> proyecto(datos -> datos.aprobadas(horas)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("horasAprobadas");
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.participantes(
                                                        List.of(
                                                                uno(DIRECTOR, "1"),
                                                                uno(INVESTIGADOR, horas)))))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("participantes[1].horas");
    }

    @Test
    void
            dadosParticipantesQueSumanLasAprobadasOUnaDecimaMas_cuandoSeCrea_entoncesSoloPasaLaSumaExacta() {
        assertThatCode(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.participantes(
                                                        List.of(
                                                                uno(DIRECTOR, "14"),
                                                                uno(INVESTIGADOR, "10")))))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                proyecto(
                                        datos ->
                                                datos.participantes(
                                                        List.of(
                                                                uno(DIRECTOR, "14"),
                                                                uno(INVESTIGADOR, "10.1")))))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("suman 24.1 y pasan las 24 aprobadas")
                .extracting("campo")
                .isEqualTo("participantes");
    }

    @Test
    void
            dadoUnProyecto_cuandoSeCorrige_entoncesElCodigoNoCambiaYCadaCambioTraeSuAnteriorYSuNuevo() {
        Proyecto antes = proyecto(datos -> datos);
        Proyecto suspendido = proyecto(datos -> datos.estado(EstadoDeProyecto.SUSPENDIDO));

        Proyecto.Correccion correccion = antes.corregido(suspendido);

        assertThat(correccion.cambios())
                .containsExactly(new CambioDeValor("estado", "EN_EJECUCION", "SUSPENDIDO"));
        assertThat(antes.corregido(antes).cambios()).isEmpty();
        assertThatThrownBy(() -> antes.corregido(proyecto(datos -> datos.codigo("OTRO"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadosLos11ProyectosDeLaCarga09_cuandoSeVerifican_entoncesTodosSonValidos() {
        var docentes = CargasDePrueba.docentes();
        List<Proyecto> proyectos = CargasDePrueba.proyectos();

        assertThat(proyectos).hasSize(11);
        for (Proyecto proyecto : proyectos) {
            CargasDePrueba.referencias().verificar(proyecto);
            proyecto.verificarDocentes(docentes::containsKey);
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = EstadoDeProyecto.class,
            names = {"APROBADO", "EN_EJECUCION"})
    void dadoUnProyectoQueRecibeHorasYSeCruzaConElPeriodo_cuandoSeConsulta_entoncesEsAsignable(
            EstadoDeProyecto estado) {
        Asignabilidad asignabilidad =
                proyecto(datos -> datos.estado(estado)).asignableEn(INICIO_I, FIN_I);

        assertThat(asignabilidad.asignable()).isTrue();
        assertThat(asignabilidad.motivos()).isEmpty();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = EstadoDeProyecto.class,
            names = {"FINALIZADO", "SUSPENDIDO"})
    void dadoUnProyectoFinalizadoOSuspendido_cuandoSeConsulta_entoncesNoEsAsignablePorSuEstado(
            EstadoDeProyecto estado) {
        Asignabilidad asignabilidad =
                proyecto(datos -> datos.estado(estado)).asignableEn(INICIO_I, FIN_I);

        assertThat(asignabilidad.asignable()).isFalse();
        assertThat(asignabilidad.motivos()).containsExactly(MotivoNoAsignable.ESTADO);
    }

    @Test
    void
            dadaUnaVigenciaQueTerminaOEmpiezaJustoAntesODespues_cuandoSeConsulta_entoncesSoloElCruceInclusiveEsAsignable() {
        assertThat(conVigencia(INICIO_I.minusYears(1), INICIO_I.minusDays(1)).motivos())
                .containsExactly(MotivoNoAsignable.VIGENCIA);
        assertThat(conVigencia(INICIO_I.minusYears(1), INICIO_I).asignable()).isTrue();
        assertThat(conVigencia(FIN_I, FIN_I.plusYears(1)).asignable()).isTrue();
        assertThat(conVigencia(FIN_I.plusDays(1), FIN_I.plusYears(1)).motivos())
                .containsExactly(MotivoNoAsignable.VIGENCIA);
    }

    @Test
    void dadoUnProyectoSuspendidoYFueraDeVigencia_cuandoSeConsulta_entoncesTraeLosDosMotivos() {
        Proyecto proyecto =
                proyecto(
                        datos ->
                                datos.estado(EstadoDeProyecto.SUSPENDIDO)
                                        .vigencia(INICIO_I.minusYears(2), INICIO_I.minusYears(1)));

        assertThat(proyecto.asignableEn(INICIO_I, FIN_I).motivos())
                .containsExactlyInAnyOrder(MotivoNoAsignable.ESTADO, MotivoNoAsignable.VIGENCIA);
    }

    private static Asignabilidad conVigencia(LocalDate desde, LocalDate hasta) {
        return proyecto(datos -> datos.vigencia(desde, hasta)).asignableEn(INICIO_I, FIN_I);
    }

    private static Participante uno(String docente, String horas) {
        return new Participante(docente, RolDeParticipante.INVESTIGADOR, new BigDecimal(horas));
    }

    /** Un proyecto de investigación en ejecución, con un cambio sobre sus datos válidos. */
    static Proyecto proyecto(UnaryOperator<Datos> cambio) {
        return cambio.apply(new Datos()).proyecto();
    }

    /** Los datos de un proyecto válido, que cada prueba cambia. */
    static final class Datos {
        private String codigo = "INV-2025-01";
        private String nombre = "  Prototipo de monitoreo IoT ";
        private List<String> carreras = List.of("550613A01-P-0301");
        private String resolucion = "RES-OCS-PRUEBA-2025-040";
        private EstadoDeProyecto estado = EstadoDeProyecto.EN_EJECUCION;
        private LocalDate inicio = LocalDate.of(2025, 4, 1);
        private LocalDate fin = LocalDate.of(2026, 12, 31);
        private List<Participante> participantes =
                List.of(
                        new Participante(
                                DIRECTOR, RolDeParticipante.DIRECTOR, new BigDecimal("14")),
                        new Participante(
                                INVESTIGADOR,
                                RolDeParticipante.INVESTIGADOR,
                                new BigDecimal("10")));
        private String aprobadas = "24";

        Datos codigo(String valor) {
            codigo = valor;
            return this;
        }

        Datos nombre(String valor) {
            nombre = valor;
            return this;
        }

        Datos carreras(List<String> valor) {
            carreras = valor;
            return this;
        }

        Datos resolucion(String valor) {
            resolucion = valor;
            return this;
        }

        Datos estado(EstadoDeProyecto valor) {
            estado = valor;
            return this;
        }

        Datos vigencia(LocalDate desde, LocalDate hasta) {
            inicio = desde;
            fin = hasta;
            return this;
        }

        Datos participantes(List<Participante> valor) {
            participantes = valor;
            return this;
        }

        Datos aprobadas(String valor) {
            aprobadas = valor;
            return this;
        }

        Proyecto proyecto() {
            return new Proyecto(
                    codigo,
                    nombre,
                    TipoDeProyecto.INVESTIGACION,
                    carreras,
                    resolucion,
                    LocalDate.of(2025, 3, 20),
                    estado,
                    inicio,
                    fin,
                    DIRECTOR,
                    participantes,
                    new BigDecimal(aprobadas));
        }
    }
}
