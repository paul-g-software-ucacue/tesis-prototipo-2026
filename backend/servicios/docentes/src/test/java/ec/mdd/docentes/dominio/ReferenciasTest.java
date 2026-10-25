package ec.mdd.docentes.dominio;

import static ec.mdd.docentes.dominio.DocentesDePrueba.con;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La carrera base, las asignaturas afines y el campo amplio existen en Académico (FR-005), sin
 * dobles; y los 47 docentes de la carga 06 de prueba son válidos (SC-002).
 */
class ReferenciasTest {

    private final Referencias referencias = DocentesDePrueba.referencias();

    @Test
    void dadosDatosConReferenciasQueExisten_cuandoSeVerifican_entoncesPasan() {
        referencias.verificar(DocentesDePrueba.datos());

        assertThat(referencias.carrera("C1")).contains("DESARROLLO DE SOFTWARE");
        assertThat(referencias.asignatura("ZZ")).isEmpty();
        assertThat(referencias.campoAmplio("06"))
                .contains("Tecnologías de la información y la comunicación");
    }

    @Test
    void dadaUnaCarreraOUnaAsignaturaQueNoExisten_cuandoSeVerifican_entoncesDatoInvalido() {
        assertThatThrownBy(
                        () ->
                                referencias.verificar(
                                        con(
                                                v -> {
                                                    v.carrera = "NO-EXISTE";
                                                    return v;
                                                })))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carreraBase");
        assertThatThrownBy(
                        () ->
                                referencias.verificar(
                                        con(
                                                v -> {
                                                    v.ficha =
                                                            new FichaAcademica(
                                                                    v.ficha.tercerNivel(),
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    List.of("A1", "ZZ-1"));
                                                    return v;
                                                })))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("ficha.asignaturasAfines[1]");
    }

    @Test
    void dadoUnCampoAmplioFueraDeLos11_cuandoSeVerifica_entoncesDatoInvalido() {
        Referencias sinEl06 =
                new Referencias(
                        referencias.carreras(),
                        referencias.asignaturas(),
                        Map.of("05", "Ciencias"));

        assertThatThrownBy(() -> sinEl06.verificar(DocentesDePrueba.datos()))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("ficha.tercerNivel.campoAmplio");
    }

    @Test
    void dadaUnaCarreraEnCierre_cuandoSeVerifica_entoncesSirve() {
        // Las referencias traen las carreras en cualquier estado: una en cierre también está.
        Referencias conCarreraEnCierre =
                new Referencias(
                        Map.of("C9", new Referencias.Carrera("CARRERA EN CIERRE", "CC")),
                        Map.of("A1", new Referencias.Asignatura("PROGRAMACIÓN", "C9")),
                        DocentesDePrueba.CAMPOS_AMPLIOS);

        conCarreraEnCierre.verificar(
                con(
                        v -> {
                            v.carrera = "C9";
                            return v;
                        }));
    }

    @Test
    void
            dadaLaCarga06DePrueba_cuandoSeLeen_entoncesLos47SonValidosYCadaUnoSeEncuentraPorSuCedula() {
        List<Docente> docentes = DocentesDePrueba.cargados();

        assertThat(docentes).hasSize(47);
        docentes.forEach(docente -> referencias.verificar(docente.datos()));
        for (Docente docente : docentes) {
            BusquedaDeDocentes porCedula =
                    new BusquedaDeDocentes(docente.identificacion().numero(), null, null);
            assertThat(docentes.stream().filter(porCedula::aplicaA)).containsExactly(docente);
        }
        assertThat(docentes)
                .filteredOn(docente -> docente.estado() == EstadoLaboral.LICENCIA)
                .hasSize(1);
    }
}
