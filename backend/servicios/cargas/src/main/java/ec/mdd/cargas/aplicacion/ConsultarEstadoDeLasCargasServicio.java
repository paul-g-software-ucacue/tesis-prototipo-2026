package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.EstadoDeCarga;
import ec.mdd.cargas.dominio.OrdenDeLasCargas;
import ec.mdd.cargas.puertos.entrada.ConsultarEstadoDeLasCargas;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import java.util.List;

/**
 * El estado de las 10 cargas (RF-56, F01-H05): lo que dice cada servicio dueño y las confirmaciones
 * «sin proyectos» que siguen valiendo. Se calcula en cada consulta; no se guarda.
 */
public final class ConsultarEstadoDeLasCargasServicio implements ConsultarEstadoDeLasCargas {

    private final ConsultarPlantillas plantillas;
    private final DuenosDeLasCargas duenos;
    private final HistorialDeCargas historial;
    private final ProyectosDelPeriodo proyectos;

    /** Recibe el diccionario, los dueños, el historial y la consulta de proyectos de Docentes. */
    public ConsultarEstadoDeLasCargasServicio(
            ConsultarPlantillas plantillas,
            DuenosDeLasCargas duenos,
            HistorialDeCargas historial,
            ProyectosDelPeriodo proyectos) {
        this.plantillas = plantillas;
        this.duenos = duenos;
        this.historial = historial;
        this.proyectos = proyectos;
    }

    @Override
    public List<EstadoDeCarga> estados() {
        DiccionarioDeDatos diccionario = plantillas.diccionario();
        List<String> periodos =
                historial.sinProyectos(CargaDeProyectos.codigo(diccionario)).stream()
                        .map(entrada -> entrada.periodo().orElseThrow())
                        .filter(this::sigueSinProyectos)
                        .toList();
        return new OrdenDeLasCargas(diccionario, duenos::tieneDatos).estados(periodos);
    }

    /** Una confirmación vale mientras su periodo exista y no tenga proyectos asignables. */
    private boolean sigueSinProyectos(String periodo) {
        try {
            return !proyectos.hayAsignables(periodo);
        } catch (PeriodoNoEncontrado yaNoExiste) {
            return false;
        }
    }
}
