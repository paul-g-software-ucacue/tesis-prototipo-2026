package ec.mdd.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Carga el contexto del gateway. Falla si la línea de Spring Cloud no es compatible con la versión
 * de Spring Boot (research §5).
 */
@SpringBootTest
class GatewayAplicacionTest {

    @Test
    void dadoElGateway_cuandoArrancaElContexto_entoncesCargaSinErrores() {}
}
