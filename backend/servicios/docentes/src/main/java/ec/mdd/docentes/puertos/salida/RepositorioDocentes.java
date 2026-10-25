package ec.mdd.docentes.puertos.salida;

import ec.mdd.docentes.dominio.Docente;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Los docentes con su ficha académica, del esquema {@code docentes} (F03-H04).
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina, y así la aplicación decide la identificación y el correo repetidos con lo que leyó,
 * sin que otro cambio se cruce (research §5).
 */
public interface RepositorioDocentes {

    /** Todos los docentes, por apellidos y nombres. */
    List<Docente> todos();

    /** El docente con esa identificación, si existe. */
    Optional<Docente> buscar(String identificacion);

    /**
     * Si otro docente usa ese correo, sin distinguir mayúsculas.
     *
     * @param salvo la identificación del docente que se corrige, que puede conservar el suyo; o
     *     {@code null}
     */
    boolean existeCorreo(String correo, String salvo);

    /** Bloquea los docentes hasta que termine la transacción. */
    void bloquear();

    /** Guarda un docente nuevo, con quién y cuándo lo registró. */
    void registrar(Docente docente, String quien, Instant momento);

    /**
     * Guarda los datos, la ficha y el estado de un docente que ya existe; la identificación no
     * cambia.
     */
    void guardar(Docente docente);
}
