package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;
import java.time.LocalDate;

/**
 * Registrar la resolución del OCS y aprobar el distributivo (F15-H01, RF-37, D-107): la Secretaría
 * general académica, con el distributivo en EN_OCS. Registrar es aprobar.
 */
@FunctionalInterface
public interface RegistrarResolucion {

    /**
     * @param periodo el código del periodo
     * @param numero el número de la resolución
     * @param fecha su fecha
     * @param nombre el nombre del archivo recibido; puede traer una ruta o faltar
     * @param archivo los bytes del PDF
     * @param quien quien registra
     * @return el estado del flujo, ya en APROBADO
     * @throws PeriodoCerrado si el periodo está cerrado
     * @throws SinDistributivo si el periodo no tiene distributivo
     */
    FlujoDelDistributivo registrar(
            String periodo,
            String numero,
            LocalDate fecha,
            String nombre,
            byte[] archivo,
            Solicitante quien);
}
