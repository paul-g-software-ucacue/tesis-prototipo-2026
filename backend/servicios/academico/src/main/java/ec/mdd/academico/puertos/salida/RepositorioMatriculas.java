package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.Matricula;
import java.time.Instant;
import java.util.List;

/** Dónde quedan los matriculados por carrera y periodo (F01-H04, carga 10). */
public interface RepositorioMatriculas {

    /** Los matriculados de esos periodos. */
    List<Matricula> deLosPeriodos(List<String> periodos);

    /** Bloquea la tabla hasta el fin de la transacción. */
    void bloquear();

    /** Registra los matriculados de una carrera en un periodo. */
    void registrar(Matricula matricula, String quien, Instant momento);

    /** Guarda el total, la fecha de corte y la fuente de unos matriculados que existen. */
    void guardar(Matricula matricula);
}
