package ec.mdd.plataforma.errores.nucleo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Error público que devuelve la API (esquema {@code Error} de {@code contratos/comun/errores.yaml},
 * principio XIII).
 *
 * <p>Solo contiene información segura y estable: el consumidor identifica el error por {@code
 * codigo}. Los campos sin valor no se envían, así que nunca hay {@code null} en el JSON.
 *
 * @param titulo categoría breve del error
 * @param descripcion qué ocurrió y qué puede hacer el usuario
 * @param codigo identificador estable del catálogo
 * @param detalles campos que no pasaron la validación; solo en errores de entrada
 * @param idIncidente identificador para buscar el incidente en los registros; solo en errores
 *     técnicos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaError(
        String titulo,
        String descripcion,
        String codigo,
        List<DetalleRespuesta> detalles,
        String idIncidente) {

    /** Copia defensiva de los detalles. */
    public RespuestaError {
        detalles = detalles == null ? null : List.copyOf(detalles);
    }

    /** Respuesta de un código del catálogo, sin detalles ni incidente. */
    public static RespuestaError deCatalogo(DefinicionDeCodigo definicion) {
        return new RespuestaError(
                definicion.titulo(), definicion.descripcion(), definicion.codigo(), null, null);
    }

    /** Respuesta de un error de entrada, con un detalle por campo. */
    public static RespuestaError conDetalles(
            DefinicionDeCodigo definicion, List<DetalleRespuesta> detalles) {
        return new RespuestaError(
                definicion.titulo(), definicion.descripcion(), definicion.codigo(), detalles, null);
    }

    /** Respuesta de un error técnico, con su identificador de incidente. */
    public static RespuestaError deIncidente(DefinicionDeCodigo definicion, String idIncidente) {
        return new RespuestaError(
                definicion.titulo(),
                definicion.descripcion(),
                definicion.codigo(),
                null,
                idIncidente);
    }
}
