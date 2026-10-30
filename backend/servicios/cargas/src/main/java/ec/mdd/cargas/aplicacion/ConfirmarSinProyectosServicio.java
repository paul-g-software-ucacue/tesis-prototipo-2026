package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.ConfirmacionSinProyectos;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.puertos.entrada.ConfirmarSinProyectos;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.PeriodoConProyectos;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import java.time.Clock;
import java.time.Instant;

/**
 * Confirma «sin proyectos» un periodo (RF-18, F01-H05): solo si no tiene proyectos asignables, y
 * una sola vez; queda en el historial de la carga de proyectos con el usuario y el momento.
 */
public final class ConfirmarSinProyectosServicio implements ConfirmarSinProyectos {

    private final ConsultarPlantillas plantillas;
    private final ProyectosDelPeriodo proyectos;
    private final HistorialDeCargas historial;
    private final Clock reloj;

    /** Recibe el diccionario, la consulta de proyectos de Docentes, el historial y el reloj. */
    public ConfirmarSinProyectosServicio(
            ConsultarPlantillas plantillas,
            ProyectosDelPeriodo proyectos,
            HistorialDeCargas historial,
            Clock reloj) {
        this.plantillas = plantillas;
        this.proyectos = proyectos;
        this.historial = historial;
        this.reloj = reloj;
    }

    @Override
    public ConfirmacionSinProyectos confirmar(String periodo, String quien) {
        if (proyectos.hayAsignables(periodo)) {
            throw new PeriodoConProyectos(periodo);
        }
        return historial.confirmarSinProyectos(
                EntradaDeHistorial.sinProyectos(
                        CargaDeProyectos.codigo(plantillas.diccionario()),
                        periodo,
                        quien,
                        Instant.now(reloj)));
    }
}
