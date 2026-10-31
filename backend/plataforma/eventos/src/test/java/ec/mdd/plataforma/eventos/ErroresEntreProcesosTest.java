package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.RegistroDeErrores;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cada fallo de una consulta entre procesos responde su código común (ADR-023). */
class ErroresEntreProcesosTest {

    private final RegistroDeErrores registro =
            new RegistroDeErrores(List.of(new ErroresEntreProcesos()));

    @Test
    void dadoUnProcesoConError_cuandoSeTraduce_entonces502() {
        assertThat(registro.buscar(ProcesoConError.class))
                .map(DefinicionDeCodigo::codigo)
                .contains(CodigosComunes.PROCESO_CON_ERROR.codigo());
    }

    @Test
    void dadoUnProcesoSinRespuesta_cuandoSeTraduce_entonces504() {
        assertThat(registro.buscar(ProcesoSinRespuesta.class))
                .map(DefinicionDeCodigo::codigo)
                .contains(CodigosComunes.PROCESO_SIN_RESPUESTA.codigo());
    }
}
