package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.EstructuraDePrueba.GESTIONES;
import static ec.mdd.academico.dominio.EstructuraDePrueba.UNIDADES;
import static ec.mdd.academico.dominio.EstructuraDePrueba.comision;
import static ec.mdd.academico.dominio.EstructuraDePrueba.completa;
import static ec.mdd.academico.dominio.EstructuraDePrueba.gestion;
import static ec.mdd.academico.dominio.EstructuraDePrueba.nueva;
import static ec.mdd.academico.dominio.UnidadOrganicaTest.invalido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Las reglas que miran toda la estructura (RF-12, RN-10, FR-003, research §3): el padre, los
 * ciclos, la sede matriz única, las gestiones del catálogo y los dependientes, sin dobles.
 */
class EstructuraOrganicaTest {

    @Test
    void dadaLaCarga01DePrueba_cuandoSeRegistraEnOrden_entoncesEntranLas24() {
        EstructuraOrganica estructura = completa();

        assertThat(estructura.unidades()).hasSize(24).containsAll(UNIDADES);
        assertThat(estructura.unidad("RECT"))
                .map(UnidadOrganica::gestionAutorizada)
                .hasValue("G01");
    }

    @Test
    void dadoUnPadreQueNoExiste_cuandoSeRegistra_entoncesDatoInvalidoEnPadre() {
        invalido(
                () -> completa().registrar(gestion("G-IDI", "Idiomas", "NADA", "G08")),
                "padre",
                "La unidad NADA no existe");
    }

    @Test
    void dadoUnPadreInactivo_cuandoSeRegistra_entoncesDatoInvalidoEnPadre() {
        EstructuraOrganica estructura = inactivar(completa(), "COM-DIST");

        invalido(
                () -> estructura.registrar(gestion("G-IDI", "Idiomas", "COM-DIST", "G08")),
                "padre",
                "La unidad COM-DIST está inactiva");
    }

    @Test
    void dadaUnaGestionFueraDelCatalogo_cuandoSeRegistra_entoncesDatoInvalidoEnGestion() {
        for (String codigo : new String[] {"G99", "G00", "g07", "X"}) {
            invalido(
                    () -> completa().registrar(gestion("G-OTRA", "Otra", "VRA", codigo)),
                    "gestionAutorizada",
                    codigo + " no está entre las 21 gestiones autorizadas de POL 6");
        }
    }

    @Test
    void dadaOtraUnidadConLaMismaGestion_cuandoSeRegistra_entoncesSeAcepta() {
        UnidadOrganica otra = gestion("G-INV2", "Investigación en sede", "MATRIZ", "G07");

        assertThat(completa().registrar(otra)).isEqualTo(otra);
    }

    @Test
    void dadaUnaSedeMatrizRegistrada_cuandoSeRegistraOtra_entoncesSedeMatrizExistente() {
        UnidadOrganica otra =
                nueva("MATRIZ2", "Otra matriz", TipoDeUnidad.SEDE_MATRIZ, null, null, null);

        assertThatThrownBy(() -> completa().registrar(otra))
                .isInstanceOf(SedeMatrizExistente.class);
    }

    @Test
    void dadaUnaEstructuraVacia_cuandoSeRegistraAlgoQueNoEsLaSedeMatriz_entoncesDatoInvalido() {
        EstructuraOrganica vacia = new EstructuraOrganica(List.of(), GESTIONES);

        invalido(
                () -> vacia.registrar(gestion("G-INV", "Investigación", "MATRIZ", "G07")),
                "padre",
                "La unidad MATRIZ no existe");
    }

    @Test
    void dadoUnPadreQueDependeDeLaUnidad_cuandoSeCorrige_entoncesDatoInvalidoPorElCiclo() {
        EstructuraOrganica estructura = completa();
        UnidadOrganica vra = estructura.unidad("VRA").orElseThrow();

        invalido(
                () -> estructura.corregir(vra, vra.nombre(), "CC-DS", "G02", null),
                "padre",
                "La unidad no puede depender de CC-DS, porque CC-DS depende de ella");
        invalido(
                () -> estructura.corregir(vra, vra.nombre(), "VRA", "G02", null),
                "padre",
                "La unidad no puede depender de sí misma");
    }

    @Test
    void dadoUnPadreValido_cuandoSeCorrige_entoncesDevuelveLaCorreccion() {
        EstructuraOrganica estructura = completa();
        UnidadOrganica bib = estructura.unidad("G-BIB").orElseThrow();

        UnidadOrganica.Correccion correccion =
                estructura.corregir(bib, bib.nombre(), "RECT", "G17", null);

        assertThat(correccion.unidad().padre()).isEqualTo("RECT");
        assertThat(correccion.cambios()).containsExactly(new CambioDeValor("padre", "VRA", "RECT"));
    }

    @Test
    void dadaUnaGestionFueraDelCatalogo_cuandoSeCorrige_entoncesDatoInvalido() {
        EstructuraOrganica estructura = completa();
        UnidadOrganica bib = estructura.unidad("G-BIB").orElseThrow();

        invalido(
                () -> estructura.corregir(bib, bib.nombre(), "VRA", "G22", null),
                "gestionAutorizada",
                "G22 no está entre las 21 gestiones autorizadas de POL 6");
    }

    @Test
    void dadaUnaUnidadConDependientesActivos_cuandoSeInactiva_entoncesUnidadConDependientes() {
        EstructuraOrganica estructura = completa();
        UnidadOrganica cal = estructura.unidad("G-CAL").orElseThrow();

        assertThatThrownBy(() -> estructura.cambiarEstado(cal, false))
                .isInstanceOf(UnidadConDependientes.class);
    }

    @Test
    void dadosSusDependientesInactivos_cuandoSeInactiva_entoncesQuedaInactiva() {
        EstructuraOrganica estructura = inactivar(completa(), "COM-EVAL");
        UnidadOrganica cal = estructura.unidad("G-CAL").orElseThrow();

        assertThat(estructura.cambiarEstado(cal, false).activa()).isFalse();
    }

    @Test
    void dadoElPadreInactivo_cuandoSeReactiva_entoncesDatoInvalidoEnPadre() {
        EstructuraOrganica estructura = inactivar(inactivar(completa(), "COM-EVAL"), "G-CAL");
        UnidadOrganica evaluacion = estructura.unidad("COM-EVAL").orElseThrow();

        invalido(
                () -> estructura.cambiarEstado(evaluacion, true),
                "padre",
                "La unidad G-CAL de la que depende está inactiva: reactívala antes");
        assertThat(
                        inactivar(completa(), "COM-EVAL")
                                .cambiarEstado(
                                        comision(
                                                        "COM-EVAL",
                                                        "Comisión de Evaluación Interna",
                                                        "G-CAL",
                                                        true)
                                                .conEstado(false),
                                        true)
                                .activa())
                .isTrue();
    }

    @Test
    void dadoElMismoEstado_cuandoCambia_entoncesDevuelveLaMismaUnidad() {
        EstructuraOrganica estructura = completa();
        UnidadOrganica cal = estructura.unidad("G-CAL").orElseThrow();

        assertThat(estructura.cambiarEstado(cal, true)).isSameAs(cal);
    }

    @Test
    void dadaUnaUnidadInactiva_cuandoSeCorrigeSinCambiarSuPadreInactivo_entoncesSeAcepta() {
        EstructuraOrganica estructura = inactivar(inactivar(completa(), "COM-EVAL"), "G-CAL");
        UnidadOrganica evaluacion = estructura.unidad("COM-EVAL").orElseThrow();

        assertThat(
                        estructura
                                .corregir(evaluacion, "Comisión de Evaluación", "G-CAL", null, true)
                                .unidad()
                                .nombre())
                .isEqualTo("Comisión de Evaluación");
    }

    private static EstructuraOrganica inactivar(EstructuraOrganica estructura, String codigo) {
        UnidadOrganica unidad = estructura.unidad(codigo).orElseThrow();
        return estructura.con(estructura.cambiarEstado(unidad, false));
    }
}
