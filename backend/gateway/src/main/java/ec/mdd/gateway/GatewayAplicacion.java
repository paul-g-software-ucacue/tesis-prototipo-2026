package ec.mdd.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranque del API Gateway, el único punto de entrada hacia los 4 procesos (ADR-006, ADR-008).
 *
 * <p>En B00-H02 solo arranca y expone su salud; las rutas hacia los procesos y la validación del
 * JWT llegan con B00-H03.
 */
@SpringBootApplication
public class GatewayAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(GatewayAplicacion.class, args);
    }
}
