package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Las generaciones y los documentos firmables en el esquema {@code firma} (F13-H02, data-model.md).
 * La cadena de firmas va en jsonb. El documento solo se inserta: el rol del proceso no puede
 * cambiarlo ni borrarlo (V2).
 */
public final class RepositorioDocumentosJdbc implements RepositorioDocumentos {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<PasoDeFirma>> CADENA = new TypeReference<>() {};
    private static final String TIPO = DocumentoFirmable.CONSOLIDADO;

    private static final String GENERACION =
            "SELECT periodo, version, hash_contenido, cadena::text AS cadena, rol_de_etapa,"
                    + " estado, motivo, pedida_por, actualizada_en"
                    + " FROM firma.generacion_documento";

    // Solo los documentos de una generación GENERADA: un registro sin su generación completa no se
    // firma.
    private static final String DOCUMENTO_CON_CADENA =
            "SELECT d.id, d.periodo, d.version, d.tipo, d.hash_contenido, d.sha256_original,"
                    + " d.archivo, d.tamano, d.generado_por, d.generado_en, g.cadena::text AS cadena"
                    + " FROM firma.documento_firmable d JOIN firma.generacion_documento g"
                    + " ON g.periodo = d.periodo AND g.version = d.version AND g.tipo = d.tipo"
                    + " AND g.estado = 'GENERADA'";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioDocumentosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<GeneracionDeDocumento> bloquear(String periodo, int version) {
        return jdbc.sql(
                        GENERACION
                                + " WHERE periodo = :periodo AND version = :version AND tipo = :tipo"
                                + " FOR UPDATE")
                .param("periodo", periodo)
                .param("version", version)
                .param("tipo", TIPO)
                .query(RepositorioDocumentosJdbc::generacion)
                .optional();
    }

    @Override
    public Optional<GeneracionDeDocumento> ultima(String periodo) {
        return jdbc.sql(
                        GENERACION
                                + " WHERE periodo = :periodo AND tipo = :tipo"
                                + " ORDER BY version DESC LIMIT 1")
                .param("periodo", periodo)
                .param("tipo", TIPO)
                .query(RepositorioDocumentosJdbc::generacion)
                .optional();
    }

    @Override
    public void guardar(GeneracionDeDocumento generacion) {
        jdbc.sql(
                        "INSERT INTO firma.generacion_documento (periodo, version, tipo,"
                                + " hash_contenido, cadena, rol_de_etapa, estado, motivo,"
                                + " pedida_por, actualizada_en) VALUES (:periodo, :version, :tipo,"
                                + " :hash, CAST(:cadena AS jsonb), :rol, :estado, :motivo,"
                                + " :pedidaPor, :momento)"
                                + " ON CONFLICT (periodo, version, tipo) DO UPDATE SET"
                                + " estado = EXCLUDED.estado, motivo = EXCLUDED.motivo,"
                                + " pedida_por = EXCLUDED.pedida_por,"
                                + " actualizada_en = EXCLUDED.actualizada_en")
                .param("periodo", generacion.periodo())
                .param("version", generacion.version())
                .param("tipo", TIPO)
                .param("hash", generacion.hashContenido())
                .param("cadena", JSON.writeValueAsString(generacion.cadena()))
                .param("rol", generacion.rolDeLaEtapa())
                .param("estado", generacion.estado().name())
                .param("motivo", generacion.motivo().orElse(null))
                .param("pedidaPor", generacion.pedidaPor())
                .param("momento", Timestamp.from(generacion.actualizadaEn()))
                .update();
    }

    @Override
    public List<FirmasDelDocumento> generados() {
        return jdbc.sql(DOCUMENTO_CON_CADENA + " ORDER BY d.periodo, d.version")
                .query(this::firmasDe)
                .list();
    }

    @Override
    public Optional<FirmasDelDocumento> firmas(UUID documento) {
        return jdbc.sql(DOCUMENTO_CON_CADENA + " WHERE d.id = :id")
                .param("id", documento)
                .query(this::firmasDe)
                .optional();
    }

    @Override
    public void registrar(DocumentoFirmable documento, List<FirmanteEsperado> firmantes) {
        jdbc.sql(
                        "INSERT INTO firma.documento_firmable (id, periodo, version, tipo,"
                                + " hash_contenido, sha256_original, archivo, tamano, generado_por,"
                                + " generado_en) VALUES (:id, :periodo, :version, :tipo, :hash,"
                                + " :sha, :archivo, :tamano, :por, :en)")
                .param("id", documento.id())
                .param("periodo", documento.periodo())
                .param("version", documento.version())
                .param("tipo", documento.tipo())
                .param("hash", documento.hashContenido())
                .param("sha", documento.sha256Original())
                .param("archivo", documento.archivo())
                .param("tamano", documento.tamano())
                .param("por", documento.generadoPor())
                .param("en", Timestamp.from(documento.generadoEn()))
                .update();
        for (FirmanteEsperado firmante : firmantes) {
            jdbc.sql(
                            "INSERT INTO firma.firmante_esperado (periodo, version, tipo, orden,"
                                    + " rol, usuario, identificacion) VALUES (:periodo, :version,"
                                    + " :tipo, :orden, :rol, :usuario, :identificacion)")
                    .param("periodo", documento.periodo())
                    .param("version", documento.version())
                    .param("tipo", documento.tipo())
                    .param("orden", firmante.orden())
                    .param("rol", firmante.rol())
                    .param("usuario", firmante.usuario())
                    .param("identificacion", firmante.identificacion())
                    .update();
        }
    }

    @Override
    public Optional<DocumentoFirmable> documento(String periodo, int version) {
        return jdbc.sql(
                        "SELECT id, periodo, version, tipo, hash_contenido, sha256_original,"
                                + " archivo, tamano, generado_por, generado_en"
                                + " FROM firma.documento_firmable"
                                + " WHERE periodo = :periodo AND version = :version AND tipo = :tipo")
                .param("periodo", periodo)
                .param("version", version)
                .param("tipo", TIPO)
                .query((fila, numero) -> documento(fila))
                .optional();
    }

    // El documento, la cadena de su generación y sus firmantes esperados.
    private FirmasDelDocumento firmasDe(ResultSet fila, int numero) throws SQLException {
        DocumentoFirmable documento = documento(fila);
        List<FirmanteEsperado> firmantes =
                jdbc.sql(
                                "SELECT orden, rol, usuario, identificacion"
                                        + " FROM firma.firmante_esperado WHERE periodo = :periodo"
                                        + " AND version = :version AND tipo = :tipo ORDER BY orden")
                        .param("periodo", documento.periodo())
                        .param("version", documento.version())
                        .param("tipo", documento.tipo())
                        .query(
                                (una, n) ->
                                        new FirmanteEsperado(
                                                una.getInt("orden"),
                                                una.getString("rol"),
                                                una.getString("usuario"),
                                                una.getString("identificacion")))
                        .list();
        return new FirmasDelDocumento(
                documento, JSON.readValue(fila.getString("cadena"), CADENA), firmantes);
    }

    private static DocumentoFirmable documento(ResultSet fila) throws SQLException {
        return new DocumentoFirmable(
                fila.getObject("id", UUID.class),
                fila.getString("periodo"),
                fila.getInt("version"),
                fila.getString("tipo"),
                fila.getString("hash_contenido"),
                fila.getString("sha256_original"),
                fila.getString("archivo"),
                fila.getInt("tamano"),
                fila.getString("generado_por"),
                fila.getTimestamp("generado_en").toInstant());
    }

    private static GeneracionDeDocumento generacion(ResultSet fila, int numero)
            throws SQLException {
        return GeneracionDeDocumento.de(
                fila.getString("periodo"),
                fila.getInt("version"),
                fila.getString("hash_contenido"),
                JSON.readValue(fila.getString("cadena"), CADENA),
                fila.getString("rol_de_etapa"),
                EstadoDeGeneracion.valueOf(fila.getString("estado")),
                Optional.ofNullable(fila.getString("motivo")),
                fila.getString("pedida_por"),
                fila.getTimestamp("actualizada_en").toInstant());
    }
}
