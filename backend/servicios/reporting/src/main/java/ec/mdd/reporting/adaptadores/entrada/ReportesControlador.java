package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.reporting.adaptadores.entrada.api.DocentesDelPeriodo;
import ec.mdd.reporting.adaptadores.entrada.api.Publicacion;
import ec.mdd.reporting.adaptadores.entrada.api.ReporteIndividual;
import ec.mdd.reporting.adaptadores.entrada.api.ReportesApi;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los reportes de la versión publicada vigente, para la pantalla «Reportes» (F20-H01, RF-45). Ver
 * el reporte no deja entrada en la bitácora; las descargas sí ({@code DescargasControlador}).
 */
@RestController
class ReportesControlador implements ReportesApi {

    private static final String PERMISO = "hasAuthority('PERMISO_REPORTES:LECTURA')";

    private final ConsultarReportes consultar;

    ReportesControlador(ConsultarReportes consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<List<Publicacion>> listarPublicaciones() {
        return ResponseEntity.ok(
                consultar.publicaciones().stream()
                        .map(RespuestasDeReporting::publicacion)
                        .toList());
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<DocentesDelPeriodo> listarDocentesDelPeriodo(String periodo) {
        return ResponseEntity.ok(RespuestasDeReporting.docentes(consultar.docentes(periodo)));
    }

    @Override
    @PreAuthorize(PERMISO)
    public ResponseEntity<ReporteIndividual> consultarReporteIndividual(
            String periodo, String identificacion) {
        return ResponseEntity.ok(
                RespuestasDeReporting.reporte(consultar.reporte(periodo, identificacion, quien())));
    }

    /** Quién pide: el usuario del token. */
    static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
