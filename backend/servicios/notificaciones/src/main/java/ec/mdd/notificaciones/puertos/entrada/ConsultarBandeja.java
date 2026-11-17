package ec.mdd.notificaciones.puertos.entrada;

/**
 * Caso de uso: la bandeja del usuario de la sesión (F16-H01, US2). Cada usuario ve solo la suya: el
 * usuario sale del token, nunca de la petición (D-108).
 */
public interface ConsultarBandeja {

    /**
     * @param usuario el usuario de la sesión
     * @param limite cuántas notificaciones, de 1 a 100
     * @return su bandeja
     */
    BandejaDeUsuario bandeja(String usuario, int limite);

    /**
     * @param usuario el usuario de la sesión
     * @return cuántas tiene sin leer, para la campana
     */
    int sinLeer(String usuario);
}
