package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.CambioDeValor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El detalle de un cambio de valor para el registro, como en Académico, y los valores anterior y
 * nuevo de la bitácora (F17-H01).
 */
final class Detalles {

    private Detalles() {}

    static Map<String, Object> de(CambioDeValor cambio) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("campo", cambio.campo());
        detalle.put("anterior", cambio.anterior());
        detalle.put("nuevo", cambio.nuevo());
        return detalle;
    }

    /** Cada campo que cambió con su valor anterior; {@code null} si no tenía. */
    static Map<String, Object> anteriores(List<CambioDeValor> cambios) {
        Map<String, Object> valores = new LinkedHashMap<>();
        cambios.forEach(cambio -> valores.put(cambio.campo(), cambio.anterior()));
        return valores;
    }

    /** Cada campo que cambió con su valor nuevo; {@code null} si quedó sin valor. */
    static Map<String, Object> nuevos(List<CambioDeValor> cambios) {
        Map<String, Object> valores = new LinkedHashMap<>();
        cambios.forEach(cambio -> valores.put(cambio.campo(), cambio.nuevo()));
        return valores;
    }
}
