package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.adaptadores.entrada.api.Dedicacion;
import ec.mdd.docentes.adaptadores.entrada.api.DocenteActivo;
import ec.mdd.docentes.adaptadores.entrada.api.ElementoDeFormacion;
import ec.mdd.docentes.adaptadores.entrada.api.FormacionDelDocente;
import ec.mdd.docentes.adaptadores.entrada.api.InternoApi;
import ec.mdd.docentes.adaptadores.entrada.api.TercerNivelDeFormacion;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.FichaAcademica;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentesActivos;
import ec.mdd.docentes.puertos.entrada.ConsultarFormacion;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Docentes entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta. Devuelven solo lo necesario: nada de contacto
 * ni de contrato, ni las asignaturas afines, que no suman (principio IX).
 */
@RestController
class InternoDocentesControlador implements InternoApi {

    private final ConsultarFormacion consultar;
    private final ConsultarDocentesActivos activos;

    InternoDocentesControlador(ConsultarFormacion consultar, ConsultarDocentesActivos activos) {
        this.consultar = consultar;
        this.activos = activos;
    }

    @Override
    public ResponseEntity<FormacionDelDocente> consultarFormacion(String identificacion) {
        try {
            return ResponseEntity.ok(respuesta(consultar.formacion(identificacion)));
        } catch (DocenteNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<DocenteActivo>> consultarDocentesActivos() {
        return ResponseEntity.ok(
                activos.activos().stream().map(InternoDocentesControlador::respuesta).toList());
    }

    private static DocenteActivo respuesta(Docente docente) {
        return new DocenteActivo(
                docente.identificacion().numero(),
                docente.datos().nombreCompleto(),
                Dedicacion.fromValue(docente.datos().dedicacion().name()),
                docente.datos().horasContratadas(),
                respuesta(docente.datos().ficha()));
    }

    private static FormacionDelDocente respuesta(FichaAcademica ficha) {
        return new FormacionDelDocente(
                new TercerNivelDeFormacion(
                        ficha.tercerNivel().titulo(),
                        ficha.tercerNivel().campo().amplio(),
                        ficha.tercerNivel().campo().especifico()),
                ficha.cuartoNivel().stream()
                        .map(
                                titulo ->
                                        new ElementoDeFormacion(
                                                titulo.titulo(), titulo.campoEspecifico()))
                        .toList(),
                ficha.experiencia().stream()
                        .map(
                                cargo ->
                                        new ElementoDeFormacion(
                                                cargo.cargo(), cargo.campoEspecifico()))
                        .toList(),
                ficha.cursos().stream()
                        .map(
                                curso ->
                                        new ElementoDeFormacion(
                                                curso.nombre(), curso.campoEspecifico()))
                        .toList());
    }
}
