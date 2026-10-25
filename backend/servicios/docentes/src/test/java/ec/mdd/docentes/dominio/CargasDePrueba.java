package ec.mdd.docentes.dominio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las cargas 01, 03, 08 y 09 de prueba (F03-H05): las unidades y los periodos como referencias de
 * Académico, y los 21 cargos y los 11 proyectos sobre los 47 docentes de {@link DocentesDePrueba}.
 * Los datos son ficticios (RNF-SEG-06).
 */
public final class CargasDePrueba {

    /** El día con el que se calculan las advertencias en las pruebas. */
    public static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    private CargasDePrueba() {}

    /** Las referencias de las cargas 01 a 04 de prueba, con las de {@link DocentesDePrueba}. */
    public static Referencias referencias() {
        Referencias base = DocentesDePrueba.referencias();
        Map<String, Referencias.Unidad> unidades = new LinkedHashMap<>();
        for (Map<String, String> fila : filas("01_estructura_organica.csv")) {
            unidades.put(
                    fila.get("codigo_unidad"),
                    new Referencias.Unidad(
                            fila.get("nombre"),
                            fila.get("tipo_unidad"),
                            vacioANulo(fila.get("codigo_gestion_autorizada")),
                            "ACTIVO".equals(fila.get("estado"))));
        }
        Map<String, Referencias.Periodo> periodos = new LinkedHashMap<>();
        for (Map<String, String> fila : filas("03_ciclos_academicos.csv")) {
            periodos.put(
                    fila.get("codigo_periodo"),
                    new Referencias.Periodo(
                            fila.get("nombre"),
                            LocalDate.parse(fila.get("fecha_inicio")),
                            LocalDate.parse(fila.get("fecha_fin"))));
        }
        return new Referencias(
                base.carreras(), base.asignaturas(), base.camposAmplios(), unidades, periodos);
    }

    /**
     * El docente de prueba ({@link DocentesDePrueba#CEDULA}), activo a tiempo completo, con su
     * maestría o sin títulos de cuarto nivel.
     */
    public static Docente docente(boolean conCuartoNivel) {
        return docente(conCuartoNivel, Dedicacion.TC);
    }

    /** El docente de prueba con esa dedicación, activo. */
    public static Docente docente(boolean conCuartoNivel, Dedicacion dedicacion) {
        return Docente.nuevo(
                new Identificacion(TipoDeIdentificacion.CEDULA, DocentesDePrueba.CEDULA),
                DocentesDePrueba.con(
                        valores -> {
                            valores.dedicacion = dedicacion;
                            valores.horas = dedicacion == Dedicacion.TC ? 40 : 20;
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
                        }),
                DocentesDePrueba.ANIO);
    }

    /** Los 47 docentes de la carga 06 de prueba, por identificación. */
    public static Map<String, Docente> docentes() {
        return DocentesDePrueba.cargados().stream()
                .collect(
                        Collectors.toMap(
                                docente -> docente.identificacion().numero(),
                                Function.identity(),
                                (uno, otro) -> uno,
                                LinkedHashMap::new));
    }

    /** Los 21 cargos de la carga 08 de prueba, en su orden. */
    public static List<Cargo> cargos() {
        return filas("08_cargos_autoridades.csv").stream()
                .map(
                        fila ->
                                new Cargo(
                                        fila.get("numero_identificacion"),
                                        TipoDeCargo.valueOf(fila.get("codigo_cargo")),
                                        LocalDate.parse(fila.get("fecha_designacion")),
                                        fila.get("codigo_unidad"),
                                        fila.get("codigo_carrera"),
                                        fecha(fila.get("fecha_fin")),
                                        fila.get("documento_designacion"),
                                        EstadoDeCargo.valueOf(fila.get("estado"))))
                .toList();
    }

    /** Los 11 proyectos de la carga 09 de prueba, en su orden. */
    public static List<Proyecto> proyectos() {
        return filas("09_proyectos_practicas.csv").stream()
                .map(
                        fila ->
                                new Proyecto(
                                        fila.get("codigo_proyecto"),
                                        fila.get("nombre"),
                                        TipoDeProyecto.valueOf(fila.get("tipo_proyecto")),
                                        lista(fila.get("codigos_carrera")),
                                        fila.get("resolucion_ocs"),
                                        LocalDate.parse(fila.get("fecha_resolucion")),
                                        EstadoDeProyecto.valueOf(fila.get("estado")),
                                        LocalDate.parse(fila.get("vigencia_inicio")),
                                        LocalDate.parse(fila.get("vigencia_fin")),
                                        fila.get("director_identificacion"),
                                        lista(fila.get("participantes")).stream()
                                                .map(CargasDePrueba::participante)
                                                .toList(),
                                        new BigDecimal(fila.get("horas_semanales_aprobadas"))))
                .toList();
    }

    private static Participante participante(String texto) {
        String[] partes = texto.split("\\|", -1);
        return new Participante(
                partes[0].strip(),
                RolDeParticipante.valueOf(partes[1].strip()),
                new BigDecimal(partes[2].strip()));
    }

    private static List<String> lista(String texto) {
        return Arrays.stream(texto.split(";"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static LocalDate fecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }

    // Un CSV con comillas: la carga 01 tiene nombres con comas («Gestión de Investigación,
    // Desarrollo Tecnológico e Innovación»).
    private static List<Map<String, String>> filas(String archivo) {
        try (InputStream entrada =
                        CargasDePrueba.class.getResourceAsStream(
                                "/docentes/datos-de-prueba/" + archivo);
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().filter(linea -> !linea.isBlank()).toList();
            List<String> encabezados = celdas(lineas.getFirst().replace("﻿", ""));
            List<Map<String, String>> filas = new ArrayList<>();
            for (String linea : lineas.subList(1, lineas.size())) {
                List<String> valores = celdas(linea);
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < encabezados.size(); i++) {
                    fila.put(encabezados.get(i).strip(), valores.get(i).strip());
                }
                filas.add(fila);
            }
            return filas;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> celdas(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == ',' && !entreComillas) {
                celdas.add(actual.toString());
                actual.setLength(0);
            } else if (c != '\r') {
                actual.append(c);
            }
        }
        celdas.add(actual.toString());
        return celdas;
    }
}
