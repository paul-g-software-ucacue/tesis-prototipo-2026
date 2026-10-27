package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.puertos.entrada.ConsultarHistorial;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import java.util.List;

/** El historial de una carga (RF-10, F01-H04), del intento más reciente al más antiguo. */
public final class ConsultarHistorialServicio implements ConsultarHistorial {

    private final ConsultarPlantillas plantillas;
    private final HistorialDeCargas historial;

    /** Recibe las cargas del diccionario y el historial. */
    public ConsultarHistorialServicio(ConsultarPlantillas plantillas, HistorialDeCargas historial) {
        this.plantillas = plantillas;
        this.historial = historial;
    }

    @Override
    public List<EntradaDeHistorial> historial(String codigo) {
        return historial.deLaCarga(plantillas.carga(codigo).codigo());
    }
}
