package ec.mdd.plataforma.cargas;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * La vista previa de un lote frente a lo que tiene su dueño (RF-10, RF-55, glosario «Vista
 * previa»), con su huella: la confirmación solo aplica la misma vista previa (FR-006).
 *
 * @param registros todos los registros, en orden de clave
 * @param huella el SHA-256, en hexadecimal, de su forma canónica
 */
public record VistaPreviaDelLote(List<RegistroDeLaVista> registros, String huella) {

    /** Copia los registros. */
    public VistaPreviaDelLote {
        registros = List.copyOf(registros);
    }

    /** Cuántos registros hay de un tipo. */
    public int cuantos(TipoDeRegistro tipo) {
        return (int) registros.stream().filter(registro -> registro.tipo() == tipo).count();
    }

    /** Los primeros registros de cada tipo, sin los iguales, para la pantalla. */
    public List<RegistroDeLaVista> primeros(int deCadaTipo) {
        Map<TipoDeRegistro, Integer> vistos = new EnumMap<>(TipoDeRegistro.class);
        List<RegistroDeLaVista> primeros = new ArrayList<>();
        for (RegistroDeLaVista registro : registros) {
            if (registro.tipo() == TipoDeRegistro.IGUAL) {
                continue;
            }
            int cuantos = vistos.merge(registro.tipo(), 1, Integer::sum);
            if (cuantos <= deCadaTipo) {
                primeros.add(registro);
            }
        }
        return primeros;
    }
}
