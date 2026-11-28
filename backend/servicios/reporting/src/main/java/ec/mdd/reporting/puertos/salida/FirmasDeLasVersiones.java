package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import java.util.Optional;

/**
 * La cadena de firmas del documento de una versión, de Firma (F19-H02, D-120). Es por versión, no
 * la del documento vigente del periodo, porque durante una reforma la versión publicada vigente no
 * es la última (D-113).
 */
public interface FirmasDeLasVersiones {

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return la cadena; vacío si la versión no tiene documento firmable
     * @throws ec.mdd.plataforma.eventos.ProcesoConError si Firma falla o responde algo inválido
     * @throws ec.mdd.plataforma.eventos.ProcesoSinRespuesta si Firma no responde a tiempo
     */
    Optional<FirmasDeLaVersion> firmas(String periodo, int version);
}
