package ec.mdd.docentes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Las reglas de un cargo (F03-H05, carga 8, POL 5.4, RN-20, D-73), sin dobles: cada campo con sus
 * casos válidos, inválidos y en el límite (principio XIV).
 */
class CargoTest {

    private static final LocalDate DESIGNACION = LocalDate.of(2025, 4, 1);
    private final Referencias referencias = CargasDePrueba.referencias();

    @Test
    void dadoUnCoordinadorDeCarreraConSuCarrera_cuandoSeCrea_entoncesQuedaVigenteSinUnidad() {
        Cargo cargo = coordinador("550613A01-P-0301");

        assertThat(cargo.estado()).isEqualTo(EstadoDeCargo.VIGENTE);
        assertThat(cargo.unidad()).isNull();
        assertThatCode(() -> referencias.verificar(cargo)).doesNotThrowAnyException();
    }

    @Test
    void dadoUnCoordinadorDeCarreraSinCarrera_cuandoSeCrea_entoncesSeRechazaEnCarrera() {
        assertThatThrownBy(() -> coordinador(null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carrera");
        assertThatThrownBy(() -> referencias.verificar(coordinador("NO-EXISTE")))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("carrera: La carrera no existe");
    }

    @Test
    void
            dadoOtroCargoSinUnidadOConUnaQueNoExisteOEstaInactiva_cuandoSeVerifica_entoncesSeRechaza() {
        assertThatThrownBy(() -> cargo(TipoDeCargo.RECTOR, null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("unidad");
        assertThatThrownBy(() -> referencias.verificar(cargo(TipoDeCargo.RECTOR, "NO-EXISTE")))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("unidad");
        Referencias conInactiva =
                new Referencias(
                        referencias.carreras(),
                        referencias.asignaturas(),
                        referencias.camposAmplios(),
                        java.util.Map.of(
                                "RECT",
                                new Referencias.Unidad("Rectorado", "GOBIERNO", "G01", false)),
                        referencias.periodos());
        assertThatThrownBy(() -> conInactiva.verificar(cargo(TipoDeCargo.RECTOR, "RECT")))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("inactiva");
        assertThatCode(() -> referencias.verificar(cargo(TipoDeCargo.RECTOR, "RECT")))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnGestor_cuandoSuUnidadEsDeOtraGestion_entoncesSeRechaza() {
        assertThatCode(
                        () ->
                                referencias.verificar(
                                        cargo(TipoDeCargo.G07_GESTOR_INVESTIGACION, "G-INV")))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                referencias.verificar(
                                        cargo(TipoDeCargo.G07_GESTOR_INVESTIGACION, "G-VIN")))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("G07");
    }

    @Test
    void dadoUnMiembroDeComision_cuandoSuUnidadNoEsUnaComision_entoncesSeRechaza() {
        assertThatCode(() -> referencias.verificar(cargo(TipoDeCargo.MIEMBRO_COMISION, "COM-DIST")))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () -> referencias.verificar(cargo(TipoDeCargo.MIEMBRO_COMISION, "G-INV")))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("comisión");
    }

    @Test
    void dadoUnCargoQueNoCoordinaCarrera_cuandoTraeCarrera_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new Cargo(
                                        DocentesDePrueba.CEDULA,
                                        TipoDeCargo.RECTOR,
                                        DESIGNACION,
                                        "RECT",
                                        "550613A01-P-0301",
                                        null,
                                        "RES-1",
                                        null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carrera");
    }

    @Test
    void dadaLaFechaDeFin_cuandoEstaEnSusLimites_entoncesSoloPasaEntreUnDiaYDosAniosDespues() {
        assertThatThrownBy(() -> conFin(DESIGNACION))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("fechaFin: El fin de funciones tiene que ser posterior");
        assertThatThrownBy(() -> conFin(DESIGNACION.minusDays(1)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("fechaFin");
        assertThatCode(() -> conFin(DESIGNACION.plusDays(1))).doesNotThrowAnyException();
        assertThatCode(() -> conFin(DESIGNACION.plusYears(2))).doesNotThrowAnyException();
        assertThatThrownBy(() -> conFin(DESIGNACION.plusYears(2).plusDays(1)))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("POL 5.4");
        assertThatCode(() -> conFin(null)).doesNotThrowAnyException();
    }

    @Test
    void dadoElDocumento_cuandoEstaVacioOPasaDe60_entoncesSeRechaza() {
        assertThatThrownBy(() -> conDocumento("  "))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("documento");
        assertThat(conDocumento("  " + "R".repeat(60) + " ").documento()).hasSize(60);
        assertThatThrownBy(() -> conDocumento("R".repeat(61)))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("60 caracteres");
    }

    @Test
    void dadosUnDocenteYUnaUnidadMalFormados_cuandoSeCrea_entoncesSeRechazanEnSuCampo() {
        assertThatThrownBy(
                        () ->
                                new Cargo(
                                        "0109-99",
                                        TipoDeCargo.RECTOR,
                                        DESIGNACION,
                                        "RECT",
                                        null,
                                        null,
                                        "RES-1",
                                        null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("docente");
        assertThatThrownBy(() -> cargo(TipoDeCargo.RECTOR, "rect"))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("unidad");
        assertThatThrownBy(
                        () ->
                                new Cargo(
                                        DocentesDePrueba.CEDULA,
                                        null,
                                        DESIGNACION,
                                        "RECT",
                                        null,
                                        null,
                                        "RES-1",
                                        null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("tipo");
    }

    @Test
    void dadoUnDocenteActivoATiempoCompleto_cuandoRecibeUnCargo_entoncesPasa() {
        assertThatCode(() -> coordinador("550613A01-P-0301").verificarDocente(docente(true)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(
            value = EstadoLaboral.class,
            names = {"LICENCIA", "INACTIVO"})
    void dadoUnDocenteQueNoEstaActivo_cuandoRecibeUnCargo_entoncesNoEstaHabilitado(
            EstadoLaboral estado) {
        Docente enLicencia = docente(true).conEstado(estado);

        assertThatThrownBy(() -> coordinador("550613A01-P-0301").verificarDocente(enLicencia))
                .isInstanceOf(DocenteNoHabilitado.class)
                .hasMessageNotContaining(DocentesDePrueba.CEDULA);
    }

    @ParameterizedTest
    @EnumSource(
            value = Dedicacion.class,
            names = {"MT", "TP"})
    void dadoUnDocenteQueNoEsDeTiempoCompleto_cuandoRecibeUnCargo_entoncesNoEstaHabilitado(
            Dedicacion dedicacion) {
        Docente medioTiempo =
                docenteCon(
                        valores -> {
                            valores.dedicacion = dedicacion;
                            valores.horas = 20;
                            return valores;
                        });

        assertThatThrownBy(() -> coordinador("550613A01-P-0301").verificarDocente(medioTiempo))
                .isInstanceOf(DocenteNoHabilitado.class);
    }

    @Test
    void dadoUnCargo_cuandoSeCorrige_entoncesLaClaveNoCambiaYCadaCambioTraeSuAnteriorYSuNuevo() {
        Cargo antes = coordinador("550613A01-P-0301");
        Cargo finalizado =
                new Cargo(
                        antes.docente(),
                        antes.tipo(),
                        antes.fechaDesignacion(),
                        null,
                        "550613A01-P-0301",
                        LocalDate.of(2026, 3, 31),
                        "RES-PRUEBA-2026-099",
                        EstadoDeCargo.FINALIZADO);

        Cargo.Correccion correccion = antes.corregido(finalizado);

        assertThat(correccion.cargo()).isEqualTo(finalizado);
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor("fechaFin", "", "2026-03-31"),
                        new CambioDeValor(
                                "documento", "RES-PRUEBA-2025-004", "RES-PRUEBA-2026-099"),
                        new CambioDeValor("estado", "VIGENTE", "FINALIZADO"));
        assertThat(antes.corregido(antes).cambios()).isEmpty();
        assertThatThrownBy(() -> antes.corregido(cargo(TipoDeCargo.RECTOR, "RECT")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnCoordinadorVigente_cuandoSeFinaliza_entoncesDejaDeSerCoordinadorVigente() {
        assertThat(coordinador("550613A01-P-0301").esCoordinadorVigente()).isTrue();
        assertThat(cargo(TipoDeCargo.RECTOR, "RECT").esCoordinadorVigente()).isFalse();
    }

    @Test
    void dadosLos21CargosDeLaCarga08_cuandoSeVerifican_entoncesTodosSonValidos() {
        var docentes = CargasDePrueba.docentes();
        List<Cargo> cargos = CargasDePrueba.cargos();

        assertThat(cargos).hasSize(21);
        for (Cargo cargo : cargos) {
            referencias.verificar(cargo);
            Docente suDocente = docentes.get(cargo.docente());
            assertThat(suDocente).as(cargo.docente()).isNotNull();
            cargo.verificarDocente(suDocente);
            // En el periodo 2026-I ninguno tiene advertencias.
            assertThat(cargo.advertencias(suDocente, LocalDate.of(2026, 6, 1))).isEmpty();
        }
        assertThat(cargos.stream().filter(Cargo::esCoordinadorVigente).map(Cargo::carrera))
                .hasSize(7)
                .doesNotHaveDuplicates();
        // Los tres miembros de comisión terminan funciones el 2026-08-31: hoy siguen vigentes.
        assertThat(
                        cargos.stream()
                                .filter(
                                        cargo ->
                                                cargo.advertencias(
                                                                docentes.get(cargo.docente()),
                                                                CargasDePrueba.HOY)
                                                        .contains(
                                                                AdvertenciaDeCargo
                                                                        .FECHA_FIN_VENCIDA))
                                .map(Cargo::tipo))
                .containsExactly(
                        TipoDeCargo.MIEMBRO_COMISION,
                        TipoDeCargo.MIEMBRO_COMISION,
                        TipoDeCargo.MIEMBRO_COMISION);
    }

    private static Cargo coordinador(String carrera) {
        return new Cargo(
                DocentesDePrueba.CEDULA,
                TipoDeCargo.COORDINADOR_CARRERA,
                DESIGNACION,
                null,
                carrera,
                null,
                "RES-PRUEBA-2025-004",
                null);
    }

    private static Cargo cargo(TipoDeCargo tipo, String unidad) {
        return new Cargo(
                DocentesDePrueba.CEDULA, tipo, DESIGNACION, unidad, null, null, "RES-1", null);
    }

    private static Cargo conFin(LocalDate fin) {
        return new Cargo(
                DocentesDePrueba.CEDULA,
                TipoDeCargo.RECTOR,
                DESIGNACION,
                "RECT",
                null,
                fin,
                "RES-1",
                null);
    }

    private static Cargo conDocumento(String documento) {
        return new Cargo(
                DocentesDePrueba.CEDULA,
                TipoDeCargo.RECTOR,
                DESIGNACION,
                "RECT",
                null,
                null,
                documento,
                null);
    }

    /** Un docente activo a tiempo completo, con su maestría o sin títulos de cuarto nivel. */
    static Docente docente(boolean conCuartoNivel) {
        return docenteCon(
                valores -> {
                    if (!conCuartoNivel) {
                        FichaAcademica ficha = valores.ficha;
                        valores.ficha =
                                new FichaAcademica(
                                        ficha.tercerNivel(),
                                        List.of(),
                                        ficha.cursos(),
                                        ficha.experiencia(),
                                        ficha.asignaturasAfines());
                    }
                    return valores;
                });
    }

    private static Docente docenteCon(
            java.util.function.Function<DocentesDePrueba.Valores, DocentesDePrueba.Valores>
                    cambio) {
        return Docente.nuevo(
                new Identificacion(TipoDeIdentificacion.CEDULA, DocentesDePrueba.CEDULA),
                DocentesDePrueba.con(cambio),
                DocentesDePrueba.ANIO);
    }
}
