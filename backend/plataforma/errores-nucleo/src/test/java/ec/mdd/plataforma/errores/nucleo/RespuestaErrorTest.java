package ec.mdd.plataforma.errores.nucleo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RespuestaErrorTest {

    @Test
    void dadoUnCodigoDelCatalogo_cuandoSeArmaLaRespuesta_entoncesNoTraeDetallesNiIncidente() {
        RespuestaError respuesta = RespuestaError.deCatalogo(CodigosComunes.RECURSO_NO_ENCONTRADO);

        assertThat(respuesta.codigo()).isEqualTo("COMUN_RECURSO_NO_ENCONTRADO");
        assertThat(respuesta.titulo()).isEqualTo(CodigosComunes.RECURSO_NO_ENCONTRADO.titulo());
        assertThat(respuesta.detalles()).isNull();
        assertThat(respuesta.idIncidente()).isNull();
    }

    @Test
    void dadoDetallesDeValidacion_cuandoSeArmaLaRespuesta_entoncesSoloTraeDetalles() {
        RespuestaError respuesta =
                RespuestaError.conDetalles(
                        CodigosComunes.SOLICITUD_INVALIDA,
                        List.of(new DetalleRespuesta("usuario", "es obligatorio")));

        assertThat(respuesta.detalles())
                .containsExactly(new DetalleRespuesta("usuario", "es obligatorio"));
        assertThat(respuesta.idIncidente()).isNull();
    }

    @Test
    void dadoUnIncidente_cuandoSeArmaLaRespuesta_entoncesSoloTraeElIdentificador() {
        RespuestaError respuesta = RespuestaError.deIncidente(CodigosComunes.ERROR_INTERNO, "id-1");

        assertThat(respuesta.idIncidente()).isEqualTo("id-1");
        assertThat(respuesta.detalles()).isNull();
    }

    @Test
    void dadoUnEstadoHttp_cuandoSeBuscaSuCodigoComun_entoncesUsaLaCorrespondencia() {
        assertThat(CodigosComunes.segunEstado(401))
                .isEqualTo(CodigosComunes.SEGURIDAD_NO_AUTENTICADO);
        assertThat(CodigosComunes.segunEstado(403)).isEqualTo(CodigosComunes.ACCESO_NO_AUTORIZADO);
        assertThat(CodigosComunes.segunEstado(404)).isEqualTo(CodigosComunes.RECURSO_NO_ENCONTRADO);
        assertThat(CodigosComunes.segunEstado(405)).isEqualTo(CodigosComunes.METODO_NO_PERMITIDO);
        assertThat(CodigosComunes.segunEstado(406)).isEqualTo(CodigosComunes.FORMATO_NO_ACEPTADO);
        assertThat(CodigosComunes.segunEstado(415)).isEqualTo(CodigosComunes.TIPO_NO_SOPORTADO);
        assertThat(CodigosComunes.segunEstado(413))
                .isEqualTo(CodigosComunes.CONTENIDO_DEMASIADO_GRANDE);
        assertThat(CodigosComunes.segunEstado(409)).isEqualTo(CodigosComunes.SOLICITUD_INVALIDA);
        assertThat(CodigosComunes.segunEstado(503))
                .isEqualTo(CodigosComunes.SERVICIO_NO_DISPONIBLE);
        assertThat(CodigosComunes.segunEstado(502)).isEqualTo(CodigosComunes.ERROR_INTERNO);
        assertThat(CodigosComunes.TODOS).hasSize(15);
    }
}
