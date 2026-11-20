package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.MotivoDeVersion;
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

    /**
     * Crea la siguiente versión con todo el contenido de la vigente, por orden de Aprobación
     * (F12-H04, D-106). Con el rechazo en firma, la vigente pasa a ANULADA; con la reforma
     * (F18-H02, D-113), sigue CONGELADA. Se llama dentro de la transacción que la bloqueó.
     *
     * @param anulada la versión que se anula, la vigente
     * @param motivo por qué se crea la nueva
     * @param quien quién la crea
     * @param momento cuándo
     * @return el id de la versión nueva
     */
    UUID crearVersionNueva(UUID anulada, MotivoDeVersion motivo, String quien, Instant momento);

    /**
     * El estado de una versión del periodo (F12-H04).
     *
     * @param periodo el código del periodo
     * @param numero el número de la versión
     * @return vacío si no existe
     */
    Optional<EstadoDeVersion> estadoDe(String periodo, int numero);

    /**
     * El motivo de una versión del distributivo del periodo (F18-H02).
     *
     * @param periodo el código del periodo
     * @param numero el número de la versión
     * @return su motivo; vacío en la versión 1, que no tiene, o si no existe
     */
    Optional<ec.mdd.generacion.puertos.entrada.MotivoDeVersion> motivoDe(
            String periodo, int numero);
}
