package ec.mdd.docentes.aplicacion.cargas;

import ec.mdd.docentes.dominio.CampoConocimiento;
import ec.mdd.docentes.dominio.CategoriaDeEscalafon;
import ec.mdd.docentes.dominio.CondicionEspecial;
import ec.mdd.docentes.dominio.Curso;
import ec.mdd.docentes.dominio.DatoInvalido;
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
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La carga 06, los docentes contratados con su ficha académica (F01-H04): sus 26 columnas,
 * traducidas a los docentes de Docentes (FR-012). Todas se guardan, así que todas cuentan como
 * cambio; el estado laboral se aplica con {@code cambiarEstado}. Registra y corrige con {@link
 * MantenerDocentes}, así que se aplican sus reglas, como el correo único o las horas de su
 * dedicación. El tipo de identificación no cambia.
 */
final class TraductorDocentesContratados extends TraductorDeDocentes<Docente> {

    /** Las columnas que se comparan, en el orden de la plantilla, sin la clave. */
    static final List<String> CAMPOS =
            List.of(
                    "tipo_identificacion",
                    "primer_apellido",
                    "segundo_apellido",
                    "primer_nombre",
                    "segundo_nombre",
                    "correo_institucional",
                    "telefono",
                    "tipo_personal",
                    "categoria_escalafon",
                    "dedicacion",
                    "horas_contratadas",
                    "fecha_ingreso",
                    "inicio_contrato",
                    "fin_contrato",
                    "condicion_especial",
                    "codigo_carrera_base",
                    "titulo_tercer_nivel",
                    "registro_senescyt_3n",
                    "campo_amplio_3n",
                    "campo_especifico_3n",
                    "titulos_cuarto_nivel",
                    "cursos_capacitaciones",
                    "experiencia_laboral",
                    "asignaturas_afines_declaradas",
                    "estado");

    private final RepositorioDocentes repositorio;
    private final MantenerDocentes mantener;

    TraductorDocentesContratados(RepositorioDocentes repositorio, MantenerDocentes mantener) {
        this.repositorio = repositorio;
        this.mantener = mantener;
    }

    @Override
    public String carga() {
        return "docentes";
    }

    @Override
    public boolean tieneDatos() {
        return repositorio.hayContratados();
    }

    @Override
    public void bloquear() {
        repositorio.bloquear();
    }

    @Override
    public Map<String, Docente> existentes(LoteDeCarga lote) {
        Map<String, Docente> existentes = new LinkedHashMap<>();
        repositorio
                .todos()
                .forEach(docente -> existentes.put(docente.identificacion().numero(), docente));
        return existentes;
    }

    @Override
    public String clave(FilaDelLote fila) {
        return fila.valor("numero_identificacion");
    }

    @Override
    public Map<String, String> campos(Docente docente) {
        DatosDelDocente datos = docente.datos();
        FichaAcademica ficha = datos.ficha();
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("tipo_identificacion", docente.identificacion().tipo().name());
        campos.put("primer_apellido", datos.primerApellido());
        campos.put("segundo_apellido", texto(datos.segundoApellido()));
        campos.put("primer_nombre", datos.primerNombre());
        campos.put("segundo_nombre", texto(datos.segundoNombre()));
        campos.put("correo_institucional", datos.correo());
        campos.put("telefono", texto(datos.telefono()));
        campos.put("tipo_personal", datos.tipoPersonal().name());
        campos.put("categoria_escalafon", texto(datos.categoria()));
        campos.put("dedicacion", datos.dedicacion().name());
        campos.put("horas_contratadas", String.valueOf(datos.horasContratadas()));
        campos.put("fecha_ingreso", datos.fechaIngreso().toString());
        campos.put("inicio_contrato", datos.inicioContrato().toString());
        campos.put("fin_contrato", texto(datos.finContrato()));
        campos.put("condicion_especial", datos.condicionEspecial().name());
        campos.put("codigo_carrera_base", datos.carreraBase());
        campos.put("titulo_tercer_nivel", ficha.tercerNivel().titulo());
        campos.put("registro_senescyt_3n", texto(ficha.tercerNivel().registroSenescyt()));
        campos.put("campo_amplio_3n", ficha.tercerNivel().campo().amplio());
        campos.put("campo_especifico_3n", ficha.tercerNivel().campo().especifico());
        campos.put(
                "titulos_cuarto_nivel",
                lista(
                        ficha.cuartoNivel(),
                        t ->
                                t.titulo()
                                        + "|"
                                        + t.tipo().name()
                                        + "|"
                                        + t.campoEspecifico()
                                        + "|"
                                        + t.anio()));
        campos.put(
                "cursos_capacitaciones",
                lista(
                        ficha.cursos(),
                        c ->
                                c.nombre()
                                        + "|"
                                        + c.campoEspecifico()
                                        + "|"
                                        + c.horas()
                                        + "|"
                                        + c.anio()));
        campos.put(
                "experiencia_laboral",
                lista(
                        ficha.experiencia(),
                        e ->
                                e.cargo()
                                        + "|"
                                        + e.institucion()
                                        + "|"
                                        + e.campoEspecifico()
                                        + "|"
                                        + numero(e.anios())));
        campos.put("asignaturas_afines_declaradas", String.join("; ", ficha.asignaturasAfines()));
        campos.put("estado", docente.estado().name());
        return campos;
    }

    @Override
    public Map<String, String> campos(FilaDelLote fila) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (String campo : CAMPOS) {
            campos.put(campo, fila.valor(campo));
        }
        campos.put(
                "horas_contratadas",
                String.valueOf(Integer.parseInt(fila.valor("horas_contratadas"))));
        if (fila.valor("condicion_especial").isEmpty()) {
            campos.put("condicion_especial", CondicionEspecial.NINGUNA.name());
        }
        campos.put("titulos_cuarto_nivel", canonica(fila.valor("titulos_cuarto_nivel"), 3, -1));
        campos.put("cursos_capacitaciones", canonica(fila.valor("cursos_capacitaciones"), 2, 3));
        campos.put("experiencia_laboral", canonica(fila.valor("experiencia_laboral"), -1, -1, 3));
        campos.put(
                "asignaturas_afines_declaradas",
                String.join("; ", elementos(fila.valor("asignaturas_afines_declaradas"))));
        return campos;
    }

    @Override
    public void registrar(FilaDelLote fila, String quien) {
        mantener.registrar(
                new Identificacion(
                        TipoDeIdentificacion.valueOf(fila.valor("tipo_identificacion")),
                        clave(fila)),
                datos(fila),
                quien);
        EstadoLaboral estado = EstadoLaboral.valueOf(fila.valor("estado"));
        if (estado != EstadoLaboral.ACTIVO) {
            mantener.cambiarEstado(clave(fila), estado, quien);
        }
    }

    @Override
    public void corregir(FilaDelLote fila, Docente actual, String quien) {
        Map<String, String> ahora = campos(actual);
        Map<String, String> archivo = campos(fila);
        if (!ahora.get("tipo_identificacion").equals(archivo.get("tipo_identificacion"))) {
            throw new DatoInvalido(
                    "tipo_identificacion",
                    "El tipo de identificación de un docente no cambia: corrige el archivo.");
        }
        ahora.remove("estado");
        archivo.remove("estado");
        if (!ahora.equals(archivo)) {
            mantener.corregir(clave(fila), datos(fila), quien);
        }
        EstadoLaboral estado = EstadoLaboral.valueOf(fila.valor("estado"));
        if (actual.estado() != estado) {
            mantener.cambiarEstado(clave(fila), estado, quien);
        }
    }

    private static DatosDelDocente datos(FilaDelLote fila) {
        return new DatosDelDocente(
                fila.valor("primer_apellido"),
                nulo(fila.valor("segundo_apellido")),
                fila.valor("primer_nombre"),
                nulo(fila.valor("segundo_nombre")),
                fila.valor("correo_institucional"),
                nulo(fila.valor("telefono")),
                TipoPersonal.valueOf(fila.valor("tipo_personal")),
                fila.valor("categoria_escalafon").isEmpty()
                        ? null
                        : CategoriaDeEscalafon.valueOf(fila.valor("categoria_escalafon")),
                Dedicacion.valueOf(fila.valor("dedicacion")),
                Integer.parseInt(fila.valor("horas_contratadas")),
                LocalDate.parse(fila.valor("fecha_ingreso")),
                LocalDate.parse(fila.valor("inicio_contrato")),
                fecha(fila.valor("fin_contrato")),
                fila.valor("condicion_especial").isEmpty()
                        ? null
                        : CondicionEspecial.valueOf(fila.valor("condicion_especial")),
                fila.valor("codigo_carrera_base"),
                new FichaAcademica(
                        new TituloTercerNivel(
                                fila.valor("titulo_tercer_nivel"),
                                nulo(fila.valor("registro_senescyt_3n")),
                                new CampoConocimiento(
                                        fila.valor("campo_amplio_3n"),
                                        fila.valor("campo_especifico_3n"))),
                        elementos(fila.valor("titulos_cuarto_nivel")).stream()
                                .map(TraductorDocentesContratados::partes)
                                .map(
                                        p ->
                                                new TituloCuartoNivel(
                                                        p[0],
                                                        TipoDeTituloDeCuartoNivel.valueOf(p[1]),
                                                        p[2],
                                                        Integer.parseInt(p[3])))
                                .toList(),
                        elementos(fila.valor("cursos_capacitaciones")).stream()
                                .map(TraductorDocentesContratados::partes)
                                .map(
                                        p ->
                                                new Curso(
                                                        p[0],
                                                        p[1],
                                                        Integer.parseInt(p[2]),
                                                        Integer.parseInt(p[3])))
                                .toList(),
                        elementos(fila.valor("experiencia_laboral")).stream()
                                .map(TraductorDocentesContratados::partes)
                                .map(
                                        p ->
                                                new ExperienciaLaboral(
                                                        p[0], p[1], p[2], new BigDecimal(p[3])))
                                .toList(),
                        elementos(fila.valor("asignaturas_afines_declaradas"))));
    }

    /**
     * Una lista de elementos con partes, en su texto canónico: las partes sin espacios, y las
     * enteras y la decimal que se digan escritas sin ceros de más.
     */
    private static String canonica(String lista, int entera, int otraEntera, int... decimales) {
        return elementos(lista).stream()
                .map(TraductorDocentesContratados::partes)
                .map(
                        p -> {
                            String[] canonicas = p.clone();
                            for (int indice : new int[] {entera, otraEntera}) {
                                if (indice >= 0 && indice < canonicas.length) {
                                    canonicas[indice] =
                                            String.valueOf(Integer.parseInt(canonicas[indice]));
                                }
                            }
                            for (int indice : decimales) {
                                if (indice < canonicas.length) {
                                    canonicas[indice] = numero(canonicas[indice]);
                                }
                            }
                            return String.join("|", canonicas);
                        })
                .collect(Collectors.joining("; "));
    }

    private static <E> String lista(List<E> elementos, Function<E, String> texto) {
        return elementos.stream().map(texto).collect(Collectors.joining("; "));
    }
}
