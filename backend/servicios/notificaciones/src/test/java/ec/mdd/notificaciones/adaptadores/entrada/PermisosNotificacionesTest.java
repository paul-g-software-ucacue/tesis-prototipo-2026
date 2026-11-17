package ec.mdd.notificaciones.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.pruebas.VerificadorDePermisos;
import org.junit.jupiter.api.Test;

/**
 * La prueba de autorización por operación de Notificaciones (RNF-SEG-03): ninguna operación del
 * contrato declara {@code x-mdd-permiso}, porque el límite es el destinatario, validado en el
 * servidor por el usuario del token (D-108). Que las de la bandeja piden sesión y que la de otro
 * usuario da 404 lo comprueba {@link BandejaContratoTest}; que el receptor solo admite un token de
 * proceso, {@link EventosNotificacionesContratoTest}.
 */
class PermisosNotificacionesTest {

    @Test
    void
            dadoElContratoDeNotificaciones_cuandoSeBuscanPermisos_entoncesNingunaOperacionLosDeclara() {
        assertThat(VerificadorDePermisos.para("notificaciones").operaciones()).isEmpty();
    }
}
