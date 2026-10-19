package ec.mdd.normativa.infraestructura;

import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.aplicacion.ConsultarNormativaServicio;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Ensambla Normativa: el repositorio del esquema {@code normativa} y la consulta de sus versiones.
 * El repositorio espera a que Flyway migre el esquema, que trae la versión 1 (F04-H01).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionNormativa {

    @Bean
    @DependsOn("flywayNormativa")
    public RepositorioVersiones repositorioVersiones(JdbcClient jdbc) {
        return new RepositorioVersionesJdbc(jdbc);
    }

    /** La consulta de las versiones de normativa (RF-19). */
    @Bean
    public ConsultarNormativa consultarNormativa(RepositorioVersiones repositorio) {
        return new ConsultarNormativaServicio(repositorio);
    }
}
