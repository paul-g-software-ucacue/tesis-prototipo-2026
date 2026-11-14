package ec.mdd.usuarios.puertos.entrada;

/**
 * La identificación de la persona de una cuenta, para otro proceso (F14-H01, D-102): Firma la pide
 * para saber si quien pide es el docente de un cargo firmante.
 */
public interface ConsultarIdentificacion {

    /**
     * @param usuario el nombre de usuario, sin distinguir mayúsculas
     * @return su identificación
     * @throws ec.mdd.usuarios.aplicacion.SinIdentificacion si la cuenta no existe o no la tiene
     */
    String de(String usuario);
}
