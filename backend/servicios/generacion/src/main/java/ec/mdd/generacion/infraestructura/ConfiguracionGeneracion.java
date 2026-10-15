package ec.mdd.generacion.infraestructura;

import ec.mdd.generacion.adaptadores.salida.PublicadorDeEventosSpring;
import ec.mdd.generacion.adaptadores.salida.RepositorioEventosDePruebaJdbc;
import ec.mdd.generacion.aplicacion.PublicarEventoDePruebaServicio;
import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Ensambla el evento de prueba de generacion (D-48). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionGeneracion {

    @Bean
    public RepositorioEventosDePrueba repositorioEventosDePrueba(JdbcClient jdbc) {
        return new RepositorioEventosDePruebaJdbc(jdbc);
    }

    @Bean
    public PublicadorDeEventos publicadorDeEventos(ApplicationEventPublisher eventos) {
        return new PublicadorDeEventosSpring(eventos);
    }

    @Bean
    public PublicarEventoDePrueba publicarEventoDePrueba(
            RepositorioEventosDePrueba repositorio, PublicadorDeEventos publicador) {
        return new PublicarEventoDePruebaServicio(repositorio, publicador, Clock.systemUTC());
    }
}
