package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoDeMalla;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Las carreras, las mallas y las asignaturas en {@code academico.carrera}, {@code malla} y {@code
 * asignatura}, y los campos amplios en {@code campo_amplio}, con {@link JdbcClient}. Las jornadas y
 * las palabras clave son arreglos de PostgreSQL, en su orden.
 */
public final class RepositorioCarrerasJdbc implements RepositorioCarreras {

    private static final String CAMPOS =
            "SELECT codigo, nombre FROM academico.campo_amplio ORDER BY codigo";

    private static final String CARRERAS =
            "SELECT codigo, nombre, siglas, sede, nivel_formacion, modalidad, jornadas,"
                    + " campo_amplio, campo_especifico, campo_detallado, numero_ciclos, estado"
                    + " FROM academico.carrera ORDER BY codigo";

    private static final String MALLAS =
            "SELECT codigo, carrera, estado FROM academico.malla ORDER BY codigo";

    private static final String ASIGNATURAS =
            "SELECT codigo, carrera, malla, nombre, ciclo, unidad_organizacion_curricular,"
                    + " horas_semanales_clase, horas_practicas_semanales, creditos, tronco_comun,"
                    + " campo_amplio, campo_especifico, palabras_clave, activa"
                    + " FROM academico.asignatura ORDER BY codigo";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR =
            "LOCK TABLE academico.carrera, academico.malla, academico.asignatura"
                    + " IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR_CARRERA =
            "INSERT INTO academico.carrera (codigo, nombre, siglas, sede, nivel_formacion,"
                    + " modalidad, jornadas, campo_amplio, campo_especifico, campo_detallado,"
                    + " numero_ciclos, estado, registrada_en, registrada_por) VALUES (:codigo,"
                    + " :nombre, :siglas, :sede, :nivel, :modalidad, :jornadas, :amplio,"
                    + " :especifico, :detallado, :ciclos, :estado, :momento, :quien)";

    private static final String GUARDAR_CARRERA =
            "UPDATE academico.carrera SET nombre = :nombre, siglas = :siglas, sede = :sede,"
                    + " nivel_formacion = :nivel, modalidad = :modalidad, jornadas = :jornadas,"
                    + " campo_amplio = :amplio, campo_especifico = :especifico, campo_detallado ="
                    + " :detallado, numero_ciclos = :ciclos, estado = :estado WHERE codigo ="
                    + " :codigo";

    private static final String REGISTRAR_MALLA =
            "INSERT INTO academico.malla (codigo, carrera, estado, registrada_en, registrada_por)"
                    + " VALUES (:codigo, :carrera, :estado, :momento, :quien)";

    private static final String GUARDAR_MALLA =
            "UPDATE academico.malla SET estado = :estado WHERE codigo = :codigo";

    private static final String REGISTRAR_ASIGNATURA =
            "INSERT INTO academico.asignatura (codigo, carrera, malla, nombre, ciclo,"
                    + " unidad_organizacion_curricular, horas_semanales_clase,"
                    + " horas_practicas_semanales, creditos, tronco_comun, campo_amplio,"
                    + " campo_especifico, palabras_clave, activa, registrada_en, registrada_por)"
                    + " VALUES (:codigo, :carrera, :malla, :nombre, :ciclo, :unidad, :horas,"
                    + " :practicas, :creditos, :tronco, :amplio, :especifico, :palabras, :activa,"
                    + " :momento, :quien)";

    private static final String GUARDAR_ASIGNATURA =
            "UPDATE academico.asignatura SET nombre = :nombre, ciclo = :ciclo,"
                    + " unidad_organizacion_curricular = :unidad, horas_semanales_clase = :horas,"
                    + " horas_practicas_semanales = :practicas, creditos = :creditos, tronco_comun"
                    + " = :tronco, campo_amplio = :amplio, campo_especifico = :especifico,"
                    + " palabras_clave = :palabras, activa = :activa WHERE codigo = :codigo";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioCarrerasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CampoAmplio> camposAmplios() {
        return jdbc.sql(CAMPOS)
                .query(
                        (fila, numero) ->
                                new CampoAmplio(fila.getString("codigo"), fila.getString("nombre")))
                .list();
    }

    @Override
    public List<Carrera> carreras() {
        return jdbc.sql(CARRERAS).query(RepositorioCarrerasJdbc::carrera).list();
    }

    @Override
    public List<Malla> mallas() {
        return jdbc.sql(MALLAS)
                .query(
                        (fila, numero) ->
                                new Malla(
                                        fila.getString("codigo"),
                                        fila.getString("carrera"),
                                        EstadoDeMalla.valueOf(fila.getString("estado"))))
                .list();
    }

    @Override
    public List<Asignatura> asignaturas() {
        return jdbc.sql(ASIGNATURAS).query(RepositorioCarrerasJdbc::asignatura).list();
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public void registrar(Carrera carrera, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR_CARRERA), carrera)
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void registrar(Malla malla, String quien, Instant momento) {
        jdbc.sql(REGISTRAR_MALLA)
                .param("codigo", malla.codigo())
                .param("carrera", malla.carrera())
                .param("estado", malla.estado().name())
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void registrar(Asignatura asignatura, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR_ASIGNATURA), asignatura)
                .param("carrera", asignatura.carrera())
                .param("malla", asignatura.malla())
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(Carrera carrera) {
        conDatos(jdbc.sql(GUARDAR_CARRERA), carrera).update();
    }

    @Override
    public void guardar(Malla malla) {
        jdbc.sql(GUARDAR_MALLA)
                .param("codigo", malla.codigo())
                .param("estado", malla.estado().name())
                .update();
    }

    @Override
    public void guardar(Asignatura asignatura) {
        conDatos(jdbc.sql(GUARDAR_ASIGNATURA), asignatura).update();
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sql, Carrera carrera) {
        return sql.param("codigo", carrera.codigo())
                .param("nombre", carrera.nombre())
                .param("siglas", carrera.siglas())
                .param("sede", carrera.sede())
                .param("nivel", carrera.nivelFormacion().name())
                .param("modalidad", carrera.modalidad().name())
                .param(
                        "jornadas",
                        carrera.jornadas().stream().map(Enum::name).toArray(String[]::new))
                .param("amplio", carrera.campo().amplio())
                .param("especifico", carrera.campo().especifico())
                .param("detallado", carrera.campoDetallado())
                .param("ciclos", carrera.numeroCiclos())
                .param("estado", carrera.estado().name());
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sql, Asignatura asignatura) {
        return sql.param("codigo", asignatura.codigo())
                .param("nombre", asignatura.nombre())
                .param("ciclo", asignatura.ciclo())
                .param("unidad", asignatura.unidadOrganizacionCurricular().name())
                .param("horas", asignatura.horasSemanalesClase())
                .param("practicas", asignatura.horasPracticasSemanales())
                .param("creditos", asignatura.creditos())
                .param("tronco", asignatura.troncoComun())
                .param("amplio", asignatura.campo().amplio())
                .param("especifico", asignatura.campo().especifico())
                .param("palabras", asignatura.palabrasClave().toArray(String[]::new))
                .param("activa", asignatura.activa());
    }

    private static Carrera carrera(ResultSet fila, int numero) throws SQLException {
        return new Carrera(
                fila.getString("codigo"),
                fila.getString("nombre"),
                fila.getString("siglas"),
                fila.getString("sede"),
                NivelDeFormacion.valueOf(fila.getString("nivel_formacion")),
                Modalidad.valueOf(fila.getString("modalidad")),
                Arrays.stream(textos(fila.getArray("jornadas"))).map(Jornada::valueOf).toList(),
                new CampoConocimiento(
                        fila.getString("campo_amplio"), fila.getString("campo_especifico")),
                fila.getString("campo_detallado"),
                fila.getInt("numero_ciclos"),
                EstadoDeCarrera.valueOf(fila.getString("estado")));
    }

    private static Asignatura asignatura(ResultSet fila, int numero) throws SQLException {
        return new Asignatura(
                fila.getString("codigo"),
                fila.getString("carrera"),
                fila.getString("malla"),
                fila.getString("nombre"),
                fila.getInt("ciclo"),
                UnidadDeOrganizacionCurricular.valueOf(
                        fila.getString("unidad_organizacion_curricular")),
                fila.getBigDecimal("horas_semanales_clase"),
                fila.getBigDecimal("horas_practicas_semanales"),
                fila.getBigDecimal("creditos"),
                fila.getBoolean("tronco_comun"),
                new CampoConocimiento(
                        fila.getString("campo_amplio"), fila.getString("campo_especifico")),
                List.of(textos(fila.getArray("palabras_clave"))),
                fila.getBoolean("activa"));
    }

    private static String[] textos(Array arreglo) throws SQLException {
        return (String[]) arreglo.getArray();
    }
}
