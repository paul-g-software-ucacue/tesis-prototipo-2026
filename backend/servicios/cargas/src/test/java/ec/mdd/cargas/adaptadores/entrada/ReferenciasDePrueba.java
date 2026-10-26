package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.cargas.adaptadores.salida.DatosDePrueba;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Los puertos publicados de Académico y de Docentes para las pruebas de contrato de Cargas,
 * escritos a mano (principio VIII): lo que existiría con las cargas 01 a 06 de prueba confirmadas.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ReferenciasDePrueba {

    @Bean
    ConsultarReferencias consultarReferencias() {
        LocalDate dia = LocalDate.of(2026, 4, 1);
        return () ->
                new ReferenciasDeAcademico(
                        columna("02_carreras.csv", "codigo_carrera").stream()
                                .map(codigo -> new CarreraDeReferencia(codigo, "CARRERA", "C"))
                                .toList(),
                        columna("04_asignaturas_malla.csv", "codigo_asignatura").stream()
                                .map(codigo -> new AsignaturaDeReferencia(codigo, "A", "C"))
                                .toList(),
                        List.of(),
                        columna("01_estructura_organica.csv", "codigo_unidad").stream()
                                .map(
                                        codigo ->
                                                new UnidadDeReferencia(
                                                        codigo, "U", "GESTION", null, true))
                                .toList(),
                        columna("03_ciclos_academicos.csv", "codigo_periodo").stream()
                                .map(codigo -> new PeriodoDeReferencia(codigo, "P", dia, dia))
                                .toList());
    }

    @Bean
    ConsultarIdentificaciones consultarIdentificaciones() {
        Set<String> docentes = columna("06_docentes.csv", "numero_identificacion");
        return () -> docentes;
    }

    private static Set<String> columna(String archivo, String campo) {
        TablaDeDatos tabla =
                new LectorPorFormato().leer(archivo, DatosDePrueba.contenido(archivo)).tabla();
        int indice = tabla.encabezados().indexOf(campo);
        return tabla.filas().stream()
                .map(fila -> fila.get(indice))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }
}
