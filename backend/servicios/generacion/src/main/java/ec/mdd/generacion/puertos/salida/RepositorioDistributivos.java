package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Los distributivos, en el esquema {@code generacion} (MD-01). */
public interface RepositorioDistributivos {

    /** La versión vigente del periodo, con sus parciales y sus propuestas; vacío si no tiene. */
    Optional<VersionDistributivo> vigenteDe(String periodo);

    /**
     * La versión vigente, bloqueada hasta que termine la transacción (D-83). Quien llega después
     * espera y lee lo que guardó el primero. Se llama dentro de una transacción.
     *
     * @return vacío si el periodo no tiene distributivo
     */
    Optional<VersionDistributivo> bloquearVigente(String periodo);

    /**
     * Guarda un distributivo nuevo con su versión y sus parciales.
     *
     * @throws DistributivoRepetido si el periodo ya tiene uno
     */
    void crear(String periodo, VersionDistributivo version, String quien, Instant momento);

    /** Agrega un parcial a la versión. */
    void agregarParcial(UUID version, ParcialCarrera parcial);

    /** Guarda la propuesta; reemplaza la de esa asignatura-paralelo. */
    void guardarPropuesta(UUID version, Propuesta propuesta);

    /** Guarda la actividad; reemplaza sus horas si el parcial ya la tiene. */
    void guardarActividad(UUID version, ActividadNoLectiva actividad);

    /**
     * Quita una actividad.
     *
     * @return si existía
     */
    boolean quitarActividad(
            UUID version, String carrera, String docente, String actividad, String proyecto);

    /**
     * Quita una propuesta.
     *
     * @return si existía
     */
    boolean quitarPropuesta(UUID version, String asignatura, String paralelo);

    /**
     * Cuándo se creó el distributivo del periodo (F12-H02).
     *
     * @param periodo el código del periodo
     * @return el instante; vacío si no hay distributivo
     */
    Optional<Instant> creadoEn(String periodo);

    /**
     * Cambia el estado de un parcial, por orden de Aprobación (MD-04, F12-H02).
     *
     * @param version la versión
     * @param carrera la carrera del parcial
     * @param estado el estado nuevo
     */
    void marcarParcial(UUID version, String carrera, EstadoDelParcial estado);

    /**
     * Congela la versión, por orden de Aprobación (MD-04, F13-H01). Se llama dentro de la
     * transacción que la bloqueó.
     *
     * @param version la versión
     */
    void congelar(UUID version);

    /**
     * El número de la versión vigente si está congelada (F13-H01). Dentro de una transacción, la
     * lee con un bloqueo compartido: no espera a otras lecturas, pero una congelación que llega
     * después espera a que termine.
     *
     * @param periodo el código del periodo
     * @return el número; vacío si no está congelada o el periodo no tiene distributivo
     */
    OptionalInt versionCongelada(String periodo);
}
