package ec.mdd.notificaciones.puertos.salida;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import java.util.List;
import java.util.Optional;

/**
 * Los usuarios de un destino, que son de Roles y de Usuarios en el mismo proceso {@code identidad}
 * (F16-H01, D-108). Notificaciones solo usa sus interfaces publicadas (principio V).
 */
public interface Destinatarios {

    /**
     * @param destino el rol y, en una devolución, la carrera; o una persona, por su usuario o su
     *     identificación (D-110)
     * @param periodo el periodo, para el Responsable de consolidación designado
     * @return los usuarios activos del destino, ordenados por usuario; vacío si una persona no
     *     tiene cuenta activa
     */
    List<Destinatario> de(DestinoDelMensaje destino, String periodo);

    /**
     * @param usuario el nombre de usuario
     * @return su nombre para mostrar; vacío si no tiene o no existe
     */
    Optional<String> nombre(String usuario);
}
