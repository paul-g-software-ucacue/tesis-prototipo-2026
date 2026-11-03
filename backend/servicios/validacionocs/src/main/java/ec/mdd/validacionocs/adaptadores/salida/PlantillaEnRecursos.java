package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.puertos.salida.PlantillaDelFlujo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** La plantilla viene dentro del servicio: si faltara, no arranca (D-89). */
public final class PlantillaEnRecursos implements PlantillaDelFlujo {

    private static final String RECURSO = "/flujo-por-defecto.yaml";

    private final String texto;

    /** Lee el recurso. */
    public PlantillaEnRecursos() {
        try (InputStream entrada = PlantillaEnRecursos.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + RECURSO);
            }
            texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Override
    public String texto() {
        return texto;
    }
}
