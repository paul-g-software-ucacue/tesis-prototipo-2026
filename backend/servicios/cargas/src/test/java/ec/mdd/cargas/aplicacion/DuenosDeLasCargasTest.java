package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Cada carga tiene exactamente un dueño (MD-13, ADR-022), con Mockito. */
class DuenosDeLasCargasTest {

    private static AplicadorDeCargas dueno(String... cargas) {
        AplicadorDeCargas aplicador = mock(AplicadorDeCargas.class);
        when(aplicador.cargas()).thenReturn(Set.of(cargas));
        return aplicador;
    }

    @Test
    void cadaCargaConSuDueno() {
        AplicadorDeCargas academico = dueno("carreras", "estructura_organica");
        AplicadorDeCargas docentes = dueno("docentes");

        DuenosDeLasCargas duenos =
                new DuenosDeLasCargas(
                        List.of(academico, docentes),
                        List.of("estructura_organica", "carreras", "docentes"));

        assertThat(duenos.de("carreras")).isSameAs(academico);
        assertThat(duenos.de("docentes")).isSameAs(docentes);
    }

    @Test
    void unaCargaSinDueno_conDosOQueNoEstaEnElDiccionario_noArranca() {
        assertThatThrownBy(
                        () ->
                                new DuenosDeLasCargas(
                                        List.of(dueno("carreras")),
                                        List.of("carreras", "docentes")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("docentes");
        assertThatThrownBy(
                        () ->
                                new DuenosDeLasCargas(
                                        List.of(dueno("carreras"), dueno("carreras")),
                                        List.of("carreras")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dos dueños");
        assertThatThrownBy(
                        () ->
                                new DuenosDeLasCargas(
                                        List.of(dueno("carreras", "comisiones")),
                                        List.of("carreras")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("comisiones");
    }
}
