package ec.mdd;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica que ningún servicio del proceso use una clase de otro servicio que no esté en su API
 * publicada, {@code puertos.entrada} (escenario «Un servicio no usa las clases internas de otro»).
 */
class ModulosTest {

    @Test
    void losServiciosDelProcesoSoloUsanLaApiPublicadaDeLosDemas() {
        ApplicationModules.of(DemoAplicacion.class).verify();
    }
}
