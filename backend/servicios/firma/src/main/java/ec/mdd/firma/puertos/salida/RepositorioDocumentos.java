package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import java.util.Optional;

/** La generación y el documento firmable de cada versión, en el esquema {@code firma} (F13-H02). */
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

    /** Registra el documento; no se cambia ni se borra. */
    void registrar(DocumentoFirmable documento);

    /** El documento de esa versión, si se generó. */
    Optional<DocumentoFirmable> documento(String periodo, int version);
}
