package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import ec.mdd.docentes.puertos.entrada.ConsultarAsignablesDelPeriodo;

/**
 * Los proyectos asignables de un periodo, por el puerto publicado de Docentes, dentro del proceso
 * {@code datos} (principio V, F01-H05).
 */
public final class ProyectosDesdePuertos implements ProyectosDelPeriodo {

    private final ConsultarAsignablesDelPeriodo consultar;

    /** Recibe la consulta de Docentes. */
    public ProyectosDesdePuertos(ConsultarAsignablesDelPeriodo consultar) {
        this.consultar = consultar;
    }

    @Override
    public boolean hayAsignables(String periodo) {
        return switch (consultar.asignablesEn(periodo)) {
            case PERIODO_NO_ENCONTRADO -> throw new PeriodoNoEncontrado(periodo);
            case NINGUNO -> false;
            case ALGUNO -> true;
        };
    }
}
