package ec.mdd.cargas.aplicacion;

import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * El servicio dueño de cada carga (MD-13, ADR-022): cada una de las 10 tiene exactamente un
 * aplicador. Si no, el proceso no arranca y dice qué carga falla.
 */
public final class DuenosDeLasCargas {

    private final Map<String, AplicadorDeCargas> porCarga = new HashMap<>();

    /**
     * @param aplicadores los de los servicios del proceso
     * @param cargas los códigos de las 10 cargas del diccionario
     * @throws IllegalStateException si una carga no tiene dueño, tiene dos o no existe
     */
    public DuenosDeLasCargas(List<AplicadorDeCargas> aplicadores, Collection<String> cargas) {
        for (AplicadorDeCargas aplicador : aplicadores) {
            for (String carga : aplicador.cargas()) {
                if (porCarga.putIfAbsent(carga, aplicador) != null) {
                    throw new IllegalStateException("La carga " + carga + " tiene dos dueños");
                }
            }
        }
        TreeSet<String> sinDueno = new TreeSet<>(cargas);
        sinDueno.removeAll(porCarga.keySet());
        if (!sinDueno.isEmpty()) {
            throw new IllegalStateException("Estas cargas no tienen dueño: " + sinDueno);
        }
        TreeSet<String> desconocidas = new TreeSet<>(porCarga.keySet());
        desconocidas.removeAll(cargas);
        if (!desconocidas.isEmpty()) {
            throw new IllegalStateException(
                    "Estas cargas no están en el diccionario: " + desconocidas);
        }
    }

    /** El dueño de una carga. */
    public AplicadorDeCargas de(String carga) {
        return porCarga.get(carga);
    }

    /** Si el dueño de la carga tiene datos vigentes de ella en ese periodo (F02-H02). */
    public boolean tieneDatosEn(String carga, String periodo) {
        return de(carga).tieneDatosEn(carga, periodo);
    }

    /** Si el dueño de la carga tiene datos vigentes de ella (F01-H05). */
    public boolean tieneDatos(String carga) {
        return de(carga).tieneDatos(carga);
    }
}
