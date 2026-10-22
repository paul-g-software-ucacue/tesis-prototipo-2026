package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.EstructuraDePrueba.comision;
import static ec.mdd.academico.dominio.EstructuraDePrueba.gestion;
import static ec.mdd.academico.dominio.EstructuraDePrueba.nueva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** Una unidad de la estructura y las reglas de su tipo (RF-12, C-12, research §3), sin dobles. */
class UnidadOrganicaTest {

    @Test
    void dadoDatosValidos_cuandoSeRegistra_entoncesQuedaActivaYSinEspaciosEnElNombre() {
        UnidadOrganica unidad = gestion("G-INV", "  Gestión de Investigación ", "VRA", "G07");

        assertThat(unidad.activa()).isTrue();
        assertThat(unidad.nombre()).isEqualTo("Gestión de Investigación");
        assertThat(unidad.gestionAutorizada()).isEqualTo("G07");
    }

    @Test
    void dadoUnCodigoConOtroFormato_cuandoSeRegistra_entoncesDatoInvalidoEnCodigo() {
        for (String codigo : new String[] {"g-inv", "", "G INV", "G/INV", "X".repeat(21)}) {
            invalido(
                    () -> gestion(codigo, "Gestión", "VRA", "G07"),
                    "codigo",
                    "El código solo admite mayúsculas, números, guion y guion bajo, hasta 20"
                            + " caracteres");
        }
        assertThat(gestion("X".repeat(20), "Gestión", "VRA", "G07").codigo()).hasSize(20);
    }

    @Test
    void dadoUnNombreVacioOLargo_cuandoSeRegistra_entoncesDatoInvalidoEnNombre() {
        invalido(
                () -> gestion("G-INV", "  ", "VRA", "G07"),
                "nombre",
                "El nombre no puede estar en blanco");
        invalido(
                () -> gestion("G-INV", "n".repeat(151), "VRA", "G07"),
                "nombre",
                "El nombre no puede tener más de 150 caracteres");
        assertThat(gestion("G-INV", "n".repeat(150), "VRA", "G07").nombre()).hasSize(150);
    }

    @Test
    void
            dadaLaSedeMatrizConPadreOOtraUnidadSinPadre_cuandoSeRegistra_entoncesDatoInvalidoEnPadre() {
        invalido(
                () -> nueva("MATRIZ", "Sede matriz", TipoDeUnidad.SEDE_MATRIZ, "OTRA", null, null),
                "padre",
                "La sede matriz no depende de otra unidad");
        invalido(
                () -> nueva("SEDE-N", "Sede norte", TipoDeUnidad.SEDE, null, null, null),
                "padre",
                "Elige la unidad de la que depende");
    }

    @Test
    void dadaUnaGestionSinGestionAutorizada_cuandoSeRegistra_entoncesDatoInvalido() {
        invalido(
                () -> gestion("G-INV", "Gestión", "VRA", null),
                "gestionAutorizada",
                "Una gestión tiene que representar una de las 21 gestiones autorizadas");
    }

    @Test
    void dadaUnaComisionConGestionAutorizada_cuandoSeRegistra_entoncesDatoInvalido() {
        invalido(
                () -> nueva("COM-X", "Comisión", TipoDeUnidad.COMISION, "VRA", "G07", true),
                "gestionAutorizada",
                "Una comisión es un dato informativo: no representa una gestión autorizada");
    }

    @Test
    void dadoUnOrganoDeGobierno_cuandoSeRegistraConOSinGestionAutorizada_entoncesSeAcepta() {
        assertThat(nueva("RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null))
                .extracting(UnidadOrganica::gestionAutorizada)
                .isEqualTo("G01");
        assertThat(nueva("OCS", "Consejo", TipoDeUnidad.GOBIERNO, "MATRIZ", null, null))
                .extracting(UnidadOrganica::gestionAutorizada)
                .isNull();
    }

    @Test
    void dadaUnaComisionSinPermanente_cuandoSeRegistra_entoncesDatoInvalido() {
        invalido(
                () -> nueva("COM-X", "Comisión", TipoDeUnidad.COMISION, "VRA", null, null),
                "comisionPermanente",
                "Indica si la comisión es permanente");
        assertThat(comision("COM-X", "Comisión", "VRA", false).comisionPermanente()).isFalse();
    }

    @Test
    void dadoOtroTipoConPermanente_cuandoSeRegistra_entoncesDatoInvalido() {
        invalido(
                () -> nueva("G-INV", "Gestión", TipoDeUnidad.GESTION, "VRA", "G07", true),
                "comisionPermanente",
                "Solo una comisión indica si es permanente");
    }

    @Test
    void dadaUnaUnidad_cuandoSeCorrige_entoncesCambiaLoPedidoYNoElCodigoNiElTipo() {
        UnidadOrganica unidad = gestion("G-INV", "Gestión de Investigación", "VRA", "G07");

        UnidadOrganica.Correccion correccion =
                unidad.corregida("Gestión de Investigación e Innovación", "RECT", "G07", null);

        assertThat(correccion.unidad())
                .isEqualTo(
                        gestion("G-INV", "Gestión de Investigación e Innovación", "RECT", "G07"));
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor(
                                "nombre",
                                "Gestión de Investigación",
                                "Gestión de Investigación e Innovación"),
                        new CambioDeValor("padre", "VRA", "RECT"));
    }

    @Test
    void dadaUnaComision_cuandoSeCorrigeElPermanenteYLaGestion_entoncesSeRegistraElCambioDeVacio() {
        UnidadOrganica gobierno =
                nueva("OCS", "Consejo", TipoDeUnidad.GOBIERNO, "MATRIZ", null, null);

        assertThat(gobierno.corregida("Consejo", "MATRIZ", "G01", null).cambios())
                .containsExactly(new CambioDeValor("gestionAutorizada", "", "G01"));
        assertThat(
                        comision("COM-X", "Comisión", "VRA", true)
                                .corregida("Comisión", "VRA", null, false)
                                .cambios())
                .containsExactly(new CambioDeValor("comisionPermanente", "true", "false"));
    }

    @Test
    void dadosLosMismosDatos_cuandoSeCorrige_entoncesNoHayCambios() {
        UnidadOrganica unidad = gestion("G-INV", "Gestión", "VRA", "G07");

        assertThat(unidad.corregida("Gestión", "VRA", "G07", null).cambios()).isEmpty();
    }

    @Test
    void dadaUnaCorreccionQueRompeLaReglaDelTipo_cuandoSeCorrige_entoncesDatoInvalido() {
        invalido(
                () ->
                        gestion("G-INV", "Gestión", "VRA", "G07")
                                .corregida("Gestión", "VRA", null, null),
                "gestionAutorizada",
                "Una gestión tiene que representar una de las 21 gestiones autorizadas");
    }

    @Test
    void dadaUnaUnidadActiva_cuandoCambiaDeEstado_entoncesSoloCambiaElEstado() {
        UnidadOrganica unidad = gestion("G-INV", "Gestión", "VRA", "G07");

        UnidadOrganica inactiva = unidad.conEstado(false);

        assertThat(inactiva.activa()).isFalse();
        assertThat(inactiva.conEstado(true)).isEqualTo(unidad);
    }

    static void invalido(ThrowingCallable accion, String campo, String motivo) {
        assertThatThrownBy(accion)
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo", "motivo")
                .containsExactly(campo, motivo);
    }
}
