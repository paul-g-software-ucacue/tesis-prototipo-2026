package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.adaptadores.entrada.api.ActividadesDelPeriodo;
import ec.mdd.normativa.adaptadores.entrada.api.InternoApi;
import ec.mdd.normativa.adaptadores.entrada.api.PesosDeAfinidad;
import ec.mdd.normativa.adaptadores.entrada.api.PesosDelPeriodo;
import ec.mdd.normativa.adaptadores.entrada.api.ProporcionDelPeriodo;
import ec.mdd.normativa.adaptadores.entrada.api.ReglasDeAsignacion;
import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.PesosAfinidad;
import ec.mdd.normativa.puertos.entrada.ActividadesDeLaVersion;
import ec.mdd.normativa.puertos.entrada.ConsultarActividadesDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarPesosDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarProporcionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarReglasDeAsignacion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.PesosDeLaVersion;
import ec.mdd.normativa.puertos.entrada.ProporcionDeLaVersion;
import ec.mdd.normativa.puertos.entrada.ReglasDeLaVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Normativa entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta.
 */
@RestController
class InternoNormativaControlador implements InternoApi {

    private final ConsultarPesosDelPeriodo consultar;
    private final ConsultarActividadesDelPeriodo actividades;
    private final ConsultarProporcionDelPeriodo proporcion;
    private final ConsultarReglasDeAsignacion reglas;

    InternoNormativaControlador(
            ConsultarPesosDelPeriodo consultar,
            ConsultarActividadesDelPeriodo actividades,
            ConsultarProporcionDelPeriodo proporcion,
            ConsultarReglasDeAsignacion reglas) {
        this.consultar = consultar;
        this.actividades = actividades;
        this.proporcion = proporcion;
        this.reglas = reglas;
    }

    @Override
    public ResponseEntity<PesosDelPeriodo> consultarPesosDelPeriodo(String periodo) {
        try {
            PesosDeLaVersion pesos = consultar.pesos(periodo);
            return ResponseEntity.ok(
                    new PesosDelPeriodo(
                            periodo,
                            pesos.version(),
                            respuesta(pesos.pesos()),
                            pesos.minimo().valor()));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<ActividadesDelPeriodo> consultarActividadesDelPeriodo(String periodo) {
        try {
            ActividadesDeLaVersion catalogo = actividades.actividades(periodo);
            return ResponseEntity.ok(
                    new ActividadesDelPeriodo(
                            periodo,
                            catalogo.version(),
                            catalogo.actividades().stream()
                                    .map(InternoNormativaControlador::respuesta)
                                    .toList()));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<ProporcionDelPeriodo> consultarProporcionDelPeriodo(String periodo) {
        try {
            ProporcionDeLaVersion meta = proporcion.proporcion(periodo);
            return ResponseEntity.ok(
                    new ProporcionDelPeriodo(
                            periodo, meta.version(), meta.clase(), meta.otrasDocencia()));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<ReglasDeAsignacion> consultarReglasDeAsignacion(String periodo) {
        try {
            ReglasDeLaVersion deLaVersion = reglas.reglas(periodo);
            return ResponseEntity.ok(
                    new ReglasDeAsignacion(
                            periodo,
                            deLaVersion.version(),
                            deLaVersion.horasMaximasDocente(),
                            deLaVersion.horasClaseConProyecto()));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static ec.mdd.normativa.adaptadores.entrada.api.ActividadDelCatalogo respuesta(
            ActividadDelCatalogo actividad) {
        return new ec.mdd.normativa.adaptadores.entrada.api.ActividadDelCatalogo(
                actividad.codigo(),
                actividad.nombre(),
                ec.mdd.normativa.adaptadores.entrada.api.ActividadDelCatalogo.ComponenteEnum
                        .fromValue(actividad.componente().name()),
                actividad.tiposDeProyecto(),
                actividad.cargos());
    }

    private static PesosDeAfinidad respuesta(PesosAfinidad pesos) {
        return new PesosDeAfinidad(
                pesos.cuartoNivelCampoEspecifico(),
                pesos.cuartoNivelCampoAmplio(),
                pesos.tercerNivelCampoEspecifico(),
                pesos.tercerNivelCampoAmplio(),
                pesos.experienciaRelacionada(),
                pesos.cursoRelacionado(),
                pesos.topeCursos());
    }
}
