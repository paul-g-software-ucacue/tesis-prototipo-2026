package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;

/**
 * Caso de uso: mantener la estructura orgánica (FR-002, F03-H01). Refleja las operaciones {@code
 * registrarUnidad}, {@code corregirUnidad} y {@code cambiarEstadoUnidad} del contrato de Académico.
 * Cada cambio queda en el registro ({@code UNIDAD_REGISTRADA}, {@code UNIDAD_CORREGIDA}, {@code
 * UNIDAD_INACTIVADA} o {@code UNIDAD_REACTIVADA}); ninguna unidad se borra.
 *
 * <p>Los datos de la unidad no cumplen su regla: {@link ec.mdd.academico.dominio.DatoInvalido}.
 */
public interface MantenerEstructura {

    /**
     * @param padre el código de la unidad de la que depende; {@code null} en la sede matriz
     * @param gestionAutorizada su código, o {@code null}
     * @param comisionPermanente solo en una comisión; si no, {@code null}
     * @param quien el usuario de la sesión
     * @return la unidad registrada, activa
     * @throws UnidadRepetida si ya hay una unidad con ese código
     * @throws ec.mdd.academico.dominio.SedeMatrizExistente si es una segunda sede matriz
     */
    UnidadOrganica registrar(
            String codigo,
            String nombre,
            TipoDeUnidad tipo,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien);

    /**
     * Corrige el nombre, el padre y los datos de su tipo; el código y el tipo no cambian.
     *
     * @return la unidad corregida
     * @throws UnidadNoEncontrada si no hay una unidad con ese código
     */
    UnidadOrganica corregir(
            String codigo,
            String nombre,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien);

    /**
     * Inactiva o reactiva una unidad; pedir el estado que ya tiene no cambia nada.
     *
     * @return la unidad en su estado nuevo
     * @throws UnidadNoEncontrada si no hay una unidad con ese código
     * @throws ec.mdd.academico.dominio.UnidadConDependientes si se inactiva con unidades activas
     *     debajo
     */
    UnidadOrganica cambiarEstado(String codigo, boolean activa, String quien);
}
