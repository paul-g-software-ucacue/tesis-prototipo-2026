package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La generación, el documento firmable y sus firmantes esperados de cada versión, en el esquema
 * {@code firma} (F13-H02, F14-H01).
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
}
