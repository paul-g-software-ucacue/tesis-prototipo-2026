package ec.mdd.roles.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.roles.dominio.NombreRepetido;
import ec.mdd.roles.dominio.RolAsignado;
import ec.mdd.roles.dominio.RolPredefinido;
import ec.mdd.roles.dominio.SinAdministrador;
import org.junit.jupiter.api.Test;

/** Los errores de Roles coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresRolesTest {

    @Test
    void dadoElCatalogoDeRoles_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresRoles());
    }

    @Test
    void dadoLasReglasDeLaAdministracion_cuandoSeBuscanSusEntradas_entoncesSonLosCuatro409() {
        assertThat(new ErroresRoles().entradas())
                .extracting(EntradaCatalogo::excepcion, entrada -> entrada.definicion().codigo())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                NombreRepetido.class, "ROLES_NOMBRE_REPETIDO"),
                        org.assertj.core.groups.Tuple.tuple(
                                RolPredefinido.class, "ROLES_ROL_PREDEFINIDO"),
                        org.assertj.core.groups.Tuple.tuple(
                                RolAsignado.class, "ROLES_ROL_ASIGNADO"),
                        org.assertj.core.groups.Tuple.tuple(
                                SinAdministrador.class, "ROLES_SIN_ADMINISTRADOR"));
        assertThat(new ErroresRoles().entradas())
                .allMatch(entrada -> entrada.definicion().estado() == 409);
    }
}
