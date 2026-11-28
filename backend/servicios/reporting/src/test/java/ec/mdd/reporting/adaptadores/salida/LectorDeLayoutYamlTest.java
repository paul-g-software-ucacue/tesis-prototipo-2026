package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.BorradorDeColumna;
import ec.mdd.reporting.dominio.ProblemaDelLayout;
import ec.mdd.reporting.puertos.salida.LectorDeLayout.LecturaDelLayout;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Leer el archivo de mapeo sin riesgo (research §7, D-119), como el de F12-H01. */
class LectorDeLayoutYamlTest {

    private final LectorDeLayoutYaml lector = new LectorDeLayoutYaml();

    @Test
    void dadoUnArchivoValido_cuandoSeLee_entoncesDaCadaColumnaConSuTextoEscrito() {
        LecturaDelLayout lectura =
                leer(
                        """
                        # comentario
                        columnas:
                          - nombre: PERIODO
                            campo: periodo
                          - nombre: COMPONENTE
                            campo: componente
                            traduccion:
                              GESTION: "05"
                              CLASE: 01
                        """);

        assertThat(lectura.borrador().problemas()).isEmpty();
        assertThat(lectura.borrador().columnas())
                .containsExactly(
                        new BorradorDeColumna("PERIODO", "periodo", null),
                        new BorradorDeColumna(
                                "COMPONENTE",
                                "componente",
                                java.util.Map.of("GESTION", "05", "CLASE", "01")));
        assertThat(lectura.borrador().columnas().get(1).traduccion().keySet())
                .containsExactly("GESTION", "CLASE");
        assertThat(lectura.texto()).startsWith("# comentario");
    }

    @Test
    void dadoUnArchivoConBom_cuandoSeLee_entoncesLoQuita() {
        byte[] conBom = ("﻿columnas: []\n").getBytes(StandardCharsets.UTF_8);

        LecturaDelLayout lectura = lector.leer(conBom);

        assertThat(lectura.texto()).isEqualTo("columnas: []\n");
        assertThat(lectura.borrador().columnas()).isEmpty();
    }

    @Test
    void dadoUnAnclaUnAliasOUnaEtiqueta_cuandoSeLee_entoncesEsIlegibleConSuLinea() {
        assertThatThrownBy(
                        () ->
                                leer(
                                        """
                                        columnas:
                                          - &a {nombre: A, campo: periodo}
                                          - *a
                                        """))
                .isInstanceOfSatisfying(
                        ArchivoIlegible.class, error -> assertThat(error.linea()).hasValue(2));
        assertThatThrownBy(() -> leer("columnas: !!python/object {}\n"))
                .isInstanceOf(ArchivoIlegible.class);
        assertThatThrownBy(() -> leer("columnas:\n  - nombre: !!str A\n"))
                .isInstanceOf(ArchivoIlegible.class);
    }

    @Test
    void dadoUnUtf8InvalidoOUnYamlRoto_cuandoSeLee_entoncesEsIlegible() {
        assertThatThrownBy(() -> lector.leer(new byte[] {'c', ':', ' ', (byte) 0xC3, (byte) 0x28}))
                .isInstanceOf(ArchivoIlegible.class);
        assertThatThrownBy(() -> leer("columnas: [\n"))
                .isInstanceOfSatisfying(
                        ArchivoIlegible.class,
                        error -> {
                            assertThat(error.linea()).isPresent();
                            assertThat(error.getMessage()).isNotBlank();
                        });
        assertThatThrownBy(() -> leer("- solo una lista\n")).isInstanceOf(ArchivoIlegible.class);
    }

    @Test
    void dadoUnArchivoDeMasDe4Niveles_cuandoSeLee_entoncesEsIlegible() {
        assertThatThrownBy(
                        () ->
                                leer(
                                        """
                                        columnas:
                                          - nombre: A
                                            campo: periodo
                                            traduccion:
                                              CLASE:
                                                hondo: 1
                                        """))
                .isInstanceOf(ArchivoIlegible.class);
    }

    @Test
    void dadasClavesDesconocidasONodosEquivocados_cuandoSeLee_entoncesSonProblemasDelBorrador() {
        LecturaDelLayout lectura =
                leer(
                        """
                        colunas: []
                        columnas:
                          - nombre: A
                            campo: periodo
                            posicion: 1
                          - solo texto
                          - nombre: [A, B]
                            campo: periodo
                            traduccion: CLASE
                        """);

        assertThat(lectura.borrador().problemas())
                .extracting(ProblemaDelLayout::columna, ProblemaDelLayout::regla)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "archivo",
                                "Línea 1: la clave «colunas» no existe. Usa solo «columnas»."),
                        org.assertj.core.groups.Tuple.tuple(
                                "columnas[1]",
                                "Línea 5: la clave «posicion» no existe. Usa solo «nombre»,"
                                        + " «campo» y «traduccion»."),
                        org.assertj.core.groups.Tuple.tuple(
                                "columnas[2]", "Línea 6: cada columna tiene «nombre» y «campo»."),
                        org.assertj.core.groups.Tuple.tuple(
                                "columnas[3]", "Línea 7: «nombre» debe ser un solo valor."),
                        org.assertj.core.groups.Tuple.tuple(
                                "columnas[3]",
                                "Línea 9: «traduccion» debe tener un código y su valor por"
                                        + " línea."));
    }

    @Test
    void dadaUnaClaveRepetida_cuandoSeLee_entoncesEsUnProblema() {
        LecturaDelLayout lectura =
                leer("columnas:\n  - nombre: A\n    nombre: B\n    campo: periodo\n");

        assertThat(lectura.borrador().problemas())
                .extracting(ProblemaDelLayout::regla)
                .containsExactly("Línea 3: la clave «nombre» está repetida.");
    }

    private LecturaDelLayout leer(String yaml) {
        return lector.leer(yaml.getBytes(StandardCharsets.UTF_8));
    }
}
