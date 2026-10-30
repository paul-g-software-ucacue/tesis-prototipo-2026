package ec.mdd.cargas.infraestructura;

import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import ec.mdd.cargas.adaptadores.salida.ProyectosDesdePuertos;
import ec.mdd.cargas.adaptadores.salida.ReferenciasDesdePuertos;
import ec.mdd.cargas.aplicacion.ConfirmarCargaServicio;
import ec.mdd.cargas.aplicacion.ConfirmarSinProyectosServicio;
import ec.mdd.cargas.aplicacion.ConsultarEstadoDeLasCargasServicio;
import ec.mdd.cargas.aplicacion.ConsultarHistorialServicio;
import ec.mdd.cargas.aplicacion.ConsultarPlantillasServicio;
import ec.mdd.cargas.aplicacion.ConsultarPreparacionServicio;
import ec.mdd.cargas.aplicacion.DuenosDeLasCargas;
import ec.mdd.cargas.aplicacion.ValidarArchivoServicio;
import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.puertos.entrada.ConfirmarCarga;
import ec.mdd.cargas.puertos.entrada.ConfirmarSinProyectos;
import ec.mdd.cargas.puertos.entrada.ConsultarEstadoDeLasCargas;
import ec.mdd.cargas.puertos.entrada.ConsultarHistorial;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ConsultarPreparacion;
import ec.mdd.cargas.puertos.salida.FuenteDeDatos;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.cargas.puertos.salida.Transaccion;
import ec.mdd.docentes.puertos.entrada.ConsultarAsignablesDelPeriodo;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Clock;
import java.util.List;
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
    public FuenteDeDatos fuenteDeDatos() {
        return new LectorPorFormato();
    }

    /** Las referencias de las filas, por las interfaces publicadas de Académico y Docentes. */
    @Bean
    public ReferenciasDelSistema referenciasDelSistema(
            ConsultarReferencias consultarReferencias,
            ConsultarIdentificaciones consultarIdentificaciones) {
        return new ReferenciasDesdePuertos(consultarReferencias, consultarIdentificaciones);
    }

    /**
     * El dueño de cada carga (MD-13, ADR-022): los aplicadores que publican Académico, Docentes y
     * Normativa. Si una de las 10 no tiene uno, o tiene dos, el proceso no arranca.
     */
    @Bean
    public DuenosDeLasCargas duenosDeLasCargas(
            List<AplicadorDeCargas> aplicadores, ConsultarPlantillas consultarPlantillas) {
        return new DuenosDeLasCargas(
                aplicadores,
                consultarPlantillas.diccionario().cargas().stream().map(Carga::codigo).toList());
    }

    @Bean
    public ValidarArchivoServicio validarArchivo(
            ConsultarPlantillas consultarPlantillas,
            FuenteDeDatos fuenteDeDatos,
            ReferenciasDelSistema referenciasDelSistema,
            DuenosDeLasCargas duenosDeLasCargas) {
        return new ValidarArchivoServicio(
                consultarPlantillas, fuenteDeDatos, referenciasDelSistema, duenosDeLasCargas);
    }

    @Bean
    public ConfirmarCarga confirmarCarga(
            ValidarArchivoServicio validarArchivo,
            DuenosDeLasCargas duenosDeLasCargas,
            HistorialDeCargas historialDeCargas,
            Transaccion transaccionCargas) {
        return new ConfirmarCargaServicio(
                validarArchivo,
                duenosDeLasCargas,
                historialDeCargas,
                transaccionCargas,
                Clock.systemUTC());
    }

    /** Si un periodo tiene proyectos asignables, por el puerto publicado de Docentes (F01-H05). */
    @Bean
    public ProyectosDelPeriodo proyectosDelPeriodo(ConsultarAsignablesDelPeriodo consultar) {
        return new ProyectosDesdePuertos(consultar);
    }

    /** El estado de las 10 cargas (F01-H05). */
    @Bean
    public ConsultarEstadoDeLasCargas consultarEstadoDeLasCargas(
            ConsultarPlantillas consultarPlantillas,
            DuenosDeLasCargas duenosDeLasCargas,
            HistorialDeCargas historialDeCargas,
            ProyectosDelPeriodo proyectosDelPeriodo) {
        return new ConsultarEstadoDeLasCargasServicio(
                consultarPlantillas, duenosDeLasCargas, historialDeCargas, proyectosDelPeriodo);
    }

    /** El tablero de preparación de un periodo (F02-H02). */
    @Bean
    public ConsultarPreparacion consultarPreparacion(
            ConsultarPlantillas consultarPlantillas,
            DuenosDeLasCargas duenosDeLasCargas,
            HistorialDeCargas historialDeCargas,
            ProyectosDelPeriodo proyectosDelPeriodo,
            ReferenciasDelSistema referenciasDelSistema) {
        return new ConsultarPreparacionServicio(
                consultarPlantillas,
                duenosDeLasCargas,
                historialDeCargas,
                proyectosDelPeriodo,
                referenciasDelSistema);
    }

    /** La confirmación «sin proyectos» de un periodo (F01-H05). */
    @Bean
    public ConfirmarSinProyectos confirmarSinProyectos(
            ConsultarPlantillas consultarPlantillas,
            ProyectosDelPeriodo proyectosDelPeriodo,
            HistorialDeCargas historialDeCargas) {
        return new ConfirmarSinProyectosServicio(
                consultarPlantillas, proyectosDelPeriodo, historialDeCargas, Clock.systemUTC());
    }

    @Bean
    public ConsultarHistorial consultarHistorial(
            ConsultarPlantillas consultarPlantillas, HistorialDeCargas historialDeCargas) {
        return new ConsultarHistorialServicio(consultarPlantillas, historialDeCargas);
    }
}
