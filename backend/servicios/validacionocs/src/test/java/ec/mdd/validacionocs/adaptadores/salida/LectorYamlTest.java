package ec.mdd.validacionocs.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.Borrador;
import ec.mdd.validacionocs.dominio.Borrador.EtapaEscrita;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.dominio.Etapa;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.PasoFirma;
import ec.mdd.validacionocs.dominio.Revision;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El archivo se lee sin interpretar nada: ni anclas, ni alias, ni tipos (FR-009, FR-011). La
 * plantilla es la de verdad, la misma que se descarga (FR-008).
 */
class LectorYamlTest {

    private static final String ETAPA =
            """
            etapas:
              EN_OCS:
                activa: true
                nombre: Resolución
                rol: SECRETARIA_GENERAL
                plazo_dias: 10
                admite_devolucion: true
            """;

    private final LectorYaml lector = new LectorYaml();

    private void assertIlegible(String texto) {
        assertThatThrownBy(() -> lector.leer(texto)).isInstanceOf(ArchivoIlegible.class);
    }

    @Test
    void dadaLaPlantilla_cuandoSeLee_entoncesEsValidaYRigeConLosValoresPorDefecto() {
        Revision revision =
                ConfiguracionFlujo.revisar(
                        lector.leer(new PlantillaEnRecursos().texto()),
                        new Firmantes(Set.of("VICERRECTOR", "RECTOR"), true));

        assertThat(revision.problemas()).isEmpty();
        ConfiguracionFlujo configuracion = revision.configuracion().orElseThrow();
        assertThat(configuracion.etapas())
                .extracting(Etapa::tipo, Etapa::activa, Etapa::rol, Etapa::plazoDias)
                .containsExactly(
                        tuple(
                                TipoDeEtapa.BORRADOR_CARRERA,
                                true,
                                "COORDINADOR_CARRERA",
                                OptionalInt.of(15)),
                        tuple(
                                TipoDeEtapa.BORRADOR_UNIDADES,
                                false,
                                "GESTOR_UNIDAD",
                                OptionalInt.of(10)),
                        tuple(
                                TipoDeEtapa.EN_CONSOLIDACION,
                                true,
                                "RESPONSABLE_CONSOLIDACION",
                                OptionalInt.of(5)),
                        tuple(TipoDeEtapa.EN_REVISION_VRA, true, "VICERRECTOR", OptionalInt.of(5)),
                        tuple(
                                TipoDeEtapa.CONGELADO_EN_FIRMA,
                                true,
                                "RESPONSABLE_CONSOLIDACION",
                                OptionalInt.of(5)),
                        tuple(TipoDeEtapa.EN_OCS, true, "SECRETARIA_GENERAL", OptionalInt.of(10)),
                        tuple(TipoDeEtapa.REMITIDO_ZONAL, false, "VICERRECTOR", OptionalInt.of(10)),
                        tuple(TipoDeEtapa.APROBADO, true, "SECRETARIA_GENERAL", OptionalInt.of(3)),
                        tuple(
                                TipoDeEtapa.PUBLICADO,
                                true,
                                "SECRETARIA_GENERAL",
                                OptionalInt.empty()));
        assertThat(configuracion.firmas())
                .extracting(PasoFirma::etiqueta, PasoFirma::rol)
                .containsExactly(
                        tuple("Elaborado", "RESPONSABLE_CONSOLIDACION"),
                        tuple("Revisado", "VICERRECTOR"),
                        tuple("Aprobado", "RECTOR"));
    }

    @Test
    void dadaLaPlantilla_cuandoSeDescarga_entoncesEstaComentadaEnEspanol() {
        assertThat(new PlantillaEnRecursos().texto())
                .contains("# Configuración del flujo")
                .contains("días corridos");
    }

    @Test
    void dadaUnaEtapa_cuandoSeLee_entoncesTraeCadaValorComoSeEscribio() {
        assertThat(lector.leer(ETAPA).etapas())
                .containsExactly(
                        new EtapaEscrita(
                                "EN_OCS",
                                "true",
                                "Resolución",
                                "SECRETARIA_GENERAL",
                                "10",
                                "true"));
    }

    @Test
    void dadoUnValorNulo_cuandoSeLee_entoncesFalta() {
        Borrador borrador = lector.leer("etapas:\n  EN_OCS:\n    nombre: ~\n    rol:\n");

        assertThat(borrador.etapas())
                .containsExactly(new EtapaEscrita("EN_OCS", null, null, null, null, null));
    }

    @Test
    void dadaUnaFirma_cuandoSeLee_entoncesTraeCadaValorComoSeEscribio() {
        Borrador borrador =
                lector.leer(
                        """
                        firmas:
                          - orden: 1
                            etiqueta: Elaborado
                            rol: RECTOR
                            etapa: EN_OCS
                            documento: CONSOLIDADO
                        """);

        assertThat(borrador.firmas())
                .containsExactly(
                        new Borrador.FirmaEscrita(
                                "1", "Elaborado", "RECTOR", "EN_OCS", "CONSOLIDADO"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "base: &a 1",
                "a: &x hola\nb: *x",
                "<<: *x",
                "a: !!java.util.Date 2026-01-01",
                "a: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[]]]",
                "a: !raro 1",
                "a: !!str 1",
                "a: !!map {b: 1}",
                "a: !!seq [1]"
            })
    void dadasAnclasAliasOTipos_cuandoSeLee_entoncesSeRechazaSinInterpretarlos(String texto) {
        assertIlegible(texto);
    }

    @Test
    void dadaUnaBombaDeAlias_cuandoSeLee_entoncesSeRechaza() {
        assertIlegible(
                """
                a: &a [x, x]
                b: &b [*a, *a]
                c: &c [*b, *b]
                d: &d [*c, *c]
                """);
    }

    @Test
    void dadaUnaAnidacionEnorme_cuandoSeLee_entoncesSeRechaza() {
        assertIlegible("[".repeat(500_000));
    }

    @Test
    void dadosDosDocumentos_cuandoSeLee_entoncesSeRechaza() {
        assertIlegible("---\netapas: {}\n---\nfirmas: []\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   \n", "# solo un comentario\n", "hola", "- a\n- b", "42"})
    void dadoUnArchivoSinUnaRaiz_cuandoSeLee_entoncesSeRechaza(String texto) {
        assertIlegible(texto);
    }

    @Test
    void dadoUnCaracterDeControl_cuandoSeLee_entoncesSeRechaza() {
        assertIlegible("etapas: \u0000\n");
    }

    @Test
    void dadoYamlRoto_cuandoSeLee_entoncesDiceLaLineaSinDetallesDelLector() {
        assertThatThrownBy(() -> lector.leer("etapas:\n  a: 1\n b: [\n"))
                .isInstanceOfSatisfying(
                        ArchivoIlegible.class,
                        error -> {
                            assertThat(error.linea()).isPresent();
                            assertThat(error.getMessage()).isNull();
                        });
    }

    @Test
    void dadaUnaClaveQueNoExiste_cuandoSeLee_entoncesLaDiceConSuLinea() {
        assertThat(lector.leer("etapas: {}\notra: 1\n").problemas())
                .containsExactly("Línea 2: la clave otra no existe.");
    }

    @Test
    void dadaUnaClaveDeEtapaQueNoExiste_cuandoSeLee_entoncesLaDice() {
        assertThat(lector.leer("etapas:\n  EN_OCS:\n    plazo: 5\n").problemas())
                .containsExactly("Línea 3: la clave plazo no existe.");
    }

    @Test
    void dadaUnaClaveDeFirmaQueNoExiste_cuandoSeLee_entoncesLaDice() {
        assertThat(lector.leer("firmas:\n  - nivel: 1\n").problemas())
                .containsExactly("Línea 2: la clave nivel no existe.");
    }

    @Test
    void dadaUnaClaveRepetida_cuandoSeLee_entoncesLaDice() {
        assertThat(lector.leer("etapas: {}\netapas: {}\n").problemas())
                .containsExactly("Línea 2: la clave etapas está repetida.");
        assertThat(lector.leer("etapas:\n  EN_OCS:\n    rol: A\n    rol: B\n").problemas())
                .containsExactly("Línea 4: la clave rol está repetida.");
    }

    @Test
    void dadaUnaEtapaRepetida_cuandoSeLee_entoncesLasDosLleganAlDominio() {
        assertThat(lector.leer("etapas:\n  EN_OCS: {}\n  EN_OCS: {}\n").etapas()).hasSize(2);
    }

    @Test
    void dadasLasEtapasComoLista_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("etapas:\n  - EN_OCS\n").problemas())
                .containsExactly("Línea 2: etapas debe ser un mapa con cada etapa.");
    }

    @Test
    void dadasLasFirmasComoMapa_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("firmas:\n  orden: 1\n").problemas())
                .containsExactly("Línea 2: firmas debe ser una lista.");
    }

    @Test
    void dadaUnaEtapaSinDatos_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("etapas:\n  EN_OCS: 5\n").problemas())
                .containsExactly("Línea 2: la etapa EN_OCS debe tener sus datos.");
    }

    @Test
    void dadaUnaFirmaSinDatos_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("firmas:\n  - 5\n").problemas())
                .containsExactly("Línea 2: cada firma debe tener sus datos.");
    }

    @Test
    void dadoUnValorQueEsUnaLista_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("etapas:\n  EN_OCS:\n    nombre: [a]\n").problemas())
                .containsExactly("Línea 3: nombre debe ser un solo valor.");
    }

    @Test
    void dadaUnaClaveQueNoEsTexto_cuandoSeLee_entoncesLoDice() {
        assertThat(lector.leer("? [a]\n: 1\n").problemas())
                .containsExactly("Línea 1: una clave debe ser un texto.");
    }

    @Test
    void dadaUnaClaveLarguisima_cuandoSeLee_entoncesNoLaRepiteCompleta() {
        assertThat(lector.leer("x".repeat(500) + ": 1\n").problemas())
                .hasSize(1)
                .first()
                .asString()
                .hasSizeLessThan(80);
    }
}
