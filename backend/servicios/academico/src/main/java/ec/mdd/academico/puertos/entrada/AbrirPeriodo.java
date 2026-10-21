package ec.mdd.academico.puertos.entrada;

/**
 * Caso de uso: abrir un periodo, asociándole su versión de normativa (RF-19, F02-H01). Refleja la
 * operación {@code abrirPeriodo} del contrato de Académico. Normativa guarda la asociación, en su
 * propia transacción, y desde entonces la versión no cambia. Queda en el registro como {@code
 * PERIODO_ABIERTO} (research §3).
 */
public interface AbrirPeriodo {

    /**
     * @param quien el usuario de la sesión
     * @return el periodo con su versión; si ya la tenía, sin cambios
     * @throws PeriodoNoEncontrado si no existe
     * @throws ec.mdd.academico.dominio.PeriodoCerrado si está cerrado
     * @throws PeriodoConOtraVersion si ya tiene otra versión
     * @throws VersionInexistente si la versión no existe
     */
    PeriodoConsultado abrir(String codigo, int version, String quien);
}
