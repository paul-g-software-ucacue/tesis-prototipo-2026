package ec.mdd.cargas.infraestructura;

import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import ec.mdd.cargas.adaptadores.salida.ReferenciasDesdePuertos;
import ec.mdd.cargas.aplicacion.ConsultarPlantillasServicio;
import ec.mdd.cargas.aplicacion.ValidarArchivoServicio;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla Cargas: el diccionario de datos versionado y la consulta de las plantillas (F01-H01), la
 * subida (F01-H02) y la revisión de las filas con las referencias de Académico y Docentes
 * (F01-H03). Si el diccionario no está completo, el proceso no arranca y dice qué falta.
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

    /** Las referencias de las filas, por las interfaces publicadas de Académico y Docentes. */
    @Bean
    public ReferenciasDelSistema referenciasDelSistema(
            ConsultarReferencias consultarReferencias,
            ConsultarIdentificaciones consultarIdentificaciones) {
        return new ReferenciasDesdePuertos(consultarReferencias, consultarIdentificaciones);
    }

    @Bean
    public ValidarArchivo validarArchivo(
            ConsultarPlantillas consultarPlantillas,
            LectorDeArchivo lectorDeArchivo,
            ReferenciasDelSistema referenciasDelSistema) {
        return new ValidarArchivoServicio(
                consultarPlantillas, lectorDeArchivo, referenciasDelSistema);
    }
}
