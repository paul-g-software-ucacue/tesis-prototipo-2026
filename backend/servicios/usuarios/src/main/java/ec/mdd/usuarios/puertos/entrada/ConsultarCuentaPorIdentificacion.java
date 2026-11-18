package ec.mdd.usuarios.puertos.entrada;

import java.util.Optional;

/**
 * La cuenta de una persona por su identificación, para Notificaciones (F16-H02, D-110): el firmante
 * esperado de un cargo se conoce por su identificación (D-102), y la notificación de firma
 * pendiente va a su cuenta. La identificación es única entre las cuentas.
 */
public interface ConsultarCuentaPorIdentificacion {

    /**
     * @param identificacion la cédula o el pasaporte, con el formato de Docentes
     * @return la cuenta con esa identificación, activa o no; vacío si no hay ninguna. No lleva la
     *     identificación, la contraseña ni su hash
     */
    Optional<ResumenDeUsuario> de(String identificacion);
}
