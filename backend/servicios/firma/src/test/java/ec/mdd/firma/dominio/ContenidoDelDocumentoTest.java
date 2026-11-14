package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Los firmantes del bloque de firmas (F13-H02, FR-006 a FR-008). */
class ContenidoDelDocumentoTest {

    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                    new PasoDeFirma(3, "Aprobado", "RECTOR"));

    @Test
    void dadaLaCadenaYLosNombres_cuandoSeArmanLosFirmantes_entoncesRotuloCargoNombreYCampo() {
        List<Firmante> firmantes =
                Firmante.de(
                        CADENA,
                        Map.of(
                                "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                                "VICERRECTOR", "LÓPEZ LUIS",
                                "RECTOR", "MORA EVA"));

        assertThat(firmantes)
                .containsExactly(
                        new Firmante(
                                1,
                                "Elaborado por",
                                "Responsable de consolidación",
                                "PÉREZ ANA",
                                "firma-1-RESPONSABLE_CONSOLIDACION"),
                        new Firmante(
                                2,
                                "Revisado por",
                                "Vicerrector académico",
                                "LÓPEZ LUIS",
                                "firma-2-VICERRECTOR"),
                        new Firmante(3, "Aprobado por", "Rector", "MORA EVA", "firma-3-RECTOR"));
    }

    @Test
    void dadaUnaCadenaFueraDeOrden_cuandoSeArmanLosFirmantes_entoncesQuedanEnSuOrden() {
        List<Firmante> firmantes =
                Firmante.de(
                        List.of(CADENA.get(2), CADENA.get(0), CADENA.get(1)),
                        Map.of(
                                "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                                "VICERRECTOR", "LÓPEZ LUIS",
                                "RECTOR", "MORA EVA"));

        assertThat(firmantes).extracting(Firmante::orden).containsExactly(1, 2, 3);
    }

    @Test
    void dadoUnFirmanteSinNombre_cuandoSeArmanLosFirmantes_entoncesFirmanteSinNombreConElCargo() {
        assertThatThrownBy(
                        () ->
                                Firmante.de(
                                        CADENA,
                                        Map.of(
                                                "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                                                "VICERRECTOR", " ")))
                .isInstanceOfSatisfying(
                        FirmanteSinNombre.class,
                        error -> assertThat(error.cargo()).isEqualTo("Vicerrector académico"))
                .hasMessage("Falta el nombre del Vicerrector académico.");
    }

    @Test
    void dadoUnRolSinCargoConocido_cuandoSeArmanLosFirmantes_entoncesElCargoEsElRol() {
        assertThat(
                        Firmante.de(
                                        List.of(
                                                new PasoDeFirma(
                                                        1, "Aprobado", "SECRETARIA_GENERAL")),
                                        Map.of("SECRETARIA_GENERAL", "RÍOS JUAN"))
                                .getFirst()
                                .cargo())
                .isEqualTo("Secretaría general");
    }
}
