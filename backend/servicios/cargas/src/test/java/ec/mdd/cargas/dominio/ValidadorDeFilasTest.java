package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static ec.mdd.cargas.dominio.DiccionarioReal.carga;
import static ec.mdd.cargas.dominio.DiccionarioReal.referenciasDePrueba;
import static ec.mdd.cargas.dominio.DiccionarioReal.tabla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las reglas de cada fila con el diccionario real y los 10 archivos de prueba, sin dobles (RF-08,
 * RF-09, FR-001 a FR-006).
 */
class ValidadorDeFilasTest {

    private static final String DOCENTES = "06_docentes.csv";
    private static final ReferenciasExistentes REFERENCIAS = referenciasDePrueba();

    @Test
    void dadaUnaCedulaSinElCeroInicial_entoncesSeMarcaYSeSugiereConElCero() {
        InformeValidacionCarga informe =
                validar(
                        "docentes",
                        cambiar(tabla(DOCENTES), 0, "numero_identificacion", "109990010"));

        assertThat(informe.problemas())
                .containsExactly(
                        new ProblemaDeFila(
                                2,
                                "numero_identificacion",
                                "109990010",
                                "Cédula inválida o sin cero inicial: le falta el 0 inicial.",
                                TipoDeProblema.ERROR,
                                Optional.of("0109990010")));
        assertThat(informe.valido()).isFalse();
    }

    @Test
    void dadaUnaCedulaConElDigitoVerificadorMal_entoncesNoHaySugerencia() {
        InformeValidacionCarga informe =
                validar(
                        "docentes",
                        cambiar(tabla(DOCENTES), 0, "numero_identificacion", "0109990011"));

        assertThat(informe.problemas())
                .singleElement()
                .satisfies(
                        problema -> {
                            assertThat(problema.motivo())
                                    .isEqualTo(
                                            "Cédula inválida o sin cero inicial: debe tener 10"
                                                    + " dígitos y un dígito verificador válido.");
                            assertThat(problema.sugerencia()).isEmpty();
                        });
    }

    @Test
    void dadoUnPasaporte_entoncesSonLetrasMayusculasODigitosHasta13() {
        TablaDeDatos pasaporte = cambiar(tabla(DOCENTES), 0, "tipo_identificacion", "PASAPORTE");

        assertThat(
                        validar(
                                        "docentes",
                                        cambiar(pasaporte, 0, "numero_identificacion", "AB123456"))
                                .problemas())
                .isEmpty();
        assertThat(
                        validar("docentes", cambiar(pasaporte, 0, "numero_identificacion", "ab123"))
                                .problemas())
                .extracting(ProblemaDeFila::motivo)
                .containsExactly("Un pasaporte tiene de 1 a 13 letras mayúsculas o dígitos.");
        assertThat(
                        validar(
                                        "docentes",
                                        cambiar(
                                                pasaporte,
                                                0,
                                                "numero_identificacion",
                                                "AB123456789012"))
                                .problemas())
                .extracting(ProblemaDeFila::motivo)
                .containsExactly("Tiene 14 caracteres; el máximo es 13.");
    }

    @Test
    void dadaUnaFilaConTresProblemas_entoncesCadaUnoEnSuFilaYSuCampo() {
        TablaDeDatos tabla = tabla(DOCENTES);
        tabla = cambiar(tabla, 3, "fecha_ingreso", "2026-02-30");
        tabla = cambiar(tabla, 3, "dedicacion", "XX");
        tabla = cambiar(tabla, 3, "primer_nombre", "A".repeat(61));

        assertThat(validar("docentes", tabla).problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::campo, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(5, "primer_nombre", "Tiene 61 caracteres; el máximo es 60."),
                        tuple(5, "dedicacion", "Usa uno de los valores permitidos: TC, MT, TP."),
                        tuple(
                                5,
                                "fecha_ingreso",
                                "Escribe una fecha que exista, con el formato aaaa-mm-dd."));
    }

    @Test
    void dadosObligatoriosVacios_entoncesSeInformanConSuCondicion() {
        TablaDeDatos tabla = tabla(DOCENTES);
        tabla = cambiar(tabla, 0, "primer_apellido", "");
        tabla = cambiar(tabla, 0, "categoria_escalafon", "");
        tabla = cambiar(tabla, 1, "fin_contrato", "");
        tabla = cambiar(tabla, 1, "categoria_escalafon", "");

        assertThat(validar("docentes", tabla).problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::campo, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(2, "primer_apellido", "Es obligatorio: complétalo."),
                        tuple(
                                2,
                                "categoria_escalafon",
                                "Es obligatorio cuando tipo_personal es TITULAR: complétalo."),
                        tuple(
                                3,
                                "fin_contrato",
                                "Es obligatorio cuando tipo_personal es OCASIONAL: complétalo."));
    }

    @Test
    void dadaUnaClaveSimpleRepetida_entoncesLaSegundaDiceLaFilaDeLaPrimera() {
        TablaDeDatos tabla = tabla(DOCENTES);
        List<List<String>> filas = new ArrayList<>(tabla.filas());
        filas.add(filas.get(0));

        assertThat(validar("docentes", new TablaDeDatos(tabla.encabezados(), filas)).problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::campo, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(
                                49,
                                "numero_identificacion",
                                "Esta clave ya está en la fila 2: no la repitas."));
    }

    @Test
    void dadaUnaClaveCompuestaRepetida_entoncesLaSegundaDiceLaFilaDeLaPrimera() {
        TablaDeDatos tabla = tabla("05_oferta_periodo.csv");
        List<List<String>> filas = new ArrayList<>(tabla.filas());
        filas.add(filas.get(1));

        assertThat(
                        validar("oferta_periodo", new TablaDeDatos(tabla.encabezados(), filas))
                                .problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::campo, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(
                                167,
                                "paralelo",
                                "Esta combinación de codigo_periodo, codigo_asignatura y paralelo"
                                        + " ya está en la fila 3: no la repitas."));
    }

    @Test
    void dadosEspaciosEnLosExtremos_entoncesAdvierteYRevisaElValorRecortado() {
        InformeValidacionCarga informe =
                validar("docentes", cambiar(tabla(DOCENTES), 0, "dedicacion", " TC "));

        assertThat(informe.problemas())
                .containsExactly(
                        ProblemaDeFila.advertencia(
                                2,
                                "dedicacion",
                                "TC",
                                "Tiene espacios en los extremos: se quitan."));
        assertThat(informe.valido()).isTrue();
        assertThat(informe.advertencias()).isOne();
    }

    @Test
    void dadaUnaFilaVaciaEntreRegistros_entoncesAdvierteYNoCuenta() {
        TablaDeDatos tabla = tabla(DOCENTES);
        List<List<String>> filas = new ArrayList<>(tabla.filas());
        filas.add(1, Collections.nCopies(tabla.encabezados().size(), ""));
        filas.add(Collections.nCopies(tabla.encabezados().size(), " "));

        InformeValidacionCarga informe =
                validar("docentes", new TablaDeDatos(tabla.encabezados(), filas));

        assertThat(informe.problemas())
                .containsExactly(ProblemaDeFila.advertencia(3, "", "", "Fila vacía: se ignora."));
        assertThat(informe.registros()).isEqualTo(47);
        assertThat(informe.valido()).isTrue();
    }

    @Test
    void dados150Problemas_entoncesLosTotalesYLosPrimeros100() {
        TablaDeDatos tabla = tabla("10_matriculados_carrera.csv");
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            List<String> fila = new ArrayList<>(tabla.filas().getFirst());
            fila.set(tabla.encabezados().indexOf("total_matriculados"), "x" + i);
            filas.add(fila);
        }

        InformeValidacionCarga informe =
                validar("matriculados_carrera", new TablaDeDatos(tabla.encabezados(), filas));

        assertThat(informe.errores()).isEqualTo(150);
        assertThat(informe.advertencias()).isZero();
        assertThat(informe.primeros(100))
                .hasSize(100)
                .first()
                .extracting(ProblemaDeFila::fila)
                .isEqualTo(2);
        assertThat(informe.primeros(100).getLast().fila()).isEqualTo(101);
        assertThat(informe.valido()).isFalse();
    }

    @Test
    void ningunMotivoRepiteElValorDeSuCelda() {
        TablaDeDatos tabla = tabla(DOCENTES);
        tabla = cambiar(tabla, 0, "numero_identificacion", "109990010");
        tabla = cambiar(tabla, 1, "correo_institucional", "sin-arroba");
        tabla = cambiar(tabla, 2, "dedicacion", "QQ");
        tabla = cambiar(tabla, 3, "codigo_carrera_base", "CARRERA-FANTASMA");
        tabla = cambiar(tabla, 4, "primer_apellido", "Minúsculas");

        List<ProblemaDeFila> problemas = validar("docentes", tabla).problemas();

        assertThat(problemas).hasSize(5);
        assertThat(problemas).allSatisfy(p -> assertThat(p.motivo()).doesNotContain(p.valor()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "01_estructura_organica.csv",
                "02_carreras.csv",
                "03_ciclos_academicos.csv",
                "04_asignaturas_malla.csv",
                "05_oferta_periodo.csv",
                "06_docentes.csv",
                "07_politica_carga_horaria.csv",
                "08_cargos_autoridades.csv",
                "09_proyectos_practicas.csv",
                "10_matriculados_carrera.csv"
            })
    void losDiezArchivosDePruebaNoTienenErrores(String archivo) {
        String codigo = archivo.substring(3, archivo.length() - ".csv".length());

        InformeValidacionCarga informe = validar(codigo, tabla(archivo));

        assertThat(informe.problemas()).as(informe.problemas().toString()).isEmpty();
        assertThat(informe.valido()).isTrue();
    }

    @Test
    void dadaUnaAsignaturaConUnaCarreraQueNoExiste_entoncesElErrorEnSuFila() {
        InformeValidacionCarga informe =
                validar(
                        "asignaturas_malla",
                        cambiar(
                                tabla("04_asignaturas_malla.csv"),
                                9,
                                "codigo_carrera",
                                "NO-EXISTE"));

        assertThat(informe.problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::campo, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(
                                11,
                                "codigo_carrera",
                                "No existe la carrera en el sistema: regístrala o confirma antes su carga."));
    }

    @Test
    void dadasReferenciasQueNoExisten_entoncesCadaUnaConLoQueFalta() {
        assertThat(motivos("oferta_periodo", "05_oferta_periodo.csv", "codigo_periodo", "2099-I"))
                .containsExactly(
                        "No existe el periodo en el sistema: regístralo o confirma antes su carga.");
        assertThat(motivos("carreras", "02_carreras.csv", "codigo_unidad_sede", "SEDE-X"))
                .containsExactly(
                        "No existe la unidad en el sistema: regístrala o confirma antes su carga.");
        assertThat(
                        motivos(
                                "cargos_autoridades",
                                "08_cargos_autoridades.csv",
                                "numero_identificacion",
                                "0109999995"))
                .containsExactly(
                        "No existe el docente en el sistema: regístralo o confirma antes su carga.");
    }

    @Test
    void dadaUnaListaConUnElementoQueNoExiste_entoncesElMotivoDiceCual() {
        assertThat(
                        motivos(
                                "proyectos_practicas",
                                "09_proyectos_practicas.csv",
                                "codigos_carrera",
                                "550613A01-P-0301; NO-EXISTE"))
                .containsExactly(
                        "El elemento 2 no existe en el sistema como carrera: regístralo o"
                                + " confirma antes su carga.");
        assertThat(
                        motivos(
                                "proyectos_practicas",
                                "09_proyectos_practicas.csv",
                                "participantes",
                                "0109990200|DIRECTOR|14; 0109999995|INVESTIGADOR|10"))
                .containsExactly(
                        "El elemento 2 no existe en el sistema como docente: regístralo o"
                                + " confirma antes su carga.");
    }

    @Test
    void elPadreDeUnaUnidadPuedeEstarEnElMismoArchivo() {
        ReferenciasExistentes sinUnidades =
                new ReferenciasExistentes(
                        Set.of(),
                        REFERENCIAS.carreras(),
                        REFERENCIAS.periodos(),
                        REFERENCIAS.asignaturas(),
                        REFERENCIAS.docentes());
        TablaDeDatos unidades = tabla("01_estructura_organica.csv");
        ValidadorDeFilas validador = DICCIONARIO.validador(carga("estructura_organica"));

        assertThat(validador.validar(unidades, sinUnidades).problemas()).isEmpty();
        assertThat(
                        validador
                                .validar(
                                        cambiar(unidades, 1, "codigo_unidad_padre", "NINGUNA"),
                                        sinUnidades)
                                .problemas())
                .extracting(ProblemaDeFila::fila, ProblemaDeFila::motivo)
                .containsExactly(
                        tuple(
                                3,
                                "No existe la unidad ni en este archivo ni en el sistema: agrégala."));
    }

    private static List<String> motivos(String carga, String archivo, String campo, String valor) {
        return validar(carga, cambiar(tabla(archivo), 0, campo, valor)).problemas().stream()
                .map(ProblemaDeFila::motivo)
                .toList();
    }

    private static InformeValidacionCarga validar(String carga, TablaDeDatos tabla) {
        return DICCIONARIO.validador(carga(carga)).validar(tabla, REFERENCIAS);
    }

    /** La tabla con un valor cambiado en una fila (la 0 es la primera después del encabezado). */
    private static TablaDeDatos cambiar(TablaDeDatos tabla, int fila, String campo, String valor) {
        List<List<String>> filas = new ArrayList<>(tabla.filas());
        List<String> cambiada = new ArrayList<>(filas.get(fila));
        cambiada.set(tabla.encabezados().indexOf(campo), valor);
        filas.set(fila, cambiada);
        return new TablaDeDatos(tabla.encabezados(), filas);
    }
}
