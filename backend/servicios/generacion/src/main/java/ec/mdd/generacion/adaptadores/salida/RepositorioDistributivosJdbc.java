package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.MotivoDeVersion;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Los distributivos en el esquema {@code generacion}. */
public final class RepositorioDistributivosJdbc implements RepositorioDistributivos {

    private static final String VIGENTE =
            "SELECT v.id, v.numero, v.estado FROM generacion.version_distributivo v"
                    + " JOIN generacion.distributivo d ON d.id = v.distributivo_id"
                    + " WHERE d.periodo = :periodo ORDER BY v.numero DESC LIMIT 1";

    private static final String POR_NUMERO =
            "SELECT v.id, v.numero, v.estado FROM generacion.version_distributivo v"
                    + " JOIN generacion.distributivo d ON d.id = v.distributivo_id"
                    + " WHERE d.periodo = :periodo AND v.numero = :numero";

    private static final String PARCIALES =
            "SELECT carrera, estado FROM generacion.parcial_carrera WHERE version_id = :version"
                    + " ORDER BY carrera";

    private static final String PROPUESTAS =
            "SELECT asignatura, paralelo, carrera, docente, nombre_docente, justificacion, texto,"
                    + " propuesta_en, propuesta_por FROM generacion.propuesta"
                    + " WHERE version_id = :version ORDER BY asignatura, paralelo";

    private static final String ACTIVIDADES =
            "SELECT carrera, docente, nombre_docente, actividad, nombre_actividad, componente,"
                    + " proyecto, nombre_proyecto, horas, asignada_en, asignada_por"
                    + " FROM generacion.actividad_no_lectiva WHERE version_id = :version"
                    + " ORDER BY carrera, nombre_docente, actividad";

    private static final String GUARDAR_ACTIVIDAD =
            "INSERT INTO generacion.actividad_no_lectiva (id, version_id, carrera, docente,"
                    + " nombre_docente, actividad, nombre_actividad, componente, proyecto,"
                    + " nombre_proyecto, horas, asignada_en, asignada_por)"
                    + " VALUES (:id, :version, :carrera, :docente, :nombreDocente, :actividad,"
                    + " :nombreActividad, :componente, :proyecto, :nombreProyecto, :horas,"
                    + " :momento, :quien)"
                    + " ON CONFLICT (version_id, carrera, docente, actividad,"
                    + " coalesce(proyecto, '')) DO UPDATE SET"
                    + " nombre_docente = EXCLUDED.nombre_docente,"
                    + " nombre_actividad = EXCLUDED.nombre_actividad,"
                    + " componente = EXCLUDED.componente,"
                    + " nombre_proyecto = EXCLUDED.nombre_proyecto, horas = EXCLUDED.horas,"
                    + " asignada_en = EXCLUDED.asignada_en,"
                    + " asignada_por = EXCLUDED.asignada_por";

    private static final String QUITAR_ACTIVIDAD =
            "DELETE FROM generacion.actividad_no_lectiva WHERE version_id = :version"
                    + " AND carrera = :carrera AND docente = :docente"
                    + " AND actividad = :actividad AND coalesce(proyecto, '') = :proyecto";

    private static final String CREAR_DISTRIBUTIVO =
            "INSERT INTO generacion.distributivo (id, periodo, creado_en, creado_por)"
                    + " VALUES (:id, :periodo, :momento, :quien)";

    private static final String CREAR_VERSION =
            "INSERT INTO generacion.version_distributivo (id, distributivo_id, numero, estado)"
                    + " VALUES (:id, :distributivo, :numero, :estado)";

    private static final String AGREGAR_PARCIAL =
            "INSERT INTO generacion.parcial_carrera (version_id, carrera, estado)"
                    + " VALUES (:version, :carrera, :estado)"
                    + " ON CONFLICT (version_id, carrera) DO NOTHING";

    private static final String GUARDAR_PROPUESTA =
            "INSERT INTO generacion.propuesta (version_id, asignatura, paralelo, carrera, docente,"
                    + " nombre_docente, justificacion, texto, propuesta_en, propuesta_por)"
                    + " VALUES (:version, :asignatura, :paralelo, :carrera, :docente, :nombre,"
                    + " :justificacion, :texto, :momento, :quien)"
                    + " ON CONFLICT (version_id, asignatura, paralelo) DO UPDATE SET"
                    + " docente = EXCLUDED.docente, nombre_docente = EXCLUDED.nombre_docente,"
                    + " justificacion = EXCLUDED.justificacion, texto = EXCLUDED.texto,"
                    + " propuesta_en = EXCLUDED.propuesta_en,"
                    + " propuesta_por = EXCLUDED.propuesta_por";

    private static final String QUITAR_PROPUESTA =
            "DELETE FROM generacion.propuesta WHERE version_id = :version"
                    + " AND asignatura = :asignatura AND paralelo = :paralelo";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioDistributivosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<VersionDistributivo> vigenteDe(String periodo) {
        return leer(VIGENTE, periodo);
    }

    // F18-H02: primero el distributivo, después la vigente, en otra sentencia. Quien esperaba el
    // bloqueo lee así la versión que confirmó el anterior: bloquear solo la fila de la vigente
    // devolvía, al soltarse, la misma fila ya superada, y dos reformas a la vez creaban la misma
    // versión dos veces.
    @Override
    public Optional<VersionDistributivo> bloquearVigente(String periodo) {
        jdbc.sql("SELECT id FROM generacion.distributivo WHERE periodo = :periodo FOR UPDATE")
                .param("periodo", periodo)
                .query(UUID.class)
                .optional();
        return leer(VIGENTE + " FOR UPDATE OF v", periodo);
    }

    private Optional<VersionDistributivo> leer(String consulta, String periodo) {
        return jdbc.sql(consulta)
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                new Cabecera(
                                        fila.getObject("id", UUID.class),
                                        fila.getInt("numero"),
                                        EstadoDeVersion.valueOf(fila.getString("estado"))))
                .optional()
                .map(this::completa);
    }

    private VersionDistributivo completa(Cabecera cabecera) {
        List<ParcialCarrera> parciales =
                jdbc.sql(PARCIALES)
                        .param("version", cabecera.id())
                        .query(
                                (fila, numero) ->
                                        new ParcialCarrera(
                                                fila.getString("carrera"),
                                                EstadoDelParcial.valueOf(fila.getString("estado"))))
                        .list();
        List<Propuesta> propuestas =
                jdbc.sql(PROPUESTAS)
                        .param("version", cabecera.id())
                        .query((fila, numero) -> propuesta(fila))
                        .list();
        List<ActividadNoLectiva> actividades =
                jdbc.sql(ACTIVIDADES)
                        .param("version", cabecera.id())
                        .query((fila, numero) -> actividad(fila))
                        .list();
        return new VersionDistributivo(
                cabecera.id(),
                cabecera.numero(),
                cabecera.estado(),
                parciales,
                propuestas,
                actividades);
    }

    private static ActividadNoLectiva actividad(ResultSet fila) throws SQLException {
        return new ActividadNoLectiva(
                fila.getString("carrera"),
                fila.getString("docente"),
                fila.getString("nombre_docente"),
                fila.getString("actividad"),
                fila.getString("nombre_actividad"),
                Componente.valueOf(fila.getString("componente")),
                fila.getString("proyecto"),
                fila.getString("nombre_proyecto"),
                fila.getBigDecimal("horas"),
                fila.getTimestamp("asignada_en").toInstant(),
                fila.getString("asignada_por"));
    }

    private static Propuesta propuesta(ResultSet fila) throws SQLException {
        return new Propuesta(
                fila.getString("asignatura"),
                fila.getString("paralelo"),
                fila.getString("carrera"),
                fila.getString("docente"),
                fila.getString("nombre_docente"),
                new Justificacion(
                        TipoDeJustificacion.valueOf(fila.getString("justificacion")),
                        fila.getString("texto")),
                fila.getTimestamp("propuesta_en").toInstant(),
                fila.getString("propuesta_por"));
    }

    @Override
    public void crear(String periodo, VersionDistributivo version, String quien, Instant momento) {
        UUID distributivo = UUID.randomUUID();
        try {
            jdbc.sql(CREAR_DISTRIBUTIVO)
                    .param("id", distributivo)
                    .param("periodo", periodo)
                    .param("momento", Timestamp.from(momento))
                    .param("quien", quien)
                    .update();
        } catch (DuplicateKeyException repetido) {
            throw new DistributivoRepetido();
        }
        jdbc.sql(CREAR_VERSION)
                .param("id", version.id())
                .param("distributivo", distributivo)
                .param("numero", version.numero())
                .param("estado", version.estado().name())
                .update();
        version.parciales().forEach(parcial -> agregarParcial(version.id(), parcial));
    }

    @Override
    public void agregarParcial(UUID version, ParcialCarrera parcial) {
        jdbc.sql(AGREGAR_PARCIAL)
                .param("version", version)
                .param("carrera", parcial.carrera())
                .param("estado", parcial.estado().name())
                .update();
    }

    @Override
    public void guardarPropuesta(UUID version, Propuesta propuesta) {
        jdbc.sql(GUARDAR_PROPUESTA)
                .param("version", version)
                .param("asignatura", propuesta.asignatura())
                .param("paralelo", propuesta.paralelo())
                .param("carrera", propuesta.carrera())
                .param("docente", propuesta.docente())
                .param("nombre", propuesta.nombreDocente())
                .param("justificacion", propuesta.justificacion().tipo().name())
                .param("texto", propuesta.justificacion().texto())
                .param("momento", Timestamp.from(propuesta.propuestaEn()))
                .param("quien", propuesta.propuestaPor())
                .update();
    }

    @Override
    public boolean quitarPropuesta(UUID version, String asignatura, String paralelo) {
        return jdbc.sql(QUITAR_PROPUESTA)
                        .param("version", version)
                        .param("asignatura", asignatura)
                        .param("paralelo", paralelo)
                        .update()
                > 0;
    }

    @Override
    public void guardarActividad(UUID version, ActividadNoLectiva actividad) {
        jdbc.sql(GUARDAR_ACTIVIDAD)
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("carrera", actividad.carrera())
                .param("docente", actividad.docente())
                .param("nombreDocente", actividad.nombreDocente())
                .param("actividad", actividad.actividad())
                .param("nombreActividad", actividad.nombreActividad())
                .param("componente", actividad.componente().name())
                .param("proyecto", actividad.proyecto())
                .param("nombreProyecto", actividad.nombreProyecto())
                .param("horas", actividad.horas())
                .param("momento", Timestamp.from(actividad.asignadaEn()))
                .param("quien", actividad.asignadaPor())
                .update();
    }

    @Override
    public boolean quitarActividad(
            UUID version, String carrera, String docente, String actividad, String proyecto) {
        return jdbc.sql(QUITAR_ACTIVIDAD)
                        .param("version", version)
                        .param("carrera", carrera)
                        .param("docente", docente)
                        .param("actividad", actividad)
                        .param("proyecto", proyecto == null ? "" : proyecto)
                        .update()
                > 0;
    }

    private record Cabecera(UUID id, int numero, EstadoDeVersion estado) {}

    @Override
    public Optional<Instant> creadoEn(String periodo) {
        return jdbc.sql("SELECT creado_en FROM generacion.distributivo WHERE periodo = :periodo")
                .param("periodo", periodo)
                .query((fila, numero) -> fila.getTimestamp("creado_en").toInstant())
                .optional();
    }

    @Override
    public void marcarParcial(UUID version, String carrera, EstadoDelParcial estado) {
        jdbc.sql(
                        "UPDATE generacion.parcial_carrera SET estado = :estado"
                                + " WHERE version_id = :version AND carrera = :carrera")
                .param("estado", estado.name())
                .param("version", version)
                .param("carrera", carrera)
                .update();
    }

    @Override
    public void congelar(UUID version) {
        jdbc.sql(
                        "UPDATE generacion.version_distributivo SET estado = 'CONGELADA'"
                                + " WHERE id = :version")
                .param("version", version)
                .update();
    }

    // F13-H01: FOR SHARE, para que congelar (FOR UPDATE de la versión) espere a la operación que
    // ya leyó la versión editable.
    @Override
    public OptionalInt versionCongelada(String periodo) {
        return jdbc.sql(VIGENTE + " FOR SHARE OF v")
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                EstadoDeVersion.valueOf(fila.getString("estado"))
                                                == EstadoDeVersion.CONGELADA
                                        ? OptionalInt.of(fila.getInt("numero"))
                                        : OptionalInt.empty())
                .optional()
                .orElse(OptionalInt.empty());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copia con {@code INSERT … SELECT}, en la transacción en curso, cada tabla que cuelga de la
     * versión, con ids nuevos donde los hay: los parciales (antes, por sus foráneas), las
     * propuestas, las actividades, las justificaciones, las consolidaciones y los informes. Los
     * momentos y los autores se conservan, así que la consolidación copiada no queda
     * desactualizada. El informe comparte su archivo, que el almacén no reescribe ni borra (V8).
     * Una tabla nueva que dependa de la versión tiene que sumarse aquí.
     */
    @Override
    public UUID crearVersionNueva(
            UUID anulada, MotivoDeVersion motivo, String quien, Instant momento) {
        UUID nueva = UUID.randomUUID();
        // F18-H02, D-113: la publicada de una reforma no se anula; queda CONGELADA, histórica.
        if (motivo != MotivoDeVersion.REFORMA) {
            jdbc.sql(
                            "UPDATE generacion.version_distributivo SET estado = 'ANULADA'"
                                    + " WHERE id = :anulada")
                    .param("anulada", anulada)
                    .update();
        }
        jdbc.sql(
                        "INSERT INTO generacion.version_distributivo (id, distributivo_id, numero,"
                                + " estado, motivo, creada_por, creada_en) SELECT :nueva,"
                                + " distributivo_id, numero + 1, 'EDITABLE', :motivo, :quien, :en"
                                + " FROM generacion.version_distributivo WHERE id = :anulada")
                .param("nueva", nueva)
                .param("motivo", motivo.name())
                .param("quien", quien)
                .param("en", Timestamp.from(momento))
                .param("anulada", anulada)
                .update();
        for (String copia : COPIAS) {
            jdbc.sql(copia).param("nueva", nueva).param("anulada", anulada).update();
        }
        return nueva;
    }

    // F12-H04: las tablas de la versión, en el orden de sus foráneas.
    private static final List<String> COPIAS =
            List.of(
                    "INSERT INTO generacion.parcial_carrera (version_id, carrera, estado)"
                            + " SELECT :nueva, carrera, estado FROM generacion.parcial_carrera"
                            + " WHERE version_id = :anulada",
                    "INSERT INTO generacion.propuesta (version_id, asignatura, paralelo, carrera,"
                            + " docente, nombre_docente, justificacion, texto, propuesta_en,"
                            + " propuesta_por) SELECT :nueva, asignatura, paralelo, carrera,"
                            + " docente, nombre_docente, justificacion, texto, propuesta_en,"
                            + " propuesta_por FROM generacion.propuesta WHERE version_id ="
                            + " :anulada",
                    "INSERT INTO generacion.actividad_no_lectiva (id, version_id, carrera,"
                            + " docente, nombre_docente, actividad, nombre_actividad, componente,"
                            + " proyecto, nombre_proyecto, horas, asignada_en, asignada_por)"
                            + " SELECT gen_random_uuid(), :nueva, carrera, docente,"
                            + " nombre_docente, actividad, nombre_actividad, componente, proyecto,"
                            + " nombre_proyecto, horas, asignada_en, asignada_por FROM"
                            + " generacion.actividad_no_lectiva WHERE version_id = :anulada",
                    "INSERT INTO generacion.justificacion_inconsistencia (id, version_id, tipo,"
                            + " objeto, texto, justificada_por, justificada_en) SELECT"
                            + " gen_random_uuid(), :nueva, tipo, objeto, texto, justificada_por,"
                            + " justificada_en FROM generacion.justificacion_inconsistencia"
                            + " WHERE version_id = :anulada",
                    "INSERT INTO generacion.consolidacion (id, version_id, consolidada_por,"
                            + " consolidada_en) SELECT gen_random_uuid(), :nueva,"
                            + " consolidada_por, consolidada_en FROM generacion.consolidacion"
                            + " WHERE version_id = :anulada",
                    "INSERT INTO generacion.informe_sustentado (id, version_id, nombre, tamano,"
                            + " huella, clave, adjuntado_por, adjuntado_en) SELECT"
                            + " gen_random_uuid(), :nueva, nombre, tamano, huella, clave,"
                            + " adjuntado_por, adjuntado_en FROM generacion.informe_sustentado"
                            + " WHERE version_id = :anulada");

    @Override
    public Optional<MotivoDeVersion> motivoDe(String periodo, int numero) {
        return jdbc.sql(
                        "SELECT v.motivo FROM generacion.version_distributivo v"
                                + " JOIN generacion.distributivo d ON d.id = v.distributivo_id"
                                + " WHERE d.periodo = :periodo AND v.numero = :numero"
                                + " AND v.motivo IS NOT NULL")
                .param("periodo", periodo)
                .param("numero", numero)
                .query(String.class)
                .optional()
                .map(MotivoDeVersion::valueOf);
    }

    @Override
    public Optional<EstadoDeVersion> estadoDe(String periodo, int numero) {
        return jdbc.sql(
                        "SELECT v.estado FROM generacion.version_distributivo v"
                                + " JOIN generacion.distributivo d ON d.id = v.distributivo_id"
                                + " WHERE d.periodo = :periodo AND v.numero = :numero")
                .param("periodo", periodo)
                .param("numero", numero)
                .query(String.class)
                .optional()
                .map(EstadoDeVersion::valueOf);
    }

    @Override
    public Optional<VersionDistributivo> version(String periodo, int numero) {
        return jdbc.sql(POR_NUMERO)
                .param("periodo", periodo)
                .param("numero", numero)
                .query(
                        (fila, n) ->
                                new Cabecera(
                                        fila.getObject("id", UUID.class),
                                        fila.getInt("numero"),
                                        EstadoDeVersion.valueOf(fila.getString("estado"))))
                .optional()
                .map(this::completa);
    }
}
