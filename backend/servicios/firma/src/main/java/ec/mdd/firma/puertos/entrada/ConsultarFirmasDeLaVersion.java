package ec.mdd.firma.puertos.entrada;

import ec.mdd.firma.dominio.FirmasDelDocumento;
import java.util.Optional;

/**
 * Caso de uso: la cadena de firmas del documento CONSOLIDADO de una versión, para otro proceso
 * (F19-H02, D-120). Reporting la usa para validar el CSV para SIGA de la versión publicada vigente,
 * que durante una reforma no es la última del periodo (D-113).
 *
 * <p>Firma solo informa los hechos del documento: sus pasos, sus firmas, su rechazo y su anulación.
 * La regla de qué es una versión «firmada» es de quien la usa.
 */
public interface ConsultarFirmasDeLaVersion {

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return el documento de esa versión con su cadena; vacío si la versión no tiene documento
     */
    Optional<FirmasDelDocumento> firmas(String periodo, int version);
}
