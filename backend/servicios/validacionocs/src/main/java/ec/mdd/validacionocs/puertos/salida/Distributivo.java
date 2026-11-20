package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que Aprobación necesita del distributivo, que es de Generación (MD-04, F12-H02). Dentro del
 * proceso lo da su interfaz publicada; Validación OCS no lee sus tablas (principio V).
 */
public interface Distributivo {

    /**
     * @param periodo el código del periodo
     * @return la versión vigente, cuándo se creó el distributivo y si cada parcial está enviado en
     *     Generación; vacío si no hay distributivo
     */
    Optional<DistributivoActual> actual(String periodo);

    /**
     * Le ordena a Generación marcar el parcial como enviado, en su propia transacción. Es
     * idempotente.
     *
     * @param periodo el código del periodo
     * @param carrera la carrera del parcial
     * @return la versión y el hash del contenido del parcial
     */
    ParcialEnviadoEnGeneracion marcarEnviado(String periodo, String carrera);

    /**
     * @param periodo el código del periodo
     * @return el hash del distributivo, el de todos sus parciales
     */
    String hash(String periodo);

    /**
     * Las condiciones del consolidado para enviarlo a revisión, calculadas al pedirlas (F10-H03,
     * D-97). Puede llamar a otro proceso: no se usa dentro de una transacción (D-83).
     *
     * @param periodo el código del periodo
     * @return si se consolidó, si quedó desactualizado, el informe y las abiertas
     */
    CondicionesDeRevision condicionesDeRevision(String periodo);

    /**
     * Marca como devueltos los parciales de esas carreras, todos o ninguno, para que vuelvan a
     * editarse (F12-H05, research §2). Es idempotente.
     *
     * @param periodo el código del periodo
     * @param carreras las carreras, sin repetidas
     * @return el número de la versión vigente
     * @throws ec.mdd.validacionocs.dominio.ParcialNoEncontrado si una carrera no tiene parcial
     */
    int marcarDevueltos(String periodo, List<String> carreras);

    /**
     * Le ordena a Generación congelar la versión vigente, en su propia transacción (F13-H01,
     * D-100). Es idempotente: si ya estaba congelada, devuelve lo mismo.
     *
     * @param periodo el código del periodo
     * @return el número de la versión y el hash del distributivo
     */
    VersionCongeladaEnAprobacion congelar(String periodo);

    /**
     * Le ordena a Generación anular la versión rechazada en firma y crear la siguiente, en su
     * propia transacción (F12-H04, D-106). Es idempotente: si ya la creó, devuelve la misma.
     *
     * @param periodo el código del periodo
     * @param anulada la versión rechazada en firma
     * @param momento cuándo se rechazó
     * @return el número y el hash de la versión nueva; vacío si Generación no tiene esa versión
     *     congelada ni la siguiente ya creada
     * @throws SinDistributivo si el periodo no tiene distributivo
     */
    Optional<VersionNuevaEnAprobacion> crearVersionNueva(
            String periodo, int anulada, java.time.Instant momento);

    /**
     * Le ordena a Generación crear la versión de una reforma, que copia la publicada, en su propia
     * transacción (F18-H02, D-113). La publicada no se anula: sigue CONGELADA. Es idempotente: si
     * ya la creó, devuelve la misma.
     *
     * @param periodo el código del periodo
     * @param publicada la versión publicada que se reforma
     * @param quien quien inició la reforma
     * @param momento cuándo
     * @return el número y el hash de la versión nueva; vacío si Generación no tiene esa versión
     *     congelada como vigente ni la siguiente ya creada para una reforma
     * @throws SinDistributivo si el periodo no tiene distributivo
     */
    Optional<VersionNuevaEnAprobacion> crearVersionDeReforma(
            String periodo, int publicada, String quien, java.time.Instant momento);

    /**
     * El distributivo de un periodo.
     *
     * @param version el número de la versión vigente
     * @param creadoEn cuándo se creó
     * @param enviados por carrera, si Generación tiene el parcial como enviado
     */
    record DistributivoActual(int version, Instant creadoEn, Map<String, Boolean> enviados) {

        /** Copia el mapa. */
        public DistributivoActual {
            enviados = Map.copyOf(enviados);
        }
    }

    /**
     * Un parcial marcado en Generación.
     *
     * @param version el número de la versión vigente
     * @param hash el SHA-256 de su contenido
     */
    record ParcialEnviadoEnGeneracion(int version, String hash) {}

    /**
     * La versión que congeló Generación (F13-H01).
     *
     * @param version su número
     * @param hash el SHA-256 del distributivo, el de todos sus parciales
     */
    record VersionCongeladaEnAprobacion(int version, String hash) {}

    /**
     * La versión nueva que creó Generación (F12-H04).
     *
     * @param version su número
     * @param hash el hash del distributivo de esa versión
     */
    record VersionNuevaEnAprobacion(int version, String hash) {}
}
