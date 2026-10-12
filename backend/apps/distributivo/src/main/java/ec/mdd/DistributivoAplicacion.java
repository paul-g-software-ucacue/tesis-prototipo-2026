package ec.mdd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranque del proceso. Vive en {@code ec.mdd} para que los módulos de Spring Modulith sean los
 * servicios {@code ec.mdd.<servicio>} que el proceso declara como dependencia (ADR-012).
 */
@SpringBootApplication
public class DistributivoAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(DistributivoAplicacion.class, args);
    }
}
