package ec.mdd.cargas.dominio;

import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.util.Objects;
import java.util.Optional;

/**
 * La vista previa de un archivo (RF-10, RF-55, glosario «Vista previa»): la del dueño si sus filas
 * no tienen errores; si los tienen, solo cuántas filas, porque una fila con errores no se puede
 * comparar (FR-001).
 *
 * @param filasConErrores cuántas filas tienen algún error
 * @param delDueno la vista previa del dueño; vacía si hay errores
 */
public record VistaPrevia(int filasConErrores, Optional<VistaPreviaDelLote> delDueno) {

    /** Valida que solo un archivo sin errores tenga la vista del dueño. */
    public VistaPrevia {
        Objects.requireNonNull(delDueno, "delDueno");
        if ((filasConErrores > 0) == delDueno.isPresent()) {
            throw new IllegalArgumentException(
                    "La vista del dueño es solo de un archivo sin errores, y todo archivo sin"
                            + " errores la tiene");
        }
    }
}
