package ec.mdd.firma.puertos.salida;

import java.util.Map;
import java.util.Set;

/**
 * El nombre de quien ocupa cada rol de la cadena en el periodo (F13-H02, research §6): el Rector y
 * el Vicerrector académico, de sus cargos vigentes en Datos; el Responsable de consolidación, de su
 * designación en Roles. Llama a otros procesos: no se usa dentro de una transacción (D-83).
 */
@FunctionalInterface
public interface FirmantesDelPeriodo {

    /**
     * @param periodo el código del periodo
     * @param roles los roles de la cadena
     * @return el nombre por rol; un rol sin titular no está
     */
    Map<String, String> nombres(String periodo, Set<String> roles);
}
