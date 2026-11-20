package ec.mdd.firma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.infraestructura.FirmaFlywayConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Las tablas del documento firmable (F13-H02, data-model.md), migradas por un rol del proceso que
 * no es superusuario, como en producción: sus restricciones y que el documento registrado no se
 * cambia ni se borra (principio VII).
 */
class DocumentoFirmableEsquemaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.7-alpine");
    private static final String ROL = "distributivo_prueba";
    private static final String CLAVE = "clave-de-prueba";
    private static final String HASH = "a".repeat(64);
    private static final String SHA = "b".repeat(64);
    private static final String CADENA =
            "[{\"orden\":1,\"etiqueta\":\"Elaborado\",\"rol\":\"RESPONSABLE_CONSOLIDACION\"}]";

    private static JdbcClient comoProceso;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource superusuario =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcClient comoSuperusuario = JdbcClient.create(superusuario);
        comoSuperusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        comoSuperusuario.sql("create schema firma authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new FirmaFlywayConfig().flywayFirma(proceso).migrate();
        comoProceso = JdbcClient.create(proceso);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadaUnaGeneracionYSuDocumento_cuandoSeRegistran_entoncesQuedanUnoPorVersion() {
        generacion("P1", 1, "GENERADA", null);
        documento("P1", 1, UUID.randomUUID());

        assertThat(
                        comoProceso
                                .sql(
                                        "select count(*) from firma.documento_firmable where periodo = 'P1'")
                                .query(Integer.class)
                                .single())
                .isEqualTo(1);
        assertThatThrownBy(() -> documento("P1", 1, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnDocumentoRegistrado_cuandoElProcesoLoCambiaOLoBorra_entoncesSeRechaza() {
        generacion("P2", 1, "GENERADA", null);
        documento("P2", 1, UUID.randomUUID());

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.documento_firmable set archivo ="
                                                        + " 'otro' where periodo = 'P2'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.documento_firmable where periodo = 'P2'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaGeneracionFallida_cuandoNoTieneMotivo_entoncesSeRechaza() {
        assertThatThrownBy(() -> generacion("P3", 1, "FALLIDA", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> generacion("P3", 1, "GENERADA", "No debería tener motivo."))
                .isInstanceOf(DataAccessException.class);
        generacion("P3", 1, "FALLIDA", "Falta el nombre del Rector.");
    }

    @Test
    void dadoUnEstadoOUnHashInvalidos_cuandoSeGuardan_entoncesSeRechazan() {
        assertThatThrownBy(() -> generacion("P4", 1, "OTRO", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "insert into firma.generacion_documento (periodo,"
                                                        + " version, tipo, hash_contenido, cadena,"
                                                        + " rol_de_etapa, estado, pedida_por,"
                                                        + " actualizada_en) values ('P4', 1,"
                                                        + " 'CONSOLIDADO', 'XYZ', cast(:cadena as"
                                                        + " jsonb), 'RESPONSABLE_CONSOLIDACION',"
                                                        + " 'EN_CURSO', 'sistema', now())")
                                        .param("cadena", CADENA)
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnDocumento_cuandoSeRegistranSusFirmantesEnLaMismaTransaccion_entoncesQuedan() {
        generacion("P5", 1, "GENERADA", null);
        documento("P5", 1, UUID.randomUUID());
        firmante("P5", 1, "RESPONSABLE_CONSOLIDACION", "docente.demo", null);
        firmante("P5", 2, "VICERRECTOR", null, "0109990028");

        assertThat(
                        comoProceso
                                .sql(
                                        "select count(*) from firma.firmante_esperado where periodo = 'P5'")
                                .query(Integer.class)
                                .single())
                .isEqualTo(2);
    }

    @Test
    void dadoUnFirmante_cuandoNoTieneExactamenteUnaPersona_entoncesSeRechaza() {
        generacion("P6", 1, "GENERADA", null);
        documento("P6", 1, UUID.randomUUID());

        assertThatThrownBy(() -> firmante("P6", 1, "RECTOR", null, null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> firmante("P6", 1, "RECTOR", "rectorado.demo", "0109990010"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> firmante("P6", 1, "RECTOR", null, "abc"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnFirmanteRegistrado_cuandoElProcesoLoCambiaOLoBorra_entoncesSeRechaza() {
        generacion("P7", 1, "GENERADA", null);
        documento("P7", 1, UUID.randomUUID());
        firmante("P7", 1, "RESPONSABLE_CONSOLIDACION", "docente.demo", null);

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.firmante_esperado set usuario ="
                                                        + " 'otro' where periodo = 'P7'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.firmante_esperado where periodo = 'P7'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaSubidaAceptadaYUnaRechazada_cuandoSeRegistran_entoncesQuedanYElProcesoLasLee() {
        generacion("P8", 1, "GENERADA", null);
        documento("P8", 1, UUID.randomUUID());
        validacion("P8", 1, true, "[]");
        validacion("P8", 2, false, "[]");

        assertThat(
                        comoProceso
                                .sql(
                                        "select count(*) from firma.validacion_firma where periodo = 'P8'")
                                .query(Integer.class)
                                .single())
                .isEqualTo(2);
    }

    @Test
    void
            dadaUnaFirmaAceptada_cuandoSeRegistraOtraAceptadaDelMismoPaso_entoncesSeRechazaYUnaRechazadaNo() {
        generacion("P9", 1, "GENERADA", null);
        documento("P9", 1, UUID.randomUUID());
        validacion("P9", 1, true, "[]");

        assertThatThrownBy(() -> validacion("P9", 1, true, "[]"))
                .isInstanceOf(DataAccessException.class);
        validacion("P9", 1, false, "[]");
        validacion("P9", 1, false, "[]");
    }

    @Test
    void dadaUnaValidacionRegistrada_cuandoElProcesoLaCambiaOLaBorra_entoncesSeRechaza() {
        generacion("P10", 1, "GENERADA", null);
        documento("P10", 1, UUID.randomUUID());
        validacion("P10", 1, false, "[]");

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.validacion_firma set aceptada = true"
                                                        + " where periodo = 'P10'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.validacion_firma where periodo = 'P10'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaValidacionInvalida_cuandoSeGuarda_entoncesSeRechaza() {
        generacion("P11", 1, "GENERADA", null);
        documento("P11", 1, UUID.randomUUID());

        assertThatThrownBy(() -> validacion("P11", 0, false, "[]"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> validacion("P11", 1, false, "{}"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> validacion("P12", 1, false, "[]"))
                .as("sin generación")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnFirmanteConNombreImpresoYOtroSin_cuandoSeRegistran_entoncesQuedanYNoSeCambian() {
        generacion("P13", 1, "GENERADA", null);
        documento("P13", 1, UUID.randomUUID());
        comoProceso
                .sql(
                        "insert into firma.firmante_esperado (periodo, version, tipo, orden, rol,"
                                + " usuario, identificacion, nombre) values ('P13', 1, 'CONSOLIDADO',"
                                + " 1, 'RESPONSABLE_CONSOLIDACION', 'docente.demo', null,"
                                + " 'Docente (demo)')")
                .update();
        firmante("P13", 2, "VICERRECTOR", null, "0109990028");

        assertThat(
                        comoProceso
                                .sql(
                                        "select coalesce(nombre, '-') from firma.firmante_esperado"
                                                + " where periodo = 'P13' order by orden")
                                .query(String.class)
                                .list())
                .containsExactly("Docente (demo)", "-");
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.firmante_esperado set nombre = 'otro'"
                                                        + " where periodo = 'P13'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnRechazoEnFirma_cuandoSeRegistraOtroDelMismoDocumento_entoncesSeRechaza() {
        generacion("P14", 1, "GENERADA", null);
        documento("P14", 1, UUID.randomUUID());
        rechazo("P14", 2, "Falta la carga de un docente de Software.");

        assertThat(
                        comoProceso
                                .sql(
                                        "select observacion from firma.rechazo_en_firma"
                                                + " where periodo = 'P14'")
                                .query(String.class)
                                .single())
                .isEqualTo("Falta la carga de un docente de Software.");
        assertThatThrownBy(() -> rechazo("P14", 2, "Otra observación del mismo paso."))
                .as("un rechazo por documento")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnRechazoInvalido_cuandoSeGuarda_entoncesSeRechaza() {
        generacion("P15", 1, "GENERADA", null);
        documento("P15", 1, UUID.randomUUID());

        assertThatThrownBy(() -> rechazo("P15", 1, "   corta    "))
                .as("menos de 10 caracteres sin los espacios de los extremos")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> rechazo("P15", 0, "Una observación suficiente."))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> rechazo("P16", 1, "Una observación suficiente."))
                .as("sin generación")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnRechazoRegistrado_cuandoElProcesoLoCambiaOLoBorra_entoncesSeRechaza() {
        generacion("P17", 1, "GENERADA", null);
        documento("P17", 1, UUID.randomUUID());
        rechazo("P17", 1, "Una observación suficiente.");

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.rechazo_en_firma set observacion ="
                                                        + " 'Otra observación cualquiera.'"
                                                        + " where periodo = 'P17'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.rechazo_en_firma where periodo = 'P17'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    // F12-H04, V7: una anulación por documento, de un documento generado, sin reescrituras.
    @Test
    void dadaUnaAnulacionDeFirmas_cuandoSeRegistraOtraDelMismoDocumento_entoncesSeRechaza() {
        generacion("P18", 1, "GENERADA", null);
        documento("P18", 1, UUID.randomUUID());
        anulacion("P18", 2);

        assertThat(
                        comoProceso
                                .sql(
                                        "select firmas_anuladas from firma.anulacion_de_firmas"
                                                + " where periodo = 'P18'")
                                .query(Integer.class)
                                .single())
                .isEqualTo(2);
        assertThatThrownBy(() -> anulacion("P18", 2))
                .as("una anulación por documento")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaAnulacionInvalida_cuandoSeGuarda_entoncesSeRechaza() {
        generacion("P19", 1, "GENERADA", null);
        documento("P19", 1, UUID.randomUUID());

        assertThatThrownBy(() -> anulacion("P19", -1)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> anulacion("P20", 0))
                .as("sin generación")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaAnulacionRegistrada_cuandoElProcesoLaCambiaOLaBorra_entoncesSeRechaza() {
        generacion("P21", 1, "GENERADA", null);
        documento("P21", 1, UUID.randomUUID());
        anulacion("P21", 1);

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.anulacion_de_firmas set"
                                                        + " firmas_anuladas = 0 where periodo ="
                                                        + " 'P21'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.anulacion_de_firmas"
                                                        + " where periodo = 'P21'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    private static void anulacion(String periodo, int anuladas) {
        comoProceso
                .sql(
                        "insert into firma.anulacion_de_firmas (periodo, version, tipo, motivo,"
                                + " firmas_anuladas, anulada_por, anulada_en) values (:periodo, 1,"
                                + " 'CONSOLIDADO', 'RECHAZO_EN_FIRMA', :anuladas, 'sistema',"
                                + " :momento)")
                .param("periodo", periodo)
                .param("anuladas", anuladas)
                .param("momento", Timestamp.from(Instant.now()))
                .update();
    }

    private static void rechazo(String periodo, int orden, String observacion) {
        comoProceso
                .sql(
                        "insert into firma.rechazo_en_firma (periodo, version, tipo, orden,"
                                + " rechazado_por, rechazado_en, observacion) values (:periodo, 1,"
                                + " 'CONSOLIDADO', :orden, 'vicerrectorado.demo', :momento,"
                                + " :observacion)")
                .param("periodo", periodo)
                .param("orden", orden)
                .param("momento", Timestamp.from(Instant.now()))
                .param("observacion", observacion)
                .update();
    }

    private static void validacion(
            String periodo, int orden, boolean aceptada, String comprobaciones) {
        comoProceso
                .sql(
                        "insert into firma.validacion_firma (id, periodo, version, tipo, orden,"
                                + " subido_por, subido_en, sha256_subido, tamano, aceptada,"
                                + " comprobaciones, certificado) values (:id, :periodo, 1,"
                                + " 'CONSOLIDADO', :orden, 'docente.demo', :momento, :sha, 2048,"
                                + " :aceptada, cast(:comprobaciones as jsonb), null)")
                .param("id", UUID.randomUUID())
                .param("periodo", periodo)
                .param("orden", orden)
                .param("momento", Timestamp.from(Instant.now()))
                .param("sha", "c".repeat(64))
                .param("aceptada", aceptada)
                .param("comprobaciones", comprobaciones)
                .update();
    }

    private static void firmante(
            String periodo, int orden, String rol, String usuario, String identificacion) {
        comoProceso
                .sql(
                        "insert into firma.firmante_esperado (periodo, version, tipo, orden, rol,"
                                + " usuario, identificacion) values (:periodo, 1, 'CONSOLIDADO',"
                                + " :orden, :rol, :usuario, :identificacion)")
                .param("periodo", periodo)
                .param("orden", orden)
                .param("rol", rol)
                .param("usuario", usuario)
                .param("identificacion", identificacion)
                .update();
    }

    private static void generacion(String periodo, int version, String estado, String motivo) {
        comoProceso
                .sql(
                        "insert into firma.generacion_documento (periodo, version, tipo,"
                                + " hash_contenido, cadena, rol_de_etapa, estado, motivo,"
                                + " pedida_por, actualizada_en) values (:periodo, :version,"
                                + " 'CONSOLIDADO', :hash, cast(:cadena as jsonb),"
                                + " 'RESPONSABLE_CONSOLIDACION', :estado, :motivo, 'sistema',"
                                + " :momento)")
                .param("periodo", periodo)
                .param("version", version)
                .param("hash", HASH)
                .param("cadena", CADENA)
                .param("estado", estado)
                .param("motivo", motivo)
                .param("momento", Timestamp.from(Instant.now()))
                .update();
    }

    private static void documento(String periodo, int version, UUID id) {
        comoProceso
                .sql(
                        "insert into firma.documento_firmable (id, periodo, version, tipo,"
                                + " hash_contenido, sha256_original, archivo, tamano, generado_por,"
                                + " generado_en) values (:id, :periodo, :version, 'CONSOLIDADO',"
                                + " :hash, :sha, :archivo, 1024, 'sistema', :momento)")
                .param("id", id)
                .param("periodo", periodo)
                .param("version", version)
                .param("hash", HASH)
                .param("sha", SHA)
                .param("archivo", "documentos/" + SHA + ".pdf")
                .param("momento", Timestamp.from(Instant.now()))
                .update();
    }
}
