package ec.mdd.plataforma.eventos;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;

/** Los fallos de las consultas entre procesos, con sus códigos comunes (ADR-023). */
public final class ErroresEntreProcesos implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                new EntradaCatalogo(ProcesoConError.class, CodigosComunes.PROCESO_CON_ERROR),
                new EntradaCatalogo(
                        ProcesoSinRespuesta.class, CodigosComunes.PROCESO_SIN_RESPUESTA));
    }
}
