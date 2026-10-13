package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.RegistroDeErrores;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Bean;

/**
 * Traducción de errores de un proceso (ADR-016): el registro con los catálogos de todos sus
 * servicios, el advice y la página {@code /error}.
 *
 * <p>En los procesos, el advice y la página de error ya los encuentra el escaneo de componentes
 * ({@code ec.mdd}); esta autoconfiguración los agrega solo si faltan, por ejemplo en las pruebas
 * con {@code @WebMvcTest} de un servicio. Se aplica antes que la página de error de Spring Boot,
 * que se retira al encontrar otro {@link ErrorController}.
 */
@AutoConfiguration(
        beforeName =
                "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ErroresAutoConfiguracion {

    /** Los catálogos de todos los servicios del proceso; falla si un código se repite. */
    @Bean
    @ConditionalOnMissingBean
    public RegistroDeErrores registroDeErrores(ObjectProvider<CatalogoDeErrores> catalogos) {
        return new RegistroDeErrores(catalogos.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public ManejadorDeErrores manejadorDeErrores(RegistroDeErrores registro) {
        return new ManejadorDeErrores(registro);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorController.class)
    public ControladorDeErrores controladorDeErrores() {
        return new ControladorDeErrores();
    }
}
