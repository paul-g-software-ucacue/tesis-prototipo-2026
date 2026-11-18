package ec.mdd.notificaciones.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import ec.mdd.roles.puertos.entrada.ConsultarDestinatarios;
import ec.mdd.usuarios.puertos.entrada.ConsultarCuentaPorIdentificacion;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Los destinatarios de cada destino, con Roles y Usuarios (D-108, D-110): por rol como en F16-H01,
 * y la persona por su usuario o su identificación, solo si su cuenta está activa.
 */
class DestinatariosDesdeIdentidadTest {

    private static final List<ResumenDeUsuario> CUENTAS =
            List.of(
                    new ResumenDeUsuario("docente.demo", "Docente (demo)", true),
                    new ResumenDeUsuario("inactivo.demo", "Inactivo", false),
                    new ResumenDeUsuario("vicerrectorado.demo", "Vicerrectorado", true));

    private static final Map<String, ResumenDeUsuario> POR_IDENTIFICACION =
            Map.of(
                    "0109990028",
                    new ResumenDeUsuario("vicerrectorado.demo", "Vicerrectorado", true),
                    "0109990036",
                    new ResumenDeUsuario("inactivo.demo", "Inactivo", false));

    private final ConsultarDestinatarios roles =
            (rol, periodo, carrera) ->
                    rol.equals("VICERRECTOR")
                            ? List.of(
                                    new ec.mdd.roles.puertos.entrada.Destinatario(
                                            "vicerrectorado.demo", Optional.empty()))
                            : List.of();
    private final ConsultarCuentaPorIdentificacion cuentas =
            identificacion -> Optional.ofNullable(POR_IDENTIFICACION.get(identificacion));
    private final DestinatariosDesdeIdentidad destinatarios =
            new DestinatariosDesdeIdentidad(roles, () -> CUENTAS, cuentas);

    @Test
    void dadoUnRol_cuandoSeResuelve_entoncesComoEnF16H01() {
        assertThat(
                        destinatarios.de(
                                new DestinoDelMensaje.PorRol("VICERRECTOR", Optional.empty()),
                                "2026-I"))
                .containsExactly(new Destinatario("vicerrectorado.demo", Optional.empty()));
    }

    @Test
    void dadoUnUsuario_cuandoSeResuelve_entoncesSoloSiExisteYEstaActivo() {
        assertThat(destinatarios.de(new DestinoDelMensaje.PorUsuario("docente.demo"), "2026-I"))
                .containsExactly(new Destinatario("docente.demo", Optional.empty()));
        assertThat(destinatarios.de(new DestinoDelMensaje.PorUsuario("inactivo.demo"), "2026-I"))
                .isEmpty();
        assertThat(destinatarios.de(new DestinoDelMensaje.PorUsuario("nadie.demo"), "2026-I"))
                .isEmpty();
    }

    @Test
    void dadaUnaIdentificacion_cuandoSeResuelve_entoncesSuCuentaSoloSiEstaActiva() {
        assertThat(
                        destinatarios.de(
                                new DestinoDelMensaje.PorIdentificacion("0109990028"), "2026-I"))
                .containsExactly(new Destinatario("vicerrectorado.demo", Optional.empty()));
        assertThat(
                        destinatarios.de(
                                new DestinoDelMensaje.PorIdentificacion("0109990036"), "2026-I"))
                .isEmpty();
        assertThat(
                        destinatarios.de(
                                new DestinoDelMensaje.PorIdentificacion("0999999999"), "2026-I"))
                .isEmpty();
    }
}
