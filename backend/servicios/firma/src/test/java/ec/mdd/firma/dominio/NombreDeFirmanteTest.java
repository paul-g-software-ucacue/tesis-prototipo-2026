package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** La normalización del nombre de D-23 (F14-H03, D-104): mayúsculas, sin tildes y espacios. */
class NombreDeFirmanteTest {

    @Test
    void dadoUnNombreConTildesYMinusculas_cuandoSeNormaliza_entoncesQuedaEnMayusculasSinTildes() {
        assertThat(NombreDeFirmante.normalizar("Peralta Maldonado Juan Andrés"))
                .isEqualTo("PERALTA MALDONADO JUAN ANDRES");
        assertThat(NombreDeFirmante.normalizar("Müller Ángel Íñigo"))
                .isEqualTo("MULLER ANGEL IÑIGO");
    }

    @Test
    void dadaLaEnie_cuandoSeNormaliza_entoncesSeConserva() {
        assertThat(NombreDeFirmante.normalizar("Núñez Peña")).isEqualTo("NUÑEZ PEÑA");
    }

    @Test
    void dadosEspaciosDeMas_cuandoSeNormaliza_entoncesQuedanUnificados() {
        assertThat(NombreDeFirmante.normalizar("  CABRERA   VAZQUEZ\tANA  GABRIELA "))
                .isEqualTo("CABRERA VAZQUEZ ANA GABRIELA");
    }

    @Test
    void dadosParentesis_cuandoSeNormaliza_entoncesSeConservan() {
        assertThat(NombreDeFirmante.normalizar("Docente (demo)")).isEqualTo("DOCENTE (DEMO)");
    }
}
