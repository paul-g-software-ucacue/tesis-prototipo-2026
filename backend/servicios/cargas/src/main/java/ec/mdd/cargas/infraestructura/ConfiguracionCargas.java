package ec.mdd.cargas.infraestructura;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import ec.mdd.cargas.aplicacion.ConsultarPlantillasServicio;
import ec.mdd.cargas.aplicacion.ValidarArchivoServicio;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
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

    @Bean
    public LectorDeArchivo lectorDeArchivo() {
        return new LectorPorFormato();
    }

    @Bean
    public ValidarArchivo validarArchivo(
            ConsultarPlantillas consultarPlantillas, LectorDeArchivo lectorDeArchivo) {
        return new ValidarArchivoServicio(consultarPlantillas, lectorDeArchivo);
    }
}
