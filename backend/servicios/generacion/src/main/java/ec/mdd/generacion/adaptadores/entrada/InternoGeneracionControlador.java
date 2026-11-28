package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.ActividadCongelada;
import ec.mdd.generacion.adaptadores.entrada.api.AsignaturaCongelada;
import ec.mdd.generacion.adaptadores.entrada.api.CargaCongeladaDelDocente;
import ec.mdd.generacion.adaptadores.entrada.api.CargasCongeladas;
import ec.mdd.generacion.adaptadores.entrada.api.Componente;
import ec.mdd.generacion.adaptadores.entrada.api.ContenidoCongeladoCompleto;
import ec.mdd.generacion.adaptadores.entrada.api.DatosDelDocenteCongelado;
import ec.mdd.generacion.adaptadores.entrada.api.Dedicacion;
import ec.mdd.generacion.adaptadores.entrada.api.DocenteCongelado;
import ec.mdd.generacion.adaptadores.entrada.api.HorasPorComponente;
import ec.mdd.generacion.adaptadores.entrada.api.InconsistenciasAlCongelar;
import ec.mdd.generacion.adaptadores.entrada.api.InternoApi;
import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado;
import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado.CargaDeLaVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Generación entre procesos (F20-H01, D-115, ADR-023). Solo con el token de
 * proceso, por la ruta {@code /interno/**}; el API Gateway no las enruta. Un 404 dice que la
 * versión no existe o no está congelada, o que el docente no está en ella.
 */
@RestController
class InternoGeneracionControlador implements InternoApi {

    private final ConsultarContenidoCongelado contenido;

    InternoGeneracionControlador(ConsultarContenidoCongelado contenido) {
        this.contenido = contenido;
    }

    @Override
    public ResponseEntity<CargasCongeladas> consultarCargasCongeladas(
            String periodo, Integer numero) {
        return contenido
                .cargas(periodo, numero)
                .map(
                        cargas ->
                                new CargasCongeladas(
                                        cargas.hash(),
                                        cargas.docentes().stream()
                                                .map(InternoGeneracionControlador::docente)
                                                .toList()))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<CargaCongeladaDelDocente> consultarCargaCongelada(
            String periodo, Integer numero, String identificacion) {
        return contenido
                .carga(periodo, numero, identificacion)
                .map(InternoGeneracionControlador::respuesta)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // F19-H01 (D-119): el contenido de todos los docentes en una llamada, para el CSV para SIGA.
    @Override
    public ResponseEntity<ContenidoCongeladoCompleto> consultarContenidoCongelado(
            String periodo, Integer numero) {
        return contenido
                .contenido(periodo, numero)
                .map(
                        completo -> {
                            ContenidoCongeladoCompleto respuesta =
                                    new ContenidoCongeladoCompleto(
                                            completo.hash(),
                                            completo.cargas().stream()
                                                    .map(
                                                            carga ->
                                                                    respuesta(
                                                                            new CargaDeLaVersion(
                                                                                    completo.hash(),
                                                                                    carga)))
                                                    .toList());
                            // F19-H02 (D-120): solo si se registraron al congelar.
                            completo.inconsistencias()
                                    .ifPresent(
                                            abiertas ->
                                                    respuesta.setInconsistenciasAbiertas(
                                                            new InconsistenciasAlCongelar(
                                                                    abiertas.duras(),
                                                                    abiertas.blandas())));
                            return respuesta;
                        })
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // F20-H02 (D-117): las horas y la dedicación de cada docente, para el reporte general.
    private static DocenteCongelado docente(DocenteDeLaVersion docente) {
        DocenteCongelado respuesta =
                new DocenteCongelado(
                        docente.identificacion(), docente.nombre(), horas(docente.horas()));
        docente.dedicacion()
                .ifPresent(una -> respuesta.dedicacion(Dedicacion.fromValue(una.name())));
        return respuesta;
    }

    private static HorasPorComponente horas(CargaHoraria horas) {
        return new HorasPorComponente(
                horas.clase(),
                horas.otrasDocencia(),
                horas.investigacion(),
                horas.vinculacion(),
                horas.gestion());
    }

    private static CargaCongeladaDelDocente respuesta(CargaDeLaVersion deLaVersion) {
        CargaCongelada carga = deLaVersion.carga();
        DatosDelDocenteCongelado docente =
                new DatosDelDocenteCongelado(carga.identificacion(), carga.nombre());
        carga.tipoIdentificacion()
                .ifPresent(
                        una ->
                                docente.tipoIdentificacion(
                                        ec.mdd.generacion.adaptadores.entrada.api
                                                .TipoDeIdentificacion.fromValue(una.name())));
        carga.dedicacion().ifPresent(una -> docente.dedicacion(Dedicacion.fromValue(una.name())));
        carga.horasContratadas().ifPresent(docente::horasContratadas);
        return new CargaCongeladaDelDocente(
                deLaVersion.hash(),
                docente,
                carga.asignaturas().stream()
                        .map(
                                una ->
                                        new AsignaturaCongelada(
                                                una.codigo(),
                                                una.nombre(),
                                                una.paralelo(),
                                                una.carrera(),
                                                una.nombreCarrera(),
                                                una.horas()))
                        .toList(),
                carga.actividades().stream()
                        .map(
                                una ->
                                        new ActividadCongelada(
                                                        una.codigo(),
                                                        una.nombre(),
                                                        Componente.fromValue(
                                                                una.componente().name()),
                                                        una.carrera(),
                                                        una.horas())
                                                .proyecto(una.proyecto())
                                                .nombreProyecto(una.nombreProyecto()))
                        .toList(),
                horas(carga.horas()));
    }
}
