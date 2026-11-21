package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.Transicion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Los procesos de aprobación, uno por periodo (F12-H02, data-model.md). */
public interface RepositorioProceso {

    /**
     * @param periodo el código del periodo
     * @return su proceso, o vacío si todavía no se creó
     */
    Optional<ProcesoAprobacion> de(String periodo);

    /**
     * El proceso, bloqueado hasta que termine la transacción ({@code FOR UPDATE}): un segundo envío
     * espera al primero (research §8).
     *
     * @param periodo el código del periodo
     * @return su proceso, o vacío si todavía no se creó
     */
    Optional<ProcesoAprobacion> bloquear(String periodo);

    /**
     * Guarda un proceso nuevo. Si el periodo ya tenía uno, de un distributivo anterior, lo
     * reemplaza con sus parciales y sus transiciones (research §5).
     *
     * @param proceso el proceso nuevo, sin transiciones
     * @param ahora cuándo se crea
     */
    void crear(ProcesoAprobacion proceso, Instant ahora);

    /**
     * Guarda la etapa y los parciales del proceso, y agrega sus transiciones nuevas. Las
     * transiciones anteriores no se tocan (principio VII).
     *
     * @param proceso el proceso, ya cambiado
     * @param nuevas las transiciones que agregó este cambio
     */
    void guardar(ProcesoAprobacion proceso, List<Transicion> nuevas);

    /**
     * La etapa del proceso y de qué distributivo es, con un bloqueo compartido de su fila hasta que
     * termina la transacción en curso: un envío a revisión simultáneo espera (#215, research §2).
     *
     * @param periodo el código del periodo
     * @return vacío si el periodo no tiene proceso
     */
    Optional<EstadoCompartido> estadoCompartido(String periodo);

    /**
     * La etapa de un proceso y cuándo se creó su distributivo.
     *
     * @param estado la etapa actual
     * @param distributivoCreadoEn cuándo se creó el distributivo que acompaña
     */
    /**
     * Los procesos en curso, para la revisión diaria de plazos (F12-H03, D-114).
     *
     * @return los periodos con un proceso que no está en PUBLICADO
     */
    List<String> enCurso();

    /**
     * Los periodos con al menos una transición a PUBLICADO, para la publicación vigente que lee
     * Reporting (F20-H01, D-115). Durante una reforma el proceso ya no está en PUBLICADO, pero
     * sigue teniendo la publicación anterior.
     *
     * @return los periodos, ordenados
     */
    List<String> conPublicacion();

    record EstadoCompartido(TipoDeEtapa estado, Instant distributivoCreadoEn) {}
}
