package ec.mdd.academico.dominio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las carreras, mallas y asignaturas de las cargas 02 y 04 de los datos de prueba de la ERS
 * (sintéticos, RNF-SEG-06), leídas de los recursos de prueba, y fábricas para las pruebas.
 */
final class DatosMaestrosDePrueba {

    /** Los 11 campos amplios de la CINE. */
    static final List<String> CAMPOS_AMPLIOS =
            List.of("00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10");

    /** La sede matriz de los datos de prueba y una unidad que no es sede. */
    static final List<UnidadOrganica> UNIDADES =
            List.of(
                    UnidadOrganica.nueva(
                            "MATRIZ", "Sede matriz", TipoDeUnidad.SEDE_MATRIZ, null, null, null),
                    UnidadOrganica.nueva(
                            "RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null),
                    UnidadOrganica.nueva(
                                    "NORTE", "Sede norte", TipoDeUnidad.SEDE, "MATRIZ", null, null)
                            .conEstado(false));

    private DatosMaestrosDePrueba() {}

    /** Una carrera vigente válida, con su código y sus siglas. */
    static Carrera carrera(String codigo, String siglas) {
        return Carrera.nueva(
                codigo,
                "DESARROLLO DE SOFTWARE",
                siglas,
                "MATRIZ",
                NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                Modalidad.PRESENCIAL,
                List.of(Jornada.NOCTURNA),
                new CampoConocimiento("06", "061"),
                "0613",
                5);
    }

    /** Una asignatura activa válida, en esa carrera y esa malla. */
    static Asignatura asignatura(String codigo, String carrera, String malla, int ciclo) {
        return Asignatura.nueva(
                codigo,
                carrera,
                malla,
                "MATEMÁTICA DISCRETA",
                ciclo,
                UnidadDeOrganizacionCurricular.BASICA,
                new BigDecimal("3"),
                BigDecimal.ZERO,
                null,
                true,
                new CampoConocimiento("05", "054"),
                List.of("matemática", "lógica", "conjuntos"));
    }

    /** Los datos maestros vacíos, con la estructura y los campos amplios de prueba. */
    static DatosMaestros vacios() {
        return new DatosMaestros(List.of(), List.of(), List.of(), UNIDADES, CAMPOS_AMPLIOS);
    }

    /** Las 7 carreras de la carga 02 de prueba, en orden. */
    static List<Carrera> carreras() {
        List<Carrera> carreras = new ArrayList<>();
        for (Map<String, String> fila : filas("02_carreras.csv")) {
            carreras.add(
                    Carrera.nueva(
                            fila.get("codigo_carrera"),
                            fila.get("nombre"),
                            fila.get("siglas"),
                            fila.get("codigo_unidad_sede"),
                            NivelDeFormacion.valueOf(fila.get("nivel_formacion")),
                            Modalidad.valueOf(fila.get("modalidad")),
                            Arrays.stream(fila.get("jornadas").split(";"))
                                    .map(String::strip)
                                    .map(Jornada::valueOf)
                                    .toList(),
                            new CampoConocimiento(
                                    fila.get("campo_amplio"), fila.get("campo_especifico")),
                            vacioANulo(fila.get("campo_detallado")),
                            Integer.parseInt(fila.get("numero_ciclos"))));
        }
        return carreras;
    }

    /** Las 300 asignaturas de la carga 04 de prueba, en orden. */
    static List<Asignatura> asignaturas() {
        List<Asignatura> asignaturas = new ArrayList<>();
        for (Map<String, String> fila : filas("04_asignaturas_malla.csv")) {
            asignaturas.add(
                    Asignatura.nueva(
                            fila.get("codigo_asignatura"),
                            fila.get("codigo_carrera"),
                            fila.get("codigo_version_malla"),
                            fila.get("nombre"),
                            Integer.parseInt(fila.get("ciclo")),
                            UnidadDeOrganizacionCurricular.valueOf(
                                    fila.get("unidad_organizacion_curricular")),
                            new BigDecimal(fila.get("horas_semanales_clase")),
                            numero(fila.get("horas_practicas_semanales")),
                            numero(fila.get("creditos")),
                            "S".equals(fila.get("tronco_comun")),
                            new CampoConocimiento(
                                    fila.get("campo_amplio"), fila.get("campo_especifico")),
                            Arrays.stream(fila.get("palabras_clave").split(";")).toList()));
        }
        return asignaturas;
    }

    /** Las 7 carreras, 12 mallas y 300 asignaturas de prueba, registradas en orden. */
    static DatosMaestros cargados() {
        DatosMaestros datos = vacios();
        for (Carrera carrera : carreras()) {
            datos = datos.con(datos.registrarCarrera(carrera));
        }
        List<Asignatura> asignaturas = asignaturas();
        for (String malla : asignaturas.stream().map(Asignatura::malla).distinct().toList()) {
            String carrera =
                    asignaturas.stream()
                            .filter(una -> una.malla().equals(malla))
                            .findFirst()
                            .orElseThrow()
                            .carrera();
            datos = datos.con(datos.registrarMalla(Malla.nueva(malla, carrera)));
        }
        for (Asignatura asignatura : asignaturas) {
            datos = datos.con(datos.registrarAsignatura(asignatura));
        }
        return datos;
    }

    /** Las 165 asignaturas-paralelo de la carga 05 de prueba, en orden, en ese periodo. */
    static List<AsignaturaParalelo> oferta(String periodo) {
        List<AsignaturaParalelo> oferta = new ArrayList<>();
        for (Map<String, String> fila : filas("05_oferta_periodo.csv")) {
            oferta.add(
                    AsignaturaParalelo.nueva(
                            periodo,
                            fila.get("codigo_asignatura"),
                            fila.get("paralelo"),
                            Jornada.valueOf(fila.get("jornada")),
                            Modalidad.valueOf(fila.get("modalidad")),
                            vacioANulo(fila.get("codigo_unidad_sede")),
                            Integer.parseInt(fila.get("numero_estudiantes")),
                            numero(fila.get("horas_semanales"))));
        }
        return oferta;
    }

    private static BigDecimal numero(String texto) {
        return texto == null || texto.isBlank() ? null : new BigDecimal(texto);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }

    /** Las filas de un CSV de prueba como mapas; los valores no llevan comillas ni comas. */
    private static List<Map<String, String>> filas(String archivo) {
        try (InputStream entrada =
                        DatosMaestrosDePrueba.class.getResourceAsStream(
                                "/academico/datos-de-prueba/" + archivo);
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().filter(linea -> !linea.isBlank()).toList();
            String[] encabezados = lineas.getFirst().split(",", -1);
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
}
