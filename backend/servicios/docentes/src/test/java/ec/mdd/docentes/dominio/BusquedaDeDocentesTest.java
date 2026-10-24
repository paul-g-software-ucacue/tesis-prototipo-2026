package ec.mdd.docentes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Buscar docentes por nombre o identificación, carrera y estado (FR-007), sin dobles. */
class BusquedaDeDocentesTest {

    private final Docente peralta = DocentesDePrueba.docente();

    @Test
    void dadoUnTextoSinMayusculasNiTildes_cuandoSeBusca_entoncesEncuentraElNombre() {
        for (String texto :
                new String[] {"peralta", "PERALTA", "Peraltá", "maldonado juan", " juan "}) {
            assertThat(new BusquedaDeDocentes(texto, null, null).aplicaA(peralta))
                    .as(texto)
                    .isTrue();
        }
        assertThat(new BusquedaDeDocentes("lopez", null, null).aplicaA(peralta)).isFalse();
    }

    @Test
    void dadoElInicioDeLaIdentificacion_cuandoSeBusca_entoncesLoEncuentra() {
        assertThat(new BusquedaDeDocentes("010999", null, null).aplicaA(peralta)).isTrue();
        assertThat(new BusquedaDeDocentes("999001", null, null).aplicaA(peralta)).isFalse();
    }

    @Test
    void dadosCarreraYEstado_cuandoSeBusca_entoncesFiltra() {
        assertThat(new BusquedaDeDocentes(null, "C1", EstadoLaboral.ACTIVO).aplicaA(peralta))
                .isTrue();
        assertThat(new BusquedaDeDocentes(null, "C2", null).aplicaA(peralta)).isFalse();
        assertThat(new BusquedaDeDocentes(null, null, EstadoLaboral.INACTIVO).aplicaA(peralta))
                .isFalse();
    }

    @Test
    void dadosCriteriosVacios_cuandoSeBusca_entoncesTraeTodos() {
        assertThat(BusquedaDeDocentes.todos().aplicaA(peralta)).isTrue();
        assertThat(new BusquedaDeDocentes(" ", "", null).aplicaA(peralta)).isTrue();
    }
}
