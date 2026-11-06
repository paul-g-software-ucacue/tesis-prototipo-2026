package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.CambioDeValor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El detalle de un cambio de valor para el registro, igual en los periodos y en la estructura, y
 * los valores anterior y nuevo de la bitácora (F17-H01).
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

    /** Un solo campo, que puede no tener valor (como la versión de un periodo sin abrir). */
    static Map<String, Object> valor(String campo, Object valor) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(campo, valor);
        return valores;
    }
}
