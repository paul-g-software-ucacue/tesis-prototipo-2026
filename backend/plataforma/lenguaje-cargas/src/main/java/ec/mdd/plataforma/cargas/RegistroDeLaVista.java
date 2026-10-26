package ec.mdd.plataforma.cargas;

import java.util.List;
import java.util.Objects;

/**
 * Un registro de la vista previa.
 *
 * @param clave los valores de la clave de la carga, unidos con « · »
 * @param tipo qué pasa con él si se confirma
 * @param fila su fila en el archivo; {@code null} en una baja
 * @param cambios los campos que cambian; vacía si no es {@link TipoDeRegistro#CAMBIA}
 */
public record RegistroDeLaVista(
        String clave, TipoDeRegistro tipo, Integer fila, List<CambioDeCampo> cambios) {

    /** Copia los cambios. */
    public RegistroDeLaVista {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(tipo, "tipo");
        cambios = List.copyOf(cambios);
    }
}
