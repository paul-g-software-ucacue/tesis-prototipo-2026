package ec.mdd.roles.puertos.entrada;

import ec.mdd.roles.dominio.Designacion;

/**
 * Caso de uso: designar al Responsable de consolidación de un periodo, o reemplazarlo (D-13,
 * ADR-003, F05-H03). Refleja la operación {@code designarResponsable} del contrato de Roles. Queda
 * en el registro como {@code DESIGNAR_RESPONSABLE}, con el anterior y el nuevo.
 */
public interface DesignarResponsable {

    /**
     * @param periodo el código del periodo
     * @param usuario a quién se designa
     * @param quien el usuario de la sesión
     * @return la designación del periodo; con el mismo usuario, la que ya tenía, sin cambios
     * @throws PeriodoDesconocido si Roles todavía no conoce el periodo
     * @throws ec.mdd.roles.dominio.PeriodoCerrado si el periodo está cerrado
     * @throws ec.mdd.roles.dominio.DesignacionInvalida si el usuario no se puede designar
     */
    Designacion designar(String periodo, String usuario, String quien);
}
