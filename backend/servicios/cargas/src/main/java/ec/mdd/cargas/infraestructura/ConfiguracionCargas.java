package ec.mdd.cargas.infraestructura;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.aplicacion.ConsultarPlantillasServicio;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla Cargas: el diccionario de datos versionado y la consulta de las plantillas (F01-H01). Si
 * el diccionario no está completo, el proceso no arranca y dice qué falta.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionCargas {

    @Bean
    public FuenteDelDiccionario fuenteDelDiccionario() {
        return new DiccionarioYaml();
    }

    @Bean
    public ConsultarPlantillas consultarPlantillas(FuenteDelDiccionario fuenteDelDiccionario) {
        return new ConsultarPlantillasServicio(fuenteDelDiccionario);
    }
}
