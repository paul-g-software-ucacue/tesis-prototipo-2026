package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Malla;
import java.time.Instant;
import java.util.List;

/**
 * Las carreras, sus mallas y sus asignaturas, y los 11 campos amplios de la CINE, del esquema
 * {@code academico} (F03-H02).
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina, y así la aplicación decide los códigos y las siglas repetidos con lo que leyó, sin
 * que otro cambio se cruce (research §4).
 */
public interface RepositorioCarreras {

    /** Los 11 campos amplios, por código. */
    List<CampoAmplio> camposAmplios();

    /** Todas las carreras, por código. */
    List<Carrera> carreras();

    /** Todas las mallas, por código. */
    List<Malla> mallas();

    /** Todas las asignaturas, activas e inactivas, por código. */
    List<Asignatura> asignaturas();

    /** Bloquea las carreras, las mallas y las asignaturas hasta que termine la transacción. */
    void bloquear();

    /** Guarda una carrera nueva. */
    void registrar(Carrera carrera, String quien, Instant momento);

    /** Guarda una malla nueva. */
    void registrar(Malla malla, String quien, Instant momento);

    /** Guarda una asignatura nueva. */
    void registrar(Asignatura asignatura, String quien, Instant momento);

    /** Guarda los datos y el estado de una carrera que ya existe; el código no cambia. */
    void guardar(Carrera carrera);

    /** Guarda el estado de una malla que ya existe. */
    void guardar(Malla malla);

    /** Guarda los datos y el estado de una asignatura que ya existe. */
    void guardar(Asignatura asignatura);
}
