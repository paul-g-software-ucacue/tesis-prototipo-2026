package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.dominio.Borrador.EtapaEscrita;
import ec.mdd.validacionocs.dominio.Borrador.FirmaEscrita;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de un archivo de configuración, y que las dice todas (RF-34, RF-58, FR-015). */
class ConfiguracionFlujoTest {

    private static final Firmantes TODOS = new Firmantes(Set.of("VICERRECTOR", "RECTOR"), true);

    private final List<EtapaEscrita> etapas = new ArrayList<>(etapasValidas());
    private final List<FirmaEscrita> firmas = new ArrayList<>(firmasValidas());

    private static List<EtapaEscrita> etapasValidas() {
        List<EtapaEscrita> todas = new ArrayList<>();
        for (TipoDeEtapa tipo : TipoDeEtapa.values()) {
            todas.add(etapa(tipo.name(), "true", "Nombre", "VICERRECTOR", "5", "false"));
        }
        return todas;
    }

    private static List<FirmaEscrita> firmasValidas() {
        return List.of(
                firma("1", "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                firma("2", "Revisado", "VICERRECTOR"),
                firma("3", "Aprobado", "RECTOR"));
    }

    private static EtapaEscrita etapa(
            String tipo, String activa, String nombre, String rol, String plazo, String devuelve) {
        return new EtapaEscrita(tipo, activa, nombre, rol, plazo, devuelve);
    }

    private static FirmaEscrita firma(String orden, String etiqueta, String rol) {
        return new FirmaEscrita(orden, etiqueta, rol, "CONGELADO_EN_FIRMA", "CONSOLIDADO");
    }

    private void cambiarEtapa(TipoDeEtapa tipo, EtapaEscrita nueva) {
        etapas.set(tipo.ordinal(), nueva);
    }

    private void desactivar(TipoDeEtapa tipo) {
        cambiarEtapa(tipo, etapa(tipo.name(), "false", "Nombre", "VICERRECTOR", "5", "false"));
    }

    private Revision revisar() {
        return ConfiguracionFlujo.revisar(new Borrador(etapas, firmas, List.of()), TODOS);
    }

    private List<String> problemas() {
        return revisar().problemas();
    }

    @Test
    void dadoUnArchivoValido_cuandoSeRevisa_entoncesDaLaConfiguracionSinProblemas() {
        Revision revision = revisar();

        assertThat(revision.problemas()).isEmpty();
        ConfiguracionFlujo configuracion = revision.configuracion().orElseThrow();
        assertThat(configuracion.etapas())
                .extracting(Etapa::tipo)
                .containsExactly(TipoDeEtapa.values());
        assertThat(configuracion.firmas())
                .extracting(PasoFirma::orden, PasoFirma::etiqueta)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "Elaborado"),
                        org.assertj.core.groups.Tuple.tuple(2, "Revisado"),
                        org.assertj.core.groups.Tuple.tuple(3, "Aprobado"));
    }

    @Test
    void dadasLasEtapasDesordenadas_cuandoSeRevisa_entoncesQuedanEnElOrdenDelFlujo() {
        java.util.Collections.reverse(etapas);

        assertThat(revisar().configuracion().orElseThrow().etapas())
                .extracting(Etapa::tipo)
                .containsExactly(TipoDeEtapa.values());
    }

    @Test
    void dadasLasFirmasDesordenadas_cuandoSeRevisa_entoncesQuedanPorSuOrden() {
        java.util.Collections.reverse(firmas);

        assertThat(revisar().configuracion().orElseThrow().firmas())
                .extracting(PasoFirma::orden)
                .containsExactly(1, 2, 3);
    }

    @Test
    void dadoUnaEtapaActiva_cuandoSeRevisa_entoncesConservaSusDatos() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "En el OCS", "SECRETARIA_GENERAL", "10", "true"));

        assertThat(revisar().configuracion().orElseThrow().etapas())
                .contains(
                        new Etapa(
                                TipoDeEtapa.EN_OCS,
                                true,
                                "En el OCS",
                                "SECRETARIA_GENERAL",
                                OptionalInt.of(10),
                                true));
    }

    @Test
    void dadoElEsqueleto_cuandoSeConsulta_entoncesSonLasCincoEtapasFijas() {
        assertThat(TipoDeEtapa.values())
                .filteredOn(TipoDeEtapa::esEsqueleto)
                .containsExactly(
                        TipoDeEtapa.BORRADOR_CARRERA,
                        TipoDeEtapa.CONGELADO_EN_FIRMA,
                        TipoDeEtapa.EN_OCS,
                        TipoDeEtapa.APROBADO,
                        TipoDeEtapa.PUBLICADO);
    }

    @ParameterizedTest
    @EnumSource(TipoDeEtapa.class)
    void dadaUnaEtapaQueFalta_cuandoSeRevisa_entoncesDiceCual(TipoDeEtapa tipo) {
        etapas.remove(tipo.ordinal());

        assertThat(problemas()).contains("Falta la etapa " + tipo.name() + ".");
    }

    @Test
    void dadaUnaEtapaQueNoExiste_cuandoSeRevisa_entoncesDiceCual() {
        etapas.add(etapa("EN_ALGO", "true", "Nombre", "VICERRECTOR", "5", "false"));

        assertThat(problemas()).containsExactly("La etapa EN_ALGO no existe.");
    }

    @Test
    void dadaUnaEtapaRepetida_cuandoSeRevisa_entoncesLoDice() {
        etapas.add(etapa("EN_OCS", "true", "Nombre", "VICERRECTOR", "5", "false"));

        assertThat(problemas()).containsExactly("La etapa EN_OCS está repetida.");
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"BORRADOR_CARRERA", "CONGELADO_EN_FIRMA", "EN_OCS", "APROBADO", "PUBLICADO"})
    void dadaUnaEtapaDelEsqueletoDesactivada_cuandoSeRevisa_entoncesSeRechaza(TipoDeEtapa tipo) {
        desactivar(tipo);
        if (tipo == TipoDeEtapa.CONGELADO_EN_FIRMA) {
            firmas.clear();
            firmas.add(new FirmaEscrita("1", "Elaborado", "RECTOR", "EN_OCS", "CONSOLIDADO"));
        }

        assertThat(problemas()).contains("La etapa " + tipo.name() + " no se puede desactivar.");
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"BORRADOR_UNIDADES", "EN_CONSOLIDACION", "EN_REVISION_VRA", "REMITIDO_ZONAL"})
    void dadaUnaEtapaOpcionalDesactivada_cuandoSeRevisa_entoncesEsValida(TipoDeEtapa tipo) {
        desactivar(tipo);

        assertThat(problemas()).isEmpty();
    }

    @Test
    void dadaUnaEtapaOpcionalDesactivadaSinPlazo_cuandoSeRevisa_entoncesEsValida() {
        cambiarEtapa(
                TipoDeEtapa.REMITIDO_ZONAL,
                etapa("REMITIDO_ZONAL", "false", "Nombre", "VICERRECTOR", null, "false"));

        assertThat(problemas()).isEmpty();
    }

    @Test
    void dadoQueActivaNoEsVerdaderoNiFalso_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS, etapa("EN_OCS", "si", "Nombre", "VICERRECTOR", "5", "false"));

        assertThat(problemas())
                .containsExactly("En la etapa EN_OCS, activa debe ser true o false.");
    }

    @Test
    void dadoQueAdmiteDevolucionNoEsVerdaderoNiFalso_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS, etapa("EN_OCS", "true", "Nombre", "VICERRECTOR", "5", null));

        assertThat(problemas())
                .containsExactly("En la etapa EN_OCS, admite_devolucion debe ser true o false.");
    }

    @Test
    void dadaUnaEtapaSinNombre_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS, etapa("EN_OCS", "true", "  ", "VICERRECTOR", "5", "false"));

        assertThat(problemas()).containsExactly("A la etapa EN_OCS le falta el nombre.");
    }

    @Test
    void dadoUnNombreDeOchentaCaracteres_cuandoSeRevisa_entoncesEsValido() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "n".repeat(80), "VICERRECTOR", "5", "false"));

        assertThat(problemas()).isEmpty();
    }

    @Test
    void dadoUnNombreDeMasDeOchentaCaracteres_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "n".repeat(81), "VICERRECTOR", "5", "false"));

        assertThat(problemas())
                .containsExactly("El nombre de la etapa EN_OCS pasa de 80 caracteres.");
    }

    @Test
    void dadaUnaEtapaSinRol_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(TipoDeEtapa.EN_OCS, etapa("EN_OCS", "true", "Nombre", null, "5", "false"));

        assertThat(problemas()).containsExactly("A la etapa EN_OCS le falta el rol.");
    }

    @Test
    void dadoUnRolQueNoExiste_cuandoSeRevisa_entoncesDiceCualYDonde() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS, etapa("EN_OCS", "true", "Nombre", "CONSERJE", "5", "false"));

        assertThat(problemas())
                .containsExactly("La etapa EN_OCS tiene el rol CONSERJE, que no existe.");
    }

    @Test
    void dadaUnaEtapaActivaSinPlazo_cuandoSeRevisa_entoncesLoDice() {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "Nombre", "VICERRECTOR", null, "false"));

        assertThat(problemas()).containsExactly("A la etapa EN_OCS le falta el plazo.");
    }

    @Test
    void dadoPublicadoSinPlazo_cuandoSeRevisa_entoncesEsValido() {
        cambiarEtapa(
                TipoDeEtapa.PUBLICADO,
                etapa("PUBLICADO", "true", "Nombre", "VICERRECTOR", null, "false"));

        assertThat(problemas()).isEmpty();
        assertThat(revisar().configuracion().orElseThrow().etapas())
                .filteredOn(una -> una.tipo() == TipoDeEtapa.PUBLICADO)
                .extracting(Etapa::plazoDias)
                .containsExactly(OptionalInt.empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "cinco", "99999", "0x10"})
    void dadoUnPlazoInvalido_cuandoSeRevisa_entoncesLoDice(String plazo) {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "Nombre", "VICERRECTOR", plazo, "false"));

        assertThat(problemas())
                .containsExactly(
                        "El plazo de la etapa EN_OCS debe ser un número entero de días, mayor que"
                                + " 0.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "365", "9999"})
    void dadoUnPlazoValido_cuandoSeRevisa_entoncesLoAcepta(String plazo) {
        cambiarEtapa(
                TipoDeEtapa.EN_OCS,
                etapa("EN_OCS", "true", "Nombre", "VICERRECTOR", plazo, "false"));

        assertThat(problemas()).isEmpty();
    }

    @Test
    void sinFirmas_cuandoSeRevisa_entoncesLoDice() {
        firmas.clear();

        assertThat(problemas()).containsExactly("Falta el árbol de firmas.");
    }

    @Test
    void dadoUnOrdenRepetido_cuandoSeRevisa_entoncesDiceCual() {
        firmas.set(2, firma("2", "Aprobado", "RECTOR"));

        assertThat(problemas()).contains("El orden 2 de las firmas está repetido.");
    }

    @Test
    void dadoUnHuecoEnElOrden_cuandoSeRevisa_entoncesDiceCualFalta() {
        firmas.set(2, firma("4", "Aprobado", "RECTOR"));

        assertThat(problemas())
                .containsExactly("Falta la firma de orden 3: van desde 1, sin huecos.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "uno", "1.5"})
    void dadoUnOrdenInvalido_cuandoSeRevisa_entoncesLoDice(String orden) {
        firmas.set(0, firma(orden, "Elaborado", "RESPONSABLE_CONSOLIDACION"));

        assertThat(problemas())
                .contains("El orden de la firma Elaborado debe ser un número entero desde 1.");
    }

    @Test
    void dadaUnaFirmaSinEtiqueta_cuandoSeRevisa_entoncesDiceSuOrden() {
        firmas.set(0, firma("1", null, "RESPONSABLE_CONSOLIDACION"));

        assertThat(problemas()).containsExactly("A la firma de orden 1 le falta la etiqueta.");
    }

    @Test
    void dadaUnaEtiquetaDeMasDeCuarentaCaracteres_cuandoSeRevisa_entoncesLoDice() {
        firmas.set(0, firma("1", "e".repeat(41), "RESPONSABLE_CONSOLIDACION"));

        assertThat(problemas())
                .containsExactly("La etiqueta de la firma de orden 1 pasa de 40 caracteres.");
    }

    @Test
    void dadaUnaFirmaConUnRolQueNoExiste_cuandoSeRevisa_entoncesDiceCual() {
        firmas.set(1, firma("2", "Revisado", "CONSERJE"));

        assertThat(problemas())
                .containsExactly("La firma Revisado tiene el rol CONSERJE, que no existe.");
    }

    @Test
    void dadaUnaFirmaSinRol_cuandoSeRevisa_entoncesLoDice() {
        firmas.set(1, firma("2", "Revisado", null));

        assertThat(problemas()).containsExactly("A la firma Revisado le falta el rol.");
    }

    @Test
    void dadaUnaFirmaEnUnaEtapaDesactivada_cuandoSeRevisa_entoncesDiceCual() {
        desactivar(TipoDeEtapa.EN_REVISION_VRA);
        firmas.set(
                1,
                new FirmaEscrita("2", "Revisado", "VICERRECTOR", "EN_REVISION_VRA", "CONSOLIDADO"));

        assertThat(problemas())
                .containsExactly("La firma Revisado está en EN_REVISION_VRA, que no está activa.");
    }

    @Test
    void dadaUnaFirmaEnUnaEtapaQueNoExiste_cuandoSeRevisa_entoncesDiceCual() {
        firmas.set(1, new FirmaEscrita("2", "Revisado", "VICERRECTOR", "EN_ALGO", "CONSOLIDADO"));

        assertThat(problemas())
                .containsExactly("La firma Revisado está en EN_ALGO, que no existe.");
    }

    @Test
    void dadaUnaFirmaSinEtapa_cuandoSeRevisa_entoncesLoDice() {
        firmas.set(1, new FirmaEscrita("2", "Revisado", "VICERRECTOR", null, "CONSOLIDADO"));

        assertThat(problemas()).containsExactly("A la firma Revisado le falta la etapa.");
    }

    @Test
    void dadaUnaFirmaSobreUnDocumentoQueNoExiste_cuandoSeRevisa_entoncesDiceCual() {
        firmas.set(1, new FirmaEscrita("2", "Revisado", "VICERRECTOR", "EN_OCS", "CONTRATO"));

        assertThat(problemas())
                .containsExactly(
                        "La firma Revisado es sobre CONTRATO, un documento que no existe.");
    }

    @Test
    void dadaUnaFirmaSinDocumento_cuandoSeRevisa_entoncesLoDice() {
        firmas.set(1, new FirmaEscrita("2", "Revisado", "VICERRECTOR", "EN_OCS", null));

        assertThat(problemas()).containsExactly("A la firma Revisado le falta el documento.");
    }

    @Test
    void dadoUnRolFirmanteSinUsuario_cuandoSeRevisa_entoncesDiceCualUnaVez() {
        firmas.add(firma("4", "Otra vez", "RECTOR"));

        Revision revision =
                ConfiguracionFlujo.revisar(
                        new Borrador(etapas, firmas, List.of()),
                        new Firmantes(Set.of("VICERRECTOR"), true));

        assertThat(revision.problemas()).containsExactly("El rol RECTOR no tiene usuarios.");
    }

    @Test
    void sinResponsableDesignado_cuandoSeRevisa_entoncesLoDice() {
        Revision revision =
                ConfiguracionFlujo.revisar(
                        new Borrador(etapas, firmas, List.of()),
                        new Firmantes(Set.of("VICERRECTOR", "RECTOR"), false));

        assertThat(revision.problemas())
                .containsExactly("Falta designar al Responsable de consolidación del periodo.");
    }

    @Test
    void sinComprobarFirmantes_cuandoSeRevisa_entoncesLosRolesSinUsuarioNoImportan() {
        assertThat(ConfiguracionFlujo.revisar(new Borrador(etapas, firmas, List.of())).problemas())
                .isEmpty();
    }

    @Test
    void dadosVariosProblemas_cuandoSeRevisa_entoncesLosDiceTodosSinConfiguracion() {
        etapas.remove(TipoDeEtapa.EN_OCS.ordinal());
        firmas.set(2, firma("3", "Aprobado", "CONSERJE"));
        cambiarEtapa(
                TipoDeEtapa.BORRADOR_CARRERA,
                etapa("BORRADOR_CARRERA", "true", "Nombre", "VICERRECTOR", "0", "false"));

        Revision revision =
                ConfiguracionFlujo.revisar(
                        new Borrador(etapas, firmas, List.of("La clave nada no existe.")),
                        new Firmantes(Set.of(), false));

        assertThat(revision.configuracion()).isEmpty();
        assertThat(revision.problemas())
                .containsExactlyInAnyOrder(
                        "La clave nada no existe.",
                        "Falta la etapa EN_OCS.",
                        "El plazo de la etapa BORRADOR_CARRERA debe ser un número entero de días,"
                                + " mayor que 0.",
                        "La firma Aprobado tiene el rol CONSERJE, que no existe.",
                        "El rol VICERRECTOR no tiene usuarios.",
                        "Falta designar al Responsable de consolidación del periodo.");
    }

    @Test
    void dadoUnProblemaDelLector_cuandoSeRevisa_entoncesNoHayConfiguracion() {
        Revision revision =
                ConfiguracionFlujo.revisar(
                        new Borrador(etapas, firmas, List.of("La clave nada no existe.")));

        assertThat(revision.configuracion()).isEmpty();
    }

    @Test
    void dadoUnTextoLarguisimo_cuandoSeRevisa_entoncesNoLoRepiteCompleto() {
        etapas.add(etapa("X".repeat(1000), "true", "Nombre", "VICERRECTOR", "5", "false"));

        assertThat(problemas()).hasSize(1).first().asString().hasSizeLessThan(80);
    }

    @Test
    void dadosMuchisimosProblemas_cuandoSeRevisa_entoncesDiceCincuentaYCuantosMas() {
        for (int indice = 0; indice < 60; indice++) {
            etapas.add(etapa("OTRA_" + indice, "true", "Nombre", "VICERRECTOR", "5", "false"));
        }

        assertThat(problemas()).hasSize(51).last().isEqualTo("Hay 10 problemas más.");
    }
}
