package ec.mdd.plataforma.eventos;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import java.time.Clock;
import java.util.concurrent.Executor;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entrega de eventos entre procesos para un proceso que publica (principio V, ADR-018).
 *
 * <p>Arma la migración del registro de publicaciones en {@code eventos_<proceso>}, el token de
 * proceso, el cliente, el oyente y la tarea de reenvío. El registro y su serialización los pone
 * Spring Modulith ({@code spring-modulith-events-jdbc} y {@code -jackson}). Activa la programación
 * de tareas: la usan la tarea de reenvío y el monitor de publicaciones atascadas.
 *
 * <p>Una clave de proceso vacía no impide arrancar: cada entrega falla con un aviso y la
 * publicación queda pendiente hasta que se configure.
 */
// Antes que la ejecución de tareas de Spring Boot: su AsyncConfigurer se retira si hay otro, y el
// de esta configuración usa igual su ejecutor (applicationTaskExecutor).
@AutoConfiguration(
        afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        beforeName = "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration")
@EnableConfigurationProperties(PropiedadesEventos.class)
@EnableScheduling
public class EventosAutoConfiguracion {

    // El ejecutor que Spring Boot crea para las tareas asíncronas de la aplicación.
    static final String EJECUTOR_DE_SPRING_BOOT = "applicationTaskExecutor";

    /**
     * Migra el registro de publicaciones solo en el esquema de eventos del proceso, con su propio
     * historial de migraciones, como el Flyway de cada servicio (ADR-013).
     */
    @Bean(initMethod = "migrate")
    public Flyway flywayEventos(DataSource dataSource, Environment entorno) {
        String esquema = EsquemaDeEventos.validar(entorno.getProperty(EsquemaDeEventos.PROPIEDAD));
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(esquema)
                .locations("classpath:db/migration/eventos")
                .load();
    }

    @Bean
    public ProveedorDeTokenDeProceso proveedorDeTokenDeProceso(PropiedadesEventos propiedades) {
        return new ProveedorDeTokenDeProceso(
                Clientes.http(propiedades.plazo()),
                propiedades.tokenProceso(),
                propiedades.proceso(),
                propiedades.clave(),
                Clock.systemUTC());
    }

    @Bean
    public ClienteDeEventos clienteDeEventos(
            PropiedadesEventos propiedades, ProveedorDeTokenDeProceso tokens) {
        return new ClienteDeEventos(
                Clientes.http(propiedades.plazo()), propiedades.destinos(), tokens);
    }

    @Bean
    public ConsultasEntreProcesos consultasEntreProcesos(
            PropiedadesEventos propiedades, ProveedorDeTokenDeProceso tokens) {
        return new ConsultasEntreProcesos(Clientes.http(propiedades.plazo()), tokens);
    }

    @Bean
    public CatalogoDeErrores erroresEntreProcesos() {
        return new ErroresEntreProcesos();
    }

    @Bean
    public OyenteEntreProcesos oyenteEntreProcesos(ClienteDeEventos cliente) {
        return new OyenteEntreProcesos(cliente);
    }

    /**
     * Las entregas fallidas no repiten su traza en ERROR (ver {@link FallosAsincronos}), con el
     * ejecutor de Spring Boot. Solo si el proceso no configuró su propio manejo asíncrono.
     */
    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public AsyncConfigurer configuracionAsincronaDeEventos(BeanFactory fabrica) {
        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return fabrica.containsBean(EJECUTOR_DE_SPRING_BOOT)
                        ? fabrica.getBean(EJECUTOR_DE_SPRING_BOOT, Executor.class)
                        : null;
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return new FallosAsincronos();
            }
        };
    }

    @Bean
    public ReenvioDePublicaciones reenvioDePublicaciones(FailedEventPublications fallidas) {
        return new ReenvioDePublicaciones(fallidas);
    }
}
