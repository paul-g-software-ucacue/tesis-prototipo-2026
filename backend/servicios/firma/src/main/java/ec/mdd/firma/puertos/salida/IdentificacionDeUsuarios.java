package ec.mdd.firma.puertos.salida;

import java.util.Optional;

/**
 * La identificación de la persona de una cuenta, de Usuarios (F14-H01, D-102). Firma la pide para
 * saber si quien pide es el docente de un cargo firmante. No viaja en el token, que llega al
 * navegador: es un dato personal (principio IX).
 */
@FunctionalInterface
public interface IdentificacionDeUsuarios {

    /**
     * @param usuario el nombre de usuario
     * @return su identificación, o vacío si la cuenta no existe o no la tiene
     */
    Optional<String> de(String usuario);
}
