package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.CambioDeValor;
import java.util.LinkedHashMap;
import java.util.Map;

/** El detalle de un cambio de valor para el registro, igual en los periodos y en la estructura. */
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
