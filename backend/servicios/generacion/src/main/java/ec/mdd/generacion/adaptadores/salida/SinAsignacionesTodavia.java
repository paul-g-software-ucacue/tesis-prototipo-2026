package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.puertos.salida.HorasAsignadas;
import java.util.Map;

/**
 * Las asignaciones todavía no existen: nadie tiene horas asignadas. F06-H03 reemplaza este
 * adaptador por el que lee las propuestas, sin cambiar la sugerencia ni su pantalla (FR-009).
 */
public final class SinAsignacionesTodavia implements HorasAsignadas {

    @Override
    public Map<String, HorasDelDocente> delPeriodo(String periodo, String carrera) {
        return Map.of();
    }
}
