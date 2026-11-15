package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.ResultadoValidacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La generación, el documento firmable, sus firmantes esperados y las validaciones de sus firmas,
 * en el esquema {@code firma} (F13-H02, F14-H01, F14-H02).
 */
public interface RepositorioDocumentos {

    /**
     * La generación de esa versión, bloqueada hasta que termine la transacción: otra generación de
     * la misma versión espera. Se llama dentro de una transacción.
     *
     * @return vacía si Aprobación todavía no pidió el documento de esa versión
     */
    Optional<GeneracionDeDocumento> bloquear(String periodo, int version);

    /** La generación de la última versión que pidió Aprobación en el periodo, sin bloquear. */
    Optional<GeneracionDeDocumento> ultima(String periodo);

    /** Guarda la generación: la crea si no existe, si no la reemplaza. */
    void guardar(GeneracionDeDocumento generacion);

    /**
     * Registra el documento con el firmante esperado de cada paso (F14-H01, D-102); no se cambian
     * ni se borran. Se llama dentro de la transacción que completa la generación.
     */
    void registrar(DocumentoFirmable documento, List<FirmanteEsperado> firmantes);

    /**
     * Los documentos generados, con la cadena de su generación y sus firmantes esperados, por
     * periodo y versión (F14-H01).
     */
    List<FirmasDelDocumento> generados();

    /** Un documento generado por su identificador, con su cadena y sus firmantes (F14-H01). */
    Optional<FirmasDelDocumento> firmas(UUID documento);

    /** El documento de esa versión, si se generó. */
    Optional<DocumentoFirmable> documento(String periodo, int version);

    /**
     * Registra el resultado de una subida del firmante en turno (F14-H02, D-103); no se cambia ni
     * se borra. Se llama dentro de la transacción que publica su cambio para la bitácora.
     *
     * <p>Si el resultado se aceptó, es la firma del paso: antes de registrarlo se bloquea la
     * generación de la versión y se comprueba que las firmas del documento siguen siendo las de
     * {@code validadoContra}. Si otra subida firmó entretanto, no registra nada y devuelve falso:
     * el archivo ya no empieza con el vigente. Tampoco si el documento se rechazó en firma
     * entretanto (F14-H04). Un resultado rechazado se registra siempre.
     *
     * @param resultado el resultado de la validación
     * @param validadoContra el documento, con las firmas contra las que se validó
     * @return falso solo si un resultado aceptado llegó tarde y no se registró
     */
    boolean registrarValidacion(ResultadoValidacion resultado, FirmasDelDocumento validadoContra);

    /**
     * Registra el rechazo en firma del documento (F14-H04, D-105); no se cambia ni se borra. Se
     * llama dentro de la transacción que publica su cambio y su aviso.
     *
     * <p>Antes de registrarlo se bloquea la generación de la versión y se comprueba que las firmas
     * siguen siendo las de {@code vistas} y que nadie rechazó: si alguien firmó o rechazó
     * entretanto, quien rechaza ya no está en turno, y no registra nada.
     *
     * @param rechazo el paso, quién, cuándo y la observación
     * @param vistas el documento, con las firmas que tenía cuando se comprobó el turno
     * @return falso si las firmas cambiaron o ya había un rechazo
     */
    boolean registrarRechazo(RechazoDelFirmante rechazo, FirmasDelDocumento vistas);

    /**
     * Los resultados de las subidas de un documento, aceptadas y rechazadas, en el orden en que
     * llegaron (F14-H02): el informe de validación de cada una.
     */
    List<ResultadoValidacion> validaciones(UUID documento);
}
