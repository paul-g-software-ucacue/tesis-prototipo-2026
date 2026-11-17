package ec.mdd.roles.puertos.entrada;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: a quién toca un mensaje de Notificaciones, en el mismo proceso {@code identidad}
 * (F16-H01, D-108). Roles es dueño de las asignaciones, de las designaciones y de la carrera de
 * cada coordinador. Solo lee: no cambia nada ni deja entrada en el registro de cambios.
 */
@FunctionalInterface
public interface ConsultarDestinatarios {

    /**
     * Los usuarios a quienes toca el mensaje, sin filtrar los inactivos: eso es de Usuarios.
     *
     * <ul>
     *   <li>RESPONSABLE_CONSOLIDACION: quienes tienen el rol asignado y, además, el designado del
     *       periodo, que lo tiene por la designación (ADR-003);
     *   <li>COORDINADOR_CARRERA con carrera: los coordinadores cuya carrera indicada es esa (D-64);
     *   <li>cualquier otro rol, o COORDINADOR_CARRERA sin carrera: todos los que tienen el rol.
     * </ul>
     *
     * @param rol uno de los diez roles; uno desconocido da una lista vacía
     * @param periodo el código del periodo, para el Responsable de consolidación
     * @param carrera con COORDINADOR_CARRERA, la carrera de la devolución
     * @return los destinatarios, ordenados por usuario
     */
    List<Destinatario> destinatarios(String rol, String periodo, Optional<String> carrera);
}
