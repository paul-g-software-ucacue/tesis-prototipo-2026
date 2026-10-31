package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.adaptadores.entrada.api.ElementoDeFormacion;
import ec.mdd.docentes.adaptadores.entrada.api.FormacionDelDocente;
import ec.mdd.docentes.adaptadores.entrada.api.InternoApi;
import ec.mdd.docentes.adaptadores.entrada.api.TercerNivelDeFormacion;
import ec.mdd.docentes.dominio.FichaAcademica;
import ec.mdd.docentes.puertos.entrada.ConsultarFormacion;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Docentes entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta. Devuelve solo la formación: nada de contacto
 * ni de contrato, ni las asignaturas afines, que no suman (principio IX).
 */
@RestController
class InternoDocentesControlador implements InternoApi {

    private final ConsultarFormacion consultar;

    InternoDocentesControlador(ConsultarFormacion consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<FormacionDelDocente> consultarFormacion(String identificacion) {
        try {
            return ResponseEntity.ok(respuesta(consultar.formacion(identificacion)));
        } catch (DocenteNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
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
