package ec.mdd.docentes.adaptadores.salida;

import ec.mdd.docentes.dominio.CampoConocimiento;
import ec.mdd.docentes.dominio.CategoriaDeEscalafon;
import ec.mdd.docentes.dominio.CondicionEspecial;
import ec.mdd.docentes.dominio.Curso;
import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Dedicacion;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.ExperienciaLaboral;
import ec.mdd.docentes.dominio.FichaAcademica;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.dominio.TipoDeIdentificacion;
import ec.mdd.docentes.dominio.TipoDeTituloDeCuartoNivel;
import ec.mdd.docentes.dominio.TipoPersonal;
import ec.mdd.docentes.dominio.TituloCuartoNivel;
import ec.mdd.docentes.dominio.TituloTercerNivel;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los docentes en {@code docentes.docente}, con {@link JdbcClient}. El título de tercer nivel va en
 * columnas; las cuatro listas de la ficha, en {@code jsonb}, con los mismos nombres del contrato
 * (research §2). El dominio las valida antes de guardar y al leer.
 */
public final class RepositorioDocentesJdbc implements RepositorioDocentes {

    private static final String COLUMNAS =
            "identificacion, tipo_identificacion, primer_apellido, segundo_apellido,"
                    + " primer_nombre, segundo_nombre, correo_institucional, telefono,"
                    + " tipo_personal, categoria_escalafon, dedicacion, horas_contratadas,"
                    + " fecha_ingreso, inicio_contrato, fin_contrato, condicion_especial,"
                    + " carrera_base, titulo_tercer_nivel, registro_senescyt, campo_amplio,"
                    + " campo_especifico, titulos_cuarto_nivel, cursos, experiencia,"
                    + " asignaturas_afines, estado";

    private static final String TODOS =
            "SELECT "
                    + COLUMNAS
                    + " FROM docentes.docente"
                    + " ORDER BY primer_apellido, segundo_apellido NULLS FIRST, primer_nombre,"
                    + " segundo_nombre NULLS FIRST, identificacion";

    private static final String BUSCAR =
            "SELECT " + COLUMNAS + " FROM docentes.docente WHERE identificacion = :identificacion";

    private static final String EXISTE_CORREO =
            "SELECT count(*) FROM docentes.docente WHERE lower(correo_institucional) = lower(:correo)"
                    + " AND identificacion IS DISTINCT FROM :salvo";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR =
            "LOCK TABLE docentes.docente IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR =
            "INSERT INTO docentes.docente ("
                    + COLUMNAS
                    + ", registrado_en, registrado_por)"
                    + " VALUES (:identificacion, :tipo, :primerApellido, :segundoApellido,"
                    + " :primerNombre, :segundoNombre, :correo, :telefono, :tipoPersonal,"
                    + " :categoria, :dedicacion, :horas, :ingreso, :inicio, :fin, :condicion,"
                    + " :carrera, :titulo, :registro, :amplio, :especifico,"
                    + " CAST(:cuartoNivel AS jsonb), CAST(:cursos AS jsonb),"
                    + " CAST(:experiencia AS jsonb), CAST(:afines AS jsonb), :estado, :momento,"
                    + " :quien)";

    private static final String GUARDAR =
            "UPDATE docentes.docente SET primer_apellido = :primerApellido,"
                    + " segundo_apellido = :segundoApellido, primer_nombre = :primerNombre,"
                    + " segundo_nombre = :segundoNombre, correo_institucional = :correo,"
                    + " telefono = :telefono, tipo_personal = :tipoPersonal,"
                    + " categoria_escalafon = :categoria, dedicacion = :dedicacion,"
                    + " horas_contratadas = :horas, fecha_ingreso = :ingreso,"
                    + " inicio_contrato = :inicio, fin_contrato = :fin,"
                    + " condicion_especial = :condicion, carrera_base = :carrera,"
                    + " titulo_tercer_nivel = :titulo, registro_senescyt = :registro,"
                    + " campo_amplio = :amplio, campo_especifico = :especifico,"
                    + " titulos_cuarto_nivel = CAST(:cuartoNivel AS jsonb),"
                    + " cursos = CAST(:cursos AS jsonb), experiencia = CAST(:experiencia AS jsonb),"
                    + " asignaturas_afines = CAST(:afines AS jsonb), estado = :estado"
                    + " WHERE identificacion = :identificacion AND tipo_identificacion = :tipo";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioDocentesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Docente> todos() {
        return jdbc.sql(TODOS).query(RepositorioDocentesJdbc::docente).list();
    }

    @Override
    public boolean hayContratados() {
        return jdbc.sql(
                        "SELECT EXISTS (SELECT 1 FROM docentes.docente WHERE estado IN ('ACTIVO', 'LICENCIA'))")
                .query(Boolean.class)
                .single();
    }

    @Override
    public Optional<Docente> buscar(String identificacion) {
        return jdbc.sql(BUSCAR)
                .param("identificacion", identificacion)
                .query(RepositorioDocentesJdbc::docente)
                .optional();
    }

    @Override
    public boolean existeCorreo(String correo, String salvo) {
        return jdbc.sql(EXISTE_CORREO)
                        .param("correo", correo)
                        .param("salvo", salvo)
                        .query(Long.class)
                        .single()
                > 0;
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public void registrar(Docente docente, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR), docente)
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(Docente docente) {
        conDatos(jdbc.sql(GUARDAR), docente).update();
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sentencia, Docente docente) {
        DatosDelDocente datos = docente.datos();
        FichaAcademica ficha = datos.ficha();
        return sentencia
                .param("identificacion", docente.identificacion().numero())
                .param("tipo", docente.identificacion().tipo().name())
                .param("primerApellido", datos.primerApellido())
                .param("segundoApellido", datos.segundoApellido())
                .param("primerNombre", datos.primerNombre())
                .param("segundoNombre", datos.segundoNombre())
                .param("correo", datos.correo())
                .param("telefono", datos.telefono())
                .param("tipoPersonal", datos.tipoPersonal().name())
                .param("categoria", datos.categoria().name())
                .param("dedicacion", datos.dedicacion().name())
                .param("horas", datos.horasContratadas())
                .param("ingreso", Date.valueOf(datos.fechaIngreso()))
                .param("inicio", Date.valueOf(datos.inicioContrato()))
                .param(
                        "fin",
                        datos.finContrato() == null ? null : Date.valueOf(datos.finContrato()))
                .param("condicion", datos.condicionEspecial().name())
                .param("carrera", datos.carreraBase())
                .param("titulo", ficha.tercerNivel().titulo())
                .param("registro", ficha.tercerNivel().registroSenescyt())
                .param("amplio", ficha.tercerNivel().campo().amplio())
                .param("especifico", ficha.tercerNivel().campo().especifico())
                .param("cuartoNivel", json(ficha.cuartoNivel(), RepositorioDocentesJdbc::titulo))
                .param("cursos", json(ficha.cursos(), RepositorioDocentesJdbc::curso))
                .param(
                        "experiencia",
                        json(ficha.experiencia(), RepositorioDocentesJdbc::experiencia))
                .param("afines", JSON.writeValueAsString(ficha.asignaturasAfines()))
                .param("estado", docente.estado().name());
    }

    private static Docente docente(ResultSet fila, int numero) throws SQLException {
        FichaAcademica ficha =
                new FichaAcademica(
                        new TituloTercerNivel(
                                fila.getString("titulo_tercer_nivel"),
                                fila.getString("registro_senescyt"),
                                new CampoConocimiento(
                                        fila.getString("campo_amplio"),
                                        fila.getString("campo_especifico"))),
                        lista(
                                fila.getString("titulos_cuarto_nivel"),
                                nodo ->
                                        new TituloCuartoNivel(
                                                nodo.get("titulo").asString(),
                                                TipoDeTituloDeCuartoNivel.valueOf(
                                                        nodo.get("tipo").asString()),
                                                nodo.get("campoEspecifico").asString(),
                                                nodo.get("anio").asInt())),
                        lista(
                                fila.getString("cursos"),
                                nodo ->
                                        new Curso(
                                                nodo.get("nombre").asString(),
                                                nodo.get("campoEspecifico").asString(),
                                                nodo.get("horas").asInt(),
                                                nodo.get("anio").asInt())),
                        lista(
                                fila.getString("experiencia"),
                                nodo ->
                                        new ExperienciaLaboral(
                                                nodo.get("cargo").asString(),
                                                nodo.get("institucion").asString(),
                                                nodo.get("campoEspecifico").asString(),
                                                nodo.get("anios").decimalValue())),
                        lista(fila.getString("asignaturas_afines"), JsonNode::asString));
        DatosDelDocente datos =
                new DatosDelDocente(
                        fila.getString("primer_apellido"),
                        fila.getString("segundo_apellido"),
                        fila.getString("primer_nombre"),
                        fila.getString("segundo_nombre"),
                        fila.getString("correo_institucional"),
                        fila.getString("telefono"),
                        TipoPersonal.valueOf(fila.getString("tipo_personal")),
                        CategoriaDeEscalafon.valueOf(fila.getString("categoria_escalafon")),
                        Dedicacion.valueOf(fila.getString("dedicacion").strip()),
                        fila.getInt("horas_contratadas"),
                        fecha(fila.getDate("fecha_ingreso")),
                        fecha(fila.getDate("inicio_contrato")),
                        fecha(fila.getDate("fin_contrato")),
                        CondicionEspecial.valueOf(fila.getString("condicion_especial")),
                        fila.getString("carrera_base"),
                        ficha);
        return new Docente(
                new Identificacion(
                        TipoDeIdentificacion.valueOf(fila.getString("tipo_identificacion")),
                        fila.getString("identificacion")),
                datos,
                EstadoLaboral.valueOf(fila.getString("estado")));
    }

    private static LocalDate fecha(Date fecha) {
        return fecha == null ? null : fecha.toLocalDate();
    }

    private static <T> String json(List<T> elementos, Function<T, Map<String, Object>> comoMapa) {
        return JSON.writeValueAsString(elementos.stream().map(comoMapa).toList());
    }

    private static <T> List<T> lista(String json, Function<JsonNode, T> crear) {
        List<T> elementos = new ArrayList<>();
        JSON.readTree(json).forEach(nodo -> elementos.add(crear.apply(nodo)));
        return elementos;
    }

    private static Map<String, Object> titulo(TituloCuartoNivel titulo) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("titulo", titulo.titulo());
        mapa.put("tipo", titulo.tipo().name());
        mapa.put("campoEspecifico", titulo.campoEspecifico());
        mapa.put("anio", titulo.anio());
        return mapa;
    }

    private static Map<String, Object> curso(Curso curso) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", curso.nombre());
        mapa.put("campoEspecifico", curso.campoEspecifico());
        mapa.put("horas", curso.horas());
        mapa.put("anio", curso.anio());
        return mapa;
    }

    private static Map<String, Object> experiencia(ExperienciaLaboral experiencia) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("cargo", experiencia.cargo());
        mapa.put("institucion", experiencia.institucion());
        mapa.put("campoEspecifico", experiencia.campoEspecifico());
        mapa.put("anios", experiencia.anios());
        return mapa;
    }
}
