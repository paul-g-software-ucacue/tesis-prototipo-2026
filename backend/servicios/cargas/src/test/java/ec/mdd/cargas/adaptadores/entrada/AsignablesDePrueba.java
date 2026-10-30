package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.docentes.puertos.entrada.AsignablesDelPeriodo;
import ec.mdd.docentes.puertos.entrada.ConsultarAsignablesDelPeriodo;
import java.util.HashMap;
import java.util.Map;

/**
 * La consulta de proyectos asignables de Docentes, escrita a mano para las pruebas de Cargas
 * (F01-H05): los periodos 2026-I y 2026-II de la carga 03 de prueba, sin proyectos asignables salvo
 * que una prueba diga otra cosa; cualquier otro periodo no existe.
 */
public final class AsignablesDePrueba implements ConsultarAsignablesDelPeriodo {

    private final Map<String, AsignablesDelPeriodo> porPeriodo = new HashMap<>();

    /** Con los dos periodos de prueba sin proyectos asignables. */
    public AsignablesDePrueba() {
        reiniciar();
    }

    /** Desde ahora, el periodo tiene esa respuesta. */
    public synchronized void con(String periodo, AsignablesDelPeriodo asignables) {
        porPeriodo.put(periodo, asignables);
    }

    /** Vuelve a los dos periodos de prueba sin proyectos asignables. */
    public synchronized void reiniciar() {
        porPeriodo.clear();
        porPeriodo.put("2026-I", AsignablesDelPeriodo.NINGUNO);
        porPeriodo.put("2026-II", AsignablesDelPeriodo.NINGUNO);
    }

    @Override
    public synchronized AsignablesDelPeriodo asignablesEn(String periodo) {
        return porPeriodo.getOrDefault(periodo, AsignablesDelPeriodo.PERIODO_NO_ENCONTRADO);
    }
}
