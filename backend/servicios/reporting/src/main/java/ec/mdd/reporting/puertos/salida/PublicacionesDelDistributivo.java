package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.Publicacion;
import java.util.List;

/**
 * La publicación vigente de cada periodo, de Aprobación (MD-11, D-115). Reporting no decide cuál
 * versión está publicada: solo la lee.
 */
public interface PublicacionesDelDistributivo {

    /**
     * @return una por periodo con distributivo publicado
     * @throws ec.mdd.plataforma.eventos.ProcesoConError si Aprobación falla
     * @throws ec.mdd.plataforma.eventos.ProcesoSinRespuesta si no responde a tiempo
     */
    List<Publicacion> vigentes();
}
