package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.CambioDeValor;
import java.util.LinkedHashMap;
import java.util.Map;

/** El detalle de un cambio de valor para el registro, como en Académico. */
final class Detalles {

    private Detalles() {}

    static Map<String, Object> de(CambioDeValor cambio) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("campo", cambio.campo());
        detalle.put("anterior", cambio.anterior());
        detalle.put("nuevo", cambio.nuevo());
        return detalle;
    }
}
