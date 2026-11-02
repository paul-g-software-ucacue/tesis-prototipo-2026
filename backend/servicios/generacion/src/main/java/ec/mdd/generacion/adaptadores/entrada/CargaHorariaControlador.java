package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.CargaHoraria;
import ec.mdd.generacion.adaptadores.entrada.api.CargaHorariaApi;
import ec.mdd.generacion.adaptadores.entrada.api.HorasPorComponente;
import ec.mdd.generacion.adaptadores.entrada.api.MetaDeProporcion;
import ec.mdd.generacion.adaptadores.entrada.api.Proporcion;
import ec.mdd.generacion.puertos.entrada.CargaDelDocente;
import ec.mdd.generacion.puertos.entrada.ConsultarCargaHoraria;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * La carga horaria de un docente (RF-27). A diferencia de las demás lecturas, no usa la carrera del
 * token: solo trae sumas (D-82).
 */
@RestController
class CargaHorariaControlador implements CargaHorariaApi {

    private static final String LEER =
            "hasAnyAuthority('PERMISO_OFERTA:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";

    private final ConsultarCargaHoraria consultar;

    CargaHorariaControlador(ConsultarCargaHoraria consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<CargaHoraria> consultarCargaHoraria(String periodo, String docente) {
        return ResponseEntity.ok(respuesta(consultar.carga(periodo, docente)));
    }

    /** Los totales de una carga, también para la lista de actividades. */
    static HorasPorComponente totales(ec.mdd.generacion.dominio.CargaHoraria carga) {
        return new HorasPorComponente(
                carga.clase(),
                carga.otrasDocencia(),
                carga.investigacion(),
                carga.vinculacion(),
                carga.gestion());
    }

    private static CargaHoraria respuesta(CargaDelDocente delDocente) {
        ec.mdd.generacion.dominio.CargaHoraria carga = delDocente.carga();
        BigDecimal limite = delDocente.limite();
        CargaHoraria respuesta =
                new CargaHoraria(
                        delDocente.docente(),
                        delDocente.nombreDocente(),
                        delDocente.horasContratadas(),
                        totales(carga),
                        carga.asignadas(),
                        carga.disponibles(limite),
                        carga.exceso(limite),
                        new MetaDeProporcion(
                                delDocente.meta().clase(), delDocente.meta().otrasDocencia()));
        carga.proporcion()
                .ifPresent(
                        proporcion ->
                                respuesta.setProporcion(
                                        new Proporcion(
                                                proporcion.clase(), proporcion.otrasDocencia())));
        return respuesta;
    }
}
