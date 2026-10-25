package ec.mdd.docentes.puertos.salida;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.TipoDeCargo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Los cargos de los docentes (F03-H05, carga 8). Ninguno se borra: se finaliza. */
public interface RepositorioCargos {

    /** Todos los cargos, por docente, cargo y fecha de designación. */
    List<Cargo> todos();

    /** El cargo con esa clave, si existe. */
    Optional<Cargo> buscar(String docente, TipoDeCargo tipo, LocalDate fechaDesignacion);

    /**
     * Si la carrera ya tiene un coordinador vigente (RN-12).
     *
     * @param salvo el cargo que se corrige, que no cuenta; o {@code null}
     */
    boolean hayCoordinadorVigente(String carrera, Cargo salvo);

    /** Bloquea los cargos hasta que termine la transacción. */
    void bloquear();

    /** Guarda un cargo nuevo, con quién y cuándo lo registró. */
    void registrar(Cargo cargo, String quien, Instant momento);

    /** Guarda los datos de un cargo que ya existe; su clave no cambia. */
    void guardar(Cargo cargo);
}
