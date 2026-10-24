package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.CarreraTest.invalido;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Una malla con su código de versión y su estado (aclaración 2 de F03-H02), sin dobles. */
class MallaTest {

    @Test
    void dadosDatosValidos_cuandoSeRegistra_entoncesQuedaVigenteYSePuedeDerogar() {
        Malla malla = Malla.nueva("DS-AC-2025", "550613A01-P-0301");

        assertThat(malla.estado()).isEqualTo(EstadoDeMalla.VIGENTE);
        assertThat(malla.conEstado(EstadoDeMalla.DEROGADA).estado())
                .isEqualTo(EstadoDeMalla.DEROGADA);
    }

    @Test
    void dadoUnCodigoConOtraFormaOSinCarrera_cuandoSeRegistra_entoncesSuCampo() {
        invalido(() -> Malla.nueva("ds-2019", "C1"), "codigo");
        invalido(() -> Malla.nueva("X".repeat(21), "C1"), "codigo");
        invalido(() -> Malla.nueva("DS-2019", null), "carrera");
    }
}
