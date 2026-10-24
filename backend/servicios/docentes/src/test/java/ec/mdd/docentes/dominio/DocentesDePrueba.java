package ec.mdd.docentes.dominio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Los datos de prueba de Docentes: un docente válido para cambiarle un campo, y los 47 docentes de
 * la carga 06 de prueba con las referencias de las cargas 02 y 04 (ficticios, RNF-SEG-06).
 */
public final class DocentesDePrueba {

    /** El año de los datos de prueba: ningún título ni curso pasa de él. */
    public static final Year ANIO = Year.of(2026);

    /** Una cédula ficticia con su dígito verificador correcto. */
    public static final String CEDULA = "0109990010";

    /** Los 11 campos amplios de la CINE, con un nombre de prueba. */
    public static final Map<String, String> CAMPOS_AMPLIOS =
            Map.ofEntries(
                    Map.entry("00", "Programas genéricos"),
                    Map.entry("01", "Educación"),
                    Map.entry("02", "Artes y humanidades"),
                    Map.entry("03", "Ciencias sociales, periodismo e información"),
                    Map.entry("04", "Administración de empresas y derecho"),
                    Map.entry("05", "Ciencias naturales, matemáticas y estadística"),
                    Map.entry("06", "Tecnologías de la información y la comunicación"),
                    Map.entry("07", "Ingeniería, industria y construcción"),
                    Map.entry("08", "Agricultura, silvicultura, pesca y veterinaria"),
                    Map.entry("09", "Salud y bienestar"),
                    Map.entry("10", "Servicios"));

    private DocentesDePrueba() {}

    /** Una ficha con un título de tercer nivel, una maestría, dos cursos y una experiencia. */
    public static FichaAcademica ficha() {
        return new FichaAcademica(
                new TituloTercerNivel(
                        "INGENIERO DE SISTEMAS", null, new CampoConocimiento("06", "061")),
                List.of(
                        new TituloCuartoNivel(
                                "MÁSTER EN TELEMÁTICA",
                                TipoDeTituloDeCuartoNivel.MAESTRIA,
                                "061",
                                2015)),
                List.of(
                        new Curso("PROGRAMACIÓN EN JAVA", "061", 40, 2023),
                        new Curso("DOCENCIA UNIVERSITARIA", "011", 60, 2021)),
                List.of(
                        new ExperienciaLaboral(
                                "DESARROLLADOR", "EMPRESA XYZ", "061", new BigDecimal("3"))),
                List.of("A1"));
    }

    /** Los datos de un docente ocasional a tiempo completo, válidos. */
    public static DatosDelDocente datos() {
        return con(datos -> datos);
    }

    /** Los datos válidos con un cambio aplicado sobre sus valores, como en un formulario. */
    public static DatosDelDocente con(Function<Valores, Valores> cambio) {
        return cambio.apply(new Valores()).datos();
    }

    /** Un docente activo con la cédula de prueba y los datos válidos. */
    public static Docente docente() {
        return Docente.nuevo(
                new Identificacion(TipoDeIdentificacion.CEDULA, CEDULA), datos(), ANIO);
    }

    /** Las referencias de las cargas 02 y 04 de prueba, más la carrera C1 y la asignatura A1. */
    public static Referencias referencias() {
        Map<String, Referencias.Carrera> carreras = new LinkedHashMap<>();
        carreras.put("C1", new Referencias.Carrera("DESARROLLO DE SOFTWARE", "DS"));
        filas("02_carreras.csv")
                .forEach(
                        fila ->
                                carreras.put(
                                        fila.get("codigo_carrera"),
                                        new Referencias.Carrera(
                                                fila.get("nombre"), fila.get("siglas"))));
        Map<String, Referencias.Asignatura> asignaturas = new LinkedHashMap<>();
        asignaturas.put("A1", new Referencias.Asignatura("PROGRAMACIÓN", "C1"));
        filas("04_asignaturas_malla.csv")
                .forEach(
                        fila ->
                                asignaturas.put(
                                        fila.get("codigo_asignatura"),
                                        new Referencias.Asignatura(
                                                fila.get("nombre"), fila.get("codigo_carrera"))));
        return new Referencias(carreras, asignaturas, CAMPOS_AMPLIOS);
    }

    /** Los 47 docentes de la carga 06 de prueba, en su orden. */
    public static List<Docente> cargados() {
        List<Docente> docentes = new ArrayList<>();
        for (Map<String, String> fila : filas("06_docentes.csv")) {
            DatosDelDocente datos =
                    new DatosDelDocente(
                            fila.get("primer_apellido"),
                            fila.get("segundo_apellido"),
                            fila.get("primer_nombre"),
                            fila.get("segundo_nombre"),
                            fila.get("correo_institucional"),
                            fila.get("telefono"),
                            TipoPersonal.valueOf(fila.get("tipo_personal")),
                            CategoriaDeEscalafon.valueOf(fila.get("categoria_escalafon")),
                            Dedicacion.valueOf(fila.get("dedicacion")),
                            Integer.parseInt(fila.get("horas_contratadas")),
                            LocalDate.parse(fila.get("fecha_ingreso")),
                            LocalDate.parse(fila.get("inicio_contrato")),
                            fecha(fila.get("fin_contrato")),
                            CondicionEspecial.valueOf(fila.get("condicion_especial")),
                            fila.get("codigo_carrera_base"),
                            new FichaAcademica(
                                    new TituloTercerNivel(
                                            fila.get("titulo_tercer_nivel"),
                                            fila.get("registro_senescyt_3n"),
                                            new CampoConocimiento(
                                                    fila.get("campo_amplio_3n"),
                                                    fila.get("campo_especifico_3n"))),
                                    elementos(
                                            fila.get("titulos_cuarto_nivel"),
                                            p ->
                                                    new TituloCuartoNivel(
                                                            p[0],
                                                            TipoDeTituloDeCuartoNivel.valueOf(p[1]),
                                                            p[2],
                                                            Integer.parseInt(p[3]))),
                                    elementos(
                                            fila.get("cursos_capacitaciones"),
                                            p ->
                                                    new Curso(
                                                            p[0],
                                                            p[1],
                                                            Integer.parseInt(p[2]),
                                                            Integer.parseInt(p[3]))),
                                    elementos(
                                            fila.get("experiencia_laboral"),
                                            p ->
                                                    new ExperienciaLaboral(
                                                            p[0],
                                                            p[1],
                                                            p[2],
                                                            new BigDecimal(p[3]))),
                                    elementos(
                                            fila.get("asignaturas_afines_declaradas"), p -> p[0])));
            Docente docente =
                    Docente.nuevo(
                            new Identificacion(
                                    TipoDeIdentificacion.valueOf(fila.get("tipo_identificacion")),
                                    fila.get("numero_identificacion")),
                            datos,
                            ANIO);
            docentes.add(
                    "ACTIVO".equals(fila.get("estado"))
                            ? docente
                            : docente.conEstado(EstadoLaboral.valueOf(fila.get("estado"))));
        }
        return docentes;
    }

    private static LocalDate fecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }

    // «a|b|c|d; e|f|g|h»: los elementos de una lista de la carga 6, con sus partes.
    private static <T> List<T> elementos(String lista, Function<String[], T> crear) {
        if (lista == null || lista.isBlank()) {
            return List.of();
        }
        return Arrays.stream(lista.split(";"))
                .map(String::strip)
                .filter(elemento -> !elemento.isEmpty())
                .map(elemento -> crear.apply(elemento.split("\\|")))
                .toList();
    }

    /** Las filas de un CSV de prueba como mapas; los valores no llevan comillas ni comas. */
    private static List<Map<String, String>> filas(String archivo) {
        try (InputStream entrada =
                        DocentesDePrueba.class.getResourceAsStream(
                                "/docentes/datos-de-prueba/" + archivo);
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().filter(linea -> !linea.isBlank()).toList();
            String[] encabezados = lineas.getFirst().replace("﻿", "").split(",", -1);
            List<Map<String, String>> filas = new ArrayList<>();
            for (String linea : lineas.subList(1, lineas.size())) {
                String[] valores = linea.split(",", -1);
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < encabezados.length; i++) {
                    fila.put(encabezados[i].strip(), valores[i].strip());
                }
                filas.add(fila);
            }
            return filas;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Los valores de un formulario de docente, para cambiar uno y armar los datos. */
    public static final class Valores {
        public String primerApellido = "PERALTA";
        public String segundoApellido = "MALDONADO";
        public String primerNombre = "JUAN";
        public String segundoNombre = "ANDRES";
        public String correo = "juan.peralta@instituto-prueba.edu.ec";
        public String telefono = "0990000001";
        public TipoPersonal tipoPersonal = TipoPersonal.OCASIONAL;
        public CategoriaDeEscalafon categoria = CategoriaDeEscalafon.NO_APLICA;
        public Dedicacion dedicacion = Dedicacion.TC;
        public int horas = 40;
        public LocalDate ingreso = LocalDate.of(2019, 10, 1);
        public LocalDate inicio = LocalDate.of(2026, 4, 1);
        public LocalDate fin = LocalDate.of(2026, 8, 31);
        public CondicionEspecial condicion = CondicionEspecial.NINGUNA;
        public String carrera = "C1";
        public FichaAcademica ficha = ficha();

        DatosDelDocente datos() {
            return new DatosDelDocente(
                    primerApellido,
                    segundoApellido,
                    primerNombre,
                    segundoNombre,
                    correo,
                    telefono,
                    tipoPersonal,
                    categoria,
                    dedicacion,
                    horas,
                    ingreso,
                    inicio,
                    fin,
                    condicion,
                    carrera,
                    ficha);
        }
    }
}
