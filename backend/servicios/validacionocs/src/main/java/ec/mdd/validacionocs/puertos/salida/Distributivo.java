package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import java.time.Instant;
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
}
