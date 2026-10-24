package ec.mdd.docentes.infraestructura;

import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.docentes.adaptadores.salida.AcademicoDesdePuertos;
import ec.mdd.docentes.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioDocentesJdbc;
import ec.mdd.docentes.adaptadores.salida.TransaccionSpring;
import ec.mdd.docentes.aplicacion.ConsultarDocentesServicio;
import ec.mdd.docentes.aplicacion.MantenerDocentesServicio;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.Transaccion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Docentes (F03-H04): el repositorio del esquema {@code docentes}, su registro de cambios,
 * la transacción, las referencias de Académico pedidas por su interfaz publicada (research §4) y
 * los casos de uso. Los nombres de los beans no se repiten con los de Académico y Normativa, que
 * viven en el mismo proceso.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionDocentes {

    @Bean
    @DependsOn("flywayDocentes")
    public RepositorioDocentes repositorioDocentes(JdbcClient jdbc) {
        return new RepositorioDocentesJdbc(jdbc);
    }

    @Bean
    public RegistroDeCambios registroDeCambiosDocentes(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionDocentes(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Las carreras, las asignaturas y los campos amplios de Académico (research §4). */
    @Bean
    public ReferenciasAcademicas referenciasAcademicas(ConsultarReferencias consultar) {
        return new AcademicoDesdePuertos(consultar);
    }

    /** Buscar y consultar los docentes con su ficha (FR-007, FR-008). */
    @Bean
    public ConsultarDocentes consultarDocentes(
            RepositorioDocentes repositorio, ReferenciasAcademicas referencias) {
        return new ConsultarDocentesServicio(repositorio, referencias);
    }

    /**
     * Registrar, corregir y cambiar el estado de los docentes, con su registro (FR-003, FR-010).
     */
    @Bean
    public MantenerDocentes mantenerDocentes(
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registroDeCambiosDocentes,
            Transaccion transaccionDocentes) {
        return new MantenerDocentesServicio(
                repositorio,
                referencias,
                registroDeCambiosDocentes,
                transaccionDocentes,
                Clock.systemUTC());
    }
}
