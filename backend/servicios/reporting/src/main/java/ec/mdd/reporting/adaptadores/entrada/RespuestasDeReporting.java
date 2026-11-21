package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.reporting.adaptadores.entrada.api.Componente;
import ec.mdd.reporting.adaptadores.entrada.api.DatosDelDocente;
import ec.mdd.reporting.adaptadores.entrada.api.Dedicacion;
import ec.mdd.reporting.adaptadores.entrada.api.DocenteDeLaVersion;
import ec.mdd.reporting.adaptadores.entrada.api.DocentesDelPeriodo;
import ec.mdd.reporting.adaptadores.entrada.api.FilaDeActividad;
import ec.mdd.reporting.adaptadores.entrada.api.FilaDeAsignatura;
import ec.mdd.reporting.adaptadores.entrada.api.HorasPorComponente;
import ec.mdd.reporting.adaptadores.entrada.api.Publicacion;
import ec.mdd.reporting.adaptadores.entrada.api.ReporteIndividual;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;

/**
 * Pasa los tipos del dominio de Reporting a los del contrato (principio IV). Los nombres coinciden
 * con los del dominio, así que estos se escriben con su paquete.
 */
final class RespuestasDeReporting {

    private RespuestasDeReporting() {}

    static Publicacion publicacion(ec.mdd.reporting.dominio.Publicacion publicacion) {
        return new Publicacion(
                publicacion.periodo(),
                publicacion.version(),
                publicacion.hash(),
                publicacion.publicadaEn().atOffset(ZoneOffset.UTC),
                publicacion.publicadaPor(),
                publicacion.enReforma());
    }

    static DocentesDelPeriodo docentes(ConsultarReportes.DocentesDelPeriodo docentes) {
        return new DocentesDelPeriodo(
                publicacion(docentes.publicacion()),
                docentes.docentes().stream()
                        .map(una -> new DocenteDeLaVersion(una.identificacion(), una.nombre()))
                        .toList());
    }

    static ReporteIndividual reporte(ec.mdd.reporting.dominio.ReporteIndividual reporte) {
        DatosDelDocente docente =
                new DatosDelDocente(reporte.docente().identificacion(), reporte.docente().nombre());
        reporte.docente()
                .dedicacion()
                .ifPresent(una -> docente.dedicacion(Dedicacion.fromValue(una.name())));
        reporte.docente().horasContratadas().ifPresent(docente::horasContratadas);
        ec.mdd.reporting.dominio.HorasPorComponente horas = reporte.horas();
        return new ReporteIndividual(
                publicacion(reporte.publicacion()),
                docente,
                reporte.asignaturas().stream()
                        .map(
                                una ->
                                        new FilaDeAsignatura(
                                                una.carrera(),
                                                una.codigo(),
                                                una.nombre(),
                                                una.paralelo(),
                                                horas(una.horas())))
                        .toList(),
                reporte.actividades().stream()
                        .map(
                                una ->
                                        new FilaDeActividad(
                                                        Componente.fromValue(
                                                                una.componente().name()),
                                                        una.codigo(),
                                                        una.nombre(),
                                                        horas(una.horas()))
                                                .unidad(una.unidad().orElse(null)))
                        .toList(),
                new HorasPorComponente(
                        horas(horas.clase()),
                        horas(horas.otrasDocencia()),
                        horas(horas.investigacion()),
                        horas(horas.vinculacion()),
                        horas(horas.gestion()),
                        horas(horas.total())),
                reporte.generadoPor(),
                reporte.generadoEn().atOffset(ZoneOffset.UTC));
    }

    // Con dos decimales, como el PDF y el CSV. El dominio ya exige que no tengan más.
    private static BigDecimal horas(BigDecimal horas) {
        return horas.setScale(2, RoundingMode.UNNECESSARY);
    }
}
