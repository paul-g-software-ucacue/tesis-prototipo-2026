package ec.mdd.docentes.adaptadores.salida;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Los cargos en {@code docentes.cargo}, con {@link JdbcClient} (F03-H05, data-model.md). */
public final class RepositorioCargosJdbc implements RepositorioCargos {

    private static final String COLUMNAS =
            "docente, tipo, fecha_designacion, unidad, carrera, fecha_fin, documento, estado";

    private static final String TODOS =
            "SELECT " + COLUMNAS + " FROM docentes.cargo ORDER BY docente, tipo, fecha_designacion";

    private static final String BUSCAR =
            "SELECT "
                    + COLUMNAS
                    + " FROM docentes.cargo WHERE docente = :docente AND tipo = :tipo"
                    + " AND fecha_designacion = :designacion";

    private static final String COORDINADOR_VIGENTE =
            "SELECT count(*) FROM docentes.cargo WHERE tipo = 'COORDINADOR_CARRERA'"
                    + " AND estado = 'VIGENTE' AND carrera = :carrera"
                    + " AND (docente, tipo, fecha_designacion) IS DISTINCT FROM"
                    + " (CAST(:docente AS varchar), CAST(:tipo AS varchar),"
                    + " CAST(:designacion AS date))";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR = "LOCK TABLE docentes.cargo IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR =
            "INSERT INTO docentes.cargo ("
                    + COLUMNAS
                    + ", registrado_en, registrado_por) VALUES (:docente, :tipo, :designacion,"
                    + " :unidad, :carrera, :fin, :documento, :estado, :momento, :quien)";

    private static final String GUARDAR =
            "UPDATE docentes.cargo SET unidad = :unidad, carrera = :carrera, fecha_fin = :fin,"
                    + " documento = :documento, estado = :estado WHERE docente = :docente"
                    + " AND tipo = :tipo AND fecha_designacion = :designacion";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioCargosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Cargo> todos() {
        return jdbc.sql(TODOS).query(RepositorioCargosJdbc::cargo).list();
    }

    @Override
    public Optional<Cargo> buscar(String docente, TipoDeCargo tipo, LocalDate fechaDesignacion) {
        return jdbc.sql(BUSCAR)
                .param("docente", docente)
                .param("tipo", tipo.name())
                .param("designacion", Date.valueOf(fechaDesignacion))
                .query(RepositorioCargosJdbc::cargo)
                .optional();
    }

    @Override
    public boolean hayCoordinadorVigente(String carrera, Cargo salvo) {
        return jdbc.sql(COORDINADOR_VIGENTE)
                        .param("carrera", carrera)
                        .param("docente", salvo == null ? null : salvo.docente())
                        .param("tipo", salvo == null ? null : salvo.tipo().name())
                        .param(
                                "designacion",
                                salvo == null ? null : Date.valueOf(salvo.fechaDesignacion()))
                        .query(Long.class)
                        .single()
                > 0;
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public void registrar(Cargo cargo, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR), cargo)
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(Cargo cargo) {
        conDatos(jdbc.sql(GUARDAR), cargo).update();
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sentencia, Cargo cargo) {
        return sentencia
                .param("docente", cargo.docente())
                .param("tipo", cargo.tipo().name())
                .param("designacion", Date.valueOf(cargo.fechaDesignacion()))
                .param("unidad", cargo.unidad())
                .param("carrera", cargo.carrera())
                .param("fin", cargo.fechaFin() == null ? null : Date.valueOf(cargo.fechaFin()))
                .param("documento", cargo.documento())
                .param("estado", cargo.estado().name());
    }

    private static Cargo cargo(ResultSet fila, int numero) throws SQLException {
        Date fin = fila.getDate("fecha_fin");
        return new Cargo(
                fila.getString("docente"),
                TipoDeCargo.valueOf(fila.getString("tipo")),
                fila.getDate("fecha_designacion").toLocalDate(),
                fila.getString("unidad"),
                fila.getString("carrera"),
                fin == null ? null : fin.toLocalDate(),
                fila.getString("documento"),
                EstadoDeCargo.valueOf(fila.getString("estado")));
    }
}
