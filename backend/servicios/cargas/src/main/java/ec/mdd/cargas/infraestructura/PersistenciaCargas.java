package ec.mdd.cargas.infraestructura;

import ec.mdd.cargas.adaptadores.salida.HistorialDeCargasJdbc;
import ec.mdd.cargas.adaptadores.salida.TransaccionSpring;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.Transaccion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * La base de Cargas: el historial de cargas en su esquema (F01-H04) y su transacción. Va aparte de
 * {@link ConfiguracionCargas} para que las pruebas de los controladores, sin base, usen un
 * historial en memoria.
 */
@Configuration(proxyBeanMethods = false)
public class PersistenciaCargas {

    @Bean
    @DependsOn("flywayCargas")
    public HistorialDeCargas historialDeCargas(JdbcClient jdbc) {
        return new HistorialDeCargasJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionCargas(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }
}
