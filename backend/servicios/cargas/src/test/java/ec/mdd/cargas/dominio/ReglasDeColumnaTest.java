package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static ec.mdd.cargas.dominio.DiccionarioReal.reglas;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Las reglas de cada columna, interpretadas del diccionario de datos real (FR-012, principio XIV):
 * cada patrón con un caso que pasa, uno que no y uno en el límite.
 */
class ReglasDeColumnaTest {

    @ParameterizedTest(name = "{0}.{1} = «{2}» → {3}")
    @CsvSource(
            delimiter = '¦',
            textBlock =
                    """
                    docentes ¦ primer_apellido ¦ PÉREZ ¦ true
                    docentes ¦ primer_apellido ¦ Pérez ¦ false
                    docentes ¦ primer_apellido ¦ PÉREZ ñ ¦ false
                    docentes ¦ segundo_nombre ¦ ANDRÉS ¦ true
                    docentes ¦ segundo_nombre ¦ andrés ¦ false
                    carreras ¦ nombre ¦ TECNOLOGÍA SUPERIOR ¦ true
                    asignaturas_malla ¦ nombre ¦ Programación ¦ false
                    estructura_organica ¦ codigo_unidad ¦ UA-01_X ¦ true
                    estructura_organica ¦ codigo_unidad ¦ ua-01 ¦ false
                    estructura_organica ¦ codigo_unidad ¦ UA 01 ¦ false
                    ciclos_academicos ¦ codigo_periodo ¦ 2026-I ¦ true
                    ciclos_academicos ¦ codigo_periodo ¦ 2026-II ¦ true
                    ciclos_academicos ¦ codigo_periodo ¦ 2026-III ¦ false
                    ciclos_academicos ¦ codigo_periodo ¦ 26-I ¦ false
                    docentes ¦ correo_institucional ¦ a@b.ec ¦ true
                    docentes ¦ correo_institucional ¦ a@b ¦ false
                    docentes ¦ correo_institucional ¦ a b@c.ec ¦ false
                    carreras ¦ siglas ¦ ABCDEFGHIJ ¦ true
                    carreras ¦ siglas ¦ ABCDEFGHIJK ¦ false
                    carreras ¦ numero_ciclos ¦ 12 ¦ false
                    carreras ¦ numero_ciclos ¦ 1a ¦ false
                    carreras ¦ numero_ciclos ¦ 0 ¦ false
                    carreras ¦ numero_ciclos ¦ 1 ¦ true
                    carreras ¦ numero_ciclos ¦ 10 ¦ true
                    carreras ¦ numero_ciclos ¦ 11 ¦ false
                    carreras ¦ numero_ciclos ¦ -1 ¦ false
                    asignaturas_malla ¦ horas_semanales_clase ¦ 2.5 ¦ true
                    asignaturas_malla ¦ horas_semanales_clase ¦ 2,5 ¦ false
                    asignaturas_malla ¦ horas_semanales_clase ¦ 0.4 ¦ false
                    asignaturas_malla ¦ horas_semanales_clase ¦ 0.5 ¦ true
                    asignaturas_malla ¦ horas_semanales_clase ¦ 20 ¦ true
                    asignaturas_malla ¦ horas_semanales_clase ¦ 20.5 ¦ false
                    asignaturas_malla ¦ horas_practicas_semanales ¦ 0 ¦ true
                    asignaturas_malla ¦ creditos ¦ 1.25 ¦ true
                    asignaturas_malla ¦ creditos ¦ 1,25 ¦ false
                    asignaturas_malla ¦ creditos ¦ 12.25 ¦ false
                    oferta_periodo ¦ numero_estudiantes ¦ 0 ¦ true
                    oferta_periodo ¦ numero_estudiantes ¦ -1 ¦ false
                    proyectos_practicas ¦ horas_semanales_aprobadas ¦ 0 ¦ false
                    proyectos_practicas ¦ horas_semanales_aprobadas ¦ 0.1 ¦ true
                    politica_carga_horaria ¦ dedicacion_total ¦ 40 ¦ true
                    politica_carga_horaria ¦ dedicacion_total ¦ 41 ¦ false
                    ciclos_academicos ¦ fecha_inicio ¦ 2026-02-28 ¦ true
                    ciclos_academicos ¦ fecha_inicio ¦ 2028-02-29 ¦ true
                    ciclos_academicos ¦ fecha_inicio ¦ 2026-02-30 ¦ false
                    ciclos_academicos ¦ fecha_inicio ¦ 28/02/2026 ¦ false
                    ciclos_academicos ¦ fecha_inicio ¦ 2026-2-28 ¦ false
                    estructura_organica ¦ es_comision_permanente ¦ S ¦ true
                    estructura_organica ¦ es_comision_permanente ¦ N ¦ true
                    estructura_organica ¦ es_comision_permanente ¦ s ¦ false
                    estructura_organica ¦ es_comision_permanente ¦ X ¦ false
                    estructura_organica ¦ tipo_unidad ¦ GESTION ¦ true
                    estructura_organica ¦ tipo_unidad ¦ Gestion ¦ false
                    estructura_organica ¦ codigo_gestion_autorizada ¦ G21 ¦ true
                    estructura_organica ¦ codigo_gestion_autorizada ¦ G22 ¦ false
                    cargos_autoridades ¦ codigo_cargo ¦ G21_GESTOR_ESTADISTICAS ¦ true
                    cargos_autoridades ¦ codigo_cargo ¦ GESTOR ¦ false
                    docentes ¦ categoria_escalafon ¦ AGREGADO_2 ¦ true
                    docentes ¦ categoria_escalafon ¦ PRINCIPAL_3 ¦ true
                    docentes ¦ categoria_escalafon ¦ AGREGADO_4 ¦ false
                    docentes ¦ categoria_escalafon ¦ AGREGADO_1..3 ¦ false
                    oferta_periodo ¦ modalidad ¦ HIBRIDA ¦ true
                    oferta_periodo ¦ modalidad ¦ VIRTUAL ¦ false
                    carreras ¦ campo_especifico ¦ 061 ¦ true
                    carreras ¦ campo_especifico ¦ 61 ¦ false
                    carreras ¦ campo_especifico ¦ 0611 ¦ false
                    carreras ¦ campo_detallado ¦ 0611 ¦ true
                    carreras ¦ jornadas ¦ MATUTINA; NOCTURNA ¦ true
                    carreras ¦ jornadas ¦ MATUTINA;NOCTURNA ¦ true
                    carreras ¦ jornadas ¦ MATUTINA; NOCHE ¦ false
                    oferta_periodo ¦ franjas_horarias ¦ LUN|18:00|20:00; SAB|07:00|09:30 ¦ true
                    oferta_periodo ¦ franjas_horarias ¦ DOM|18:00|20:00 ¦ false
                    oferta_periodo ¦ franjas_horarias ¦ LUN|25:00|20:00 ¦ false
                    oferta_periodo ¦ franjas_horarias ¦ LUN|18:00 ¦ false
                    docentes ¦ titulos_cuarto_nivel ¦ MAGÍSTER EN EDUCACIÓN|MAESTRIA|011|2019 ¦ true
                    docentes ¦ titulos_cuarto_nivel ¦ MAGÍSTER EN EDUCACIÓN|MAESTRIA|011 ¦ false
                    docentes ¦ titulos_cuarto_nivel ¦ MAGÍSTER EN EDUCACIÓN|MAGISTER|011|2019 ¦ false
                    docentes ¦ experiencia_laboral ¦ DOCENTE|ISTX|011|2.5 ¦ true
                    docentes ¦ experiencia_laboral ¦ DOCENTE|ISTX|011|2,5 ¦ false
                    proyectos_practicas ¦ participantes ¦ 0102030405|DIRECTOR|4 ¦ true
                    proyectos_practicas ¦ participantes ¦ 0102030405|JEFE|4 ¦ false
                    proyectos_practicas ¦ participantes ¦ 0102030405|DIRECTOR ¦ false
                    """)
    void cadaReglaDelDiccionario_pasaONoPasa(
            String carga, String campo, String valor, boolean pasa) {
        assertThat(reglas(carga, campo).revisar(valor).isEmpty())
                .as(() -> reglas(carga, campo).revisar(valor).orElse("pasa"))
                .isEqualTo(pasa);
    }

    @Test
    void dadoUnValorFueraDelCatalogo_entoncesElMotivoDiceLosValoresPermitidos() {
        assertThat(reglas("docentes", "dedicacion").revisar("XX"))
                .contains("Use uno de los valores permitidos: TC, MT, TP.");
        assertThat(reglas("cargos_autoridades", "codigo_cargo").revisar("GESTOR").orElseThrow())
                .startsWith("Use uno de los valores permitidos: RECTOR, VICERRECTOR,")
                .endsWith("y 12 más (vea el diccionario de la carga).");
    }

    @Test
    void cadaMotivoDiceQueHacerYNoRepiteElValor() {
        assertThat(reglas("carreras", "siglas").revisar("ABCDEFGHIJK"))
                .contains("Tiene 11 caracteres; el máximo es 10.");
        assertThat(reglas("ciclos_academicos", "fecha_inicio").revisar("2026-02-30"))
                .contains("Escriba una fecha que exista, con el formato aaaa-mm-dd.");
        assertThat(reglas("carreras", "numero_ciclos").revisar("11"))
                .contains("Debe estar entre 1 y 10.");
        assertThat(reglas("docentes", "primer_apellido").revisar("Pérez"))
                .contains("Escríbalo en mayúsculas, como pide el diccionario.");
        assertThat(reglas("proyectos_practicas", "participantes").revisar("0102030405|JEFE|4"))
                .contains(
                        "El elemento 1: «rol» debe ser uno de DIRECTOR, INVESTIGADOR,"
                                + " DOCENTE_APOYO, TUTOR_PRACTICAS.");
    }

    @Test
    void losFormatosQueNoSeInterpretanSonExactamenteEstos() {
        Set<String> sinInterpretar = new TreeSet<>();
        for (Carga carga : DICCIONARIO.cargas()) {
            for (ColumnaDeCarga columna : carga.columnas()) {
                if (!DICCIONARIO.validador(carga).reglas(columna.campo()).formatoInterpretado()) {
                    sinInterpretar.add(columna.formato());
                }
            }
        }

        assertThat(sinInterpretar)
                .containsExactly(
                        "Como consta en SENESCYT",
                        "Ej.: 550613A01-P-0301",
                        "Ej.: A_NOCTURNA",
                        "Ej.: DS-P-II-POO-204",
                        "Ej.: SENESCYT-2024-03",
                        "Ej.: SIGA, Secretaría",
                        "Ej.: programación; objetos; java",
                        "TC = 40, MT = 20, TP ≤ 19");
    }
}
