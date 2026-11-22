package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Las reglas del archivo de mapeo (FR-015, research §6, D-119): todos los problemas juntos. */
class LayoutExportacionTest {

    private static final BorradorDeColumna IDENTIFICACION =
            columna("NUMERO_IDENTIFICACION", "identificacion");
    private static final BorradorDeColumna COMPONENTE = columna("COMPONENTE", "componente");
    private static final BorradorDeColumna HORAS = columna("HORAS_SEMANALES", "horas");

    @Test
    void dadoUnBorradorValido_cuandoSeValida_entoncesDaLasColumnasEnSuOrden() {
        ValidacionDelLayout validacion =
                LayoutExportacion.validar(
                        borrador(
                                HORAS,
                                columna("periodo", "periodo"),
                                IDENTIFICACION,
                                traducida(
                                        "COMPONENTE",
                                        "componente",
                                        Map.of(
                                                "CLASE", "01",
                                                "OTRAS_DOCENCIA", "02",
                                                "INVESTIGACION", "03",
                                                "VINCULACION", "04",
                                                "GESTION", "05"))));

        assertThat(validacion.problemas()).isEmpty();
        assertThat(validacion.columnas())
                .extracting(ColumnaDelLayout::nombre)
                .containsExactly(
                        "HORAS_SEMANALES", "periodo", "NUMERO_IDENTIFICACION", "COMPONENTE");
        assertThat(validacion.columnas().get(3).traduccion())
                .containsEntry("CLASE", "01")
                .containsKeys("CLASE", "OTRAS_DOCENCIA", "INVESTIGACION", "VINCULACION", "GESTION");
    }

    @Test
    void dadoUnLayoutValido_cuandoSeArma_entoncesTieneSuHashYSuEncabezado() {
        List<ColumnaDelLayout> columnas =
                LayoutExportacion.validar(borrador(IDENTIFICACION, COMPONENTE, HORAS)).columnas();
        Instant momento = Instant.parse("2026-11-22T15:00:00Z");

        LayoutExportacion porDefecto = LayoutExportacion.porDefecto("columnas: []", columnas);
        LayoutExportacion cargado =
                LayoutExportacion.cargado(3, "columnas: []", columnas, "secretaria.demo", momento);

        assertThat(porDefecto.numero()).isZero();
        assertThat(porDefecto.esPorDefecto()).isTrue();
        assertThat(porDefecto.cargadoPor()).isEmpty();
        assertThat(porDefecto.sha256()).matches("^[0-9a-f]{64}$");
        assertThat(cargado.sha256()).isEqualTo(porDefecto.sha256());
        assertThat(cargado.esPorDefecto()).isFalse();
        assertThat(cargado.cargadoPor()).contains("secretaria.demo");
        assertThat(cargado.cargadoEn()).contains(momento);
        assertThat(cargado.encabezado())
                .containsExactly("NUMERO_IDENTIFICACION", "COMPONENTE", "HORAS_SEMANALES");
        assertThatThrownBy(
                        () ->
                                LayoutExportacion.cargado(
                                        0, "x", columnas, "secretaria.demo", momento))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnArchivoSinColumnas_cuandoSeValida_entoncesFaltanColumnasYLasObligatorias() {
        ValidacionDelLayout validacion = LayoutExportacion.validar(borrador());

        assertThat(validacion.problemas())
                .extracting(ProblemaDelLayout::columna)
                .containsOnly("archivo");
        assertThat(reglas(validacion))
                .anyMatch(una -> una.contains("no tiene columnas"))
                .anyMatch(una -> una.contains("«identificacion»"))
                .anyMatch(una -> una.contains("«componente»"))
                .anyMatch(una -> una.contains("«horas»"));
    }

    @Test
    void dadas30Columnas_cuandoSeValida_entoncesVale_yCon31No() {
        List<BorradorDeColumna> treinta =
                new ArrayList<>(List.of(IDENTIFICACION, COMPONENTE, HORAS));
        for (int i = treinta.size(); i < 30; i++) {
            treinta.add(columna("EXTRA_" + i, "periodo"));
        }
        List<BorradorDeColumna> treintaYUna = new ArrayList<>(treinta);
        treintaYUna.add(columna("EXTRA_30", "periodo"));

        assertThat(LayoutExportacion.validar(new BorradorDeLayout(treinta, List.of())).problemas())
                .isEmpty();
        assertThat(reglas(LayoutExportacion.validar(new BorradorDeLayout(treintaYUna, List.of()))))
                .singleElement()
                .asString()
                .contains("31 columnas");
    }

    @Test
    void dadosNombresEnElLimite_cuandoSeValida_entonces64ValenY65No() {
        String sesentaYCuatro = "A" + "b".repeat(63);
        String sesentaYCinco = "A" + "b".repeat(64);

        assertThat(
                        LayoutExportacion.validar(
                                        borrador(
                                                IDENTIFICACION,
                                                COMPONENTE,
                                                HORAS,
                                                columna(sesentaYCuatro, "periodo")))
                                .problemas())
                .isEmpty();
        assertThat(
                        LayoutExportacion.validar(
                                        borrador(
                                                IDENTIFICACION,
                                                COMPONENTE,
                                                HORAS,
                                                columna(sesentaYCinco, "periodo")))
                                .problemas())
                .singleElement()
                .satisfies(
                        problema -> {
                            assertThat(problema.columna()).startsWith("columnas[4]");
                            assertThat(problema.regla()).contains("no es válido");
                        });
    }

    @Test
    void dadoUnArchivoConVariosProblemas_cuandoSeValida_entoncesLosDaTodosConSuColumna() {
        ValidacionDelLayout validacion =
                LayoutExportacion.validar(
                        borrador(
                                columna("PERIODO", "horas_semanales"),
                                columna("periodo", "periodo"),
                                columna(null, "docente"),
                                columna("1CODIGO", "asignatura"),
                                columna("CARRERA", null),
                                traducida("ASIGNATURA", "asignatura", Map.of("X", "1")),
                                IDENTIFICACION,
                                COMPONENTE,
                                HORAS));

        assertThat(validacion.problemas())
                .extracting(ProblemaDelLayout::columna)
                .containsExactly(
                        "columnas[1] (PERIODO)",
                        "columnas[2] (periodo)",
                        "columnas[3]",
                        "columnas[4] (1CODIGO)",
                        "columnas[5] (CARRERA)",
                        "columnas[6] (ASIGNATURA)");
        assertThat(reglas(validacion))
                .containsExactly(
                        "El campo «horas_semanales» no existe. Usa uno de la lista de la plantilla.",
                        "El nombre «periodo» ya está en columnas[1]. Cada columna necesita un"
                                + " nombre distinto.",
                        "Falta «nombre». Escribe el encabezado de la columna.",
                        "El nombre «1CODIGO» no es válido: empieza con una letra y usa solo"
                                + " letras sin tildes, dígitos o «_», hasta 64 caracteres.",
                        "Falta «campo». Usa uno de la lista de la plantilla.",
                        "El campo «asignatura» no admite traducción. Quita «traduccion».");
    }

    @Test
    void dadoQueFaltaCadaObligatoria_cuandoSeValida_entoncesLaNombra() {
        assertThat(reglas(LayoutExportacion.validar(borrador(COMPONENTE, HORAS))))
                .containsExactly("Falta una columna con el campo «identificacion».");
        assertThat(reglas(LayoutExportacion.validar(borrador(IDENTIFICACION, HORAS))))
                .containsExactly("Falta una columna con el campo «componente».");
        assertThat(reglas(LayoutExportacion.validar(borrador(IDENTIFICACION, COMPONENTE))))
                .containsExactly("Falta una columna con el campo «horas».");
    }

    @Test
    void dadaUnaTraduccionIncompletaOConUnCodigoDeMas_cuandoSeValida_entoncesLoDice() {
        assertThat(
                        reglas(
                                LayoutExportacion.validar(
                                        borrador(
                                                IDENTIFICACION,
                                                COMPONENTE,
                                                HORAS,
                                                traducida(
                                                        "DEDICACION",
                                                        "dedicacion",
                                                        Map.of("TC", "1", "MT", "2"))))))
                .containsExactly("Falta traducir: TP.");
        assertThat(
                        reglas(
                                LayoutExportacion.validar(
                                        borrador(
                                                IDENTIFICACION,
                                                COMPONENTE,
                                                HORAS,
                                                traducida(
                                                        "TIPO",
                                                        "tipo_identificacion",
                                                        orden(
                                                                "CEDULA", "C",
                                                                "PASAPORTE", "P",
                                                                "RUC", "R"))))))
                .containsExactly(
                        "«RUC» no es un código de «tipo_identificacion»: usa solo CEDULA,"
                                + " PASAPORTE.");
    }

    @Test
    void dadosValoresDeTraduccionInvalidosNAORepetidos_cuandoSeValida_entoncesLosDice() {
        ValidacionDelLayout validacion =
                LayoutExportacion.validar(
                        borrador(
                                IDENTIFICACION,
                                COMPONENTE,
                                HORAS,
                                traducida(
                                        "DEDICACION",
                                        "dedicacion",
                                        orden("TC", "_X", "MT", "na", "TP", "01")),
                                traducida(
                                        "TIPO",
                                        "tipo_identificacion",
                                        orden("CEDULA", "01", "PASAPORTE", "01"))));

        assertThat(reglas(validacion))
                .containsExactly(
                        "El valor «_X» de «TC» no es válido: de 1 a 30 letras sin tildes,"
                                + " dígitos, espacio, «.», «_» o «-», empezando con letra o"
                                + " dígito.",
                        "«MT» no puede traducirse a «na», que significa «no aplica».",
                        "El valor «01» se repite en «CEDULA» y «PASAPORTE»: cada código necesita"
                                + " un valor distinto.");
    }

    @Test
    void dadosMasDe50Problemas_cuandoSeRechaza_entoncesDa50YCuantosMas() {
        List<BorradorDeColumna> malas = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            malas.add(columna("C" + i, "no_existe"));
        }
        ValidacionDelLayout validacion =
                LayoutExportacion.validar(new BorradorDeLayout(malas, List.of()));

        LayoutInvalido invalido = new LayoutInvalido(validacion.problemas());

        assertThat(invalido.problemas()).hasSize(51);
        assertThat(invalido.problemas().get(50).regla())
                .startsWith("Hay ")
                .endsWith(" problemas más.");
    }

    @Test
    void dadosProblemasDelLector_cuandoSeValida_entoncesVanPrimero() {
        ProblemaDelLayout delLector =
                new ProblemaDelLayout("archivo", "La clave «colunas» no existe. Usa «columnas».");

        ValidacionDelLayout validacion =
                LayoutExportacion.validar(
                        new BorradorDeLayout(
                                List.of(IDENTIFICACION, COMPONENTE, HORAS), List.of(delLector)));

        assertThat(validacion.problemas()).containsExactly(delLector);
        assertThat(validacion.esValida()).isFalse();
    }

    private static List<String> reglas(ValidacionDelLayout validacion) {
        return validacion.problemas().stream().map(ProblemaDelLayout::regla).toList();
    }

    static BorradorDeLayout borrador(BorradorDeColumna... columnas) {
        return new BorradorDeLayout(List.of(columnas), List.of());
    }

    static BorradorDeColumna columna(String nombre, String campo) {
        return new BorradorDeColumna(nombre, campo, null);
    }

    static BorradorDeColumna traducida(
            String nombre, String campo, Map<String, String> traduccion) {
        return new BorradorDeColumna(nombre, campo, new LinkedHashMap<>(traduccion));
    }

    private static Map<String, String> orden(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(pares[i], pares[i + 1]);
        }
        return mapa;
    }
}
