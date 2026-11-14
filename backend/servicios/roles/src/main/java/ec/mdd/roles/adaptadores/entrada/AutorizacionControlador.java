package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.roles.adaptadores.entrada.api.Autorizacion;
import ec.mdd.roles.adaptadores.entrada.api.AutorizacionApi;
import ec.mdd.roles.adaptadores.entrada.api.Firmantes;
import ec.mdd.roles.adaptadores.entrada.api.ResponsableDelPeriodo;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarFirmantes;
import ec.mdd.roles.puertos.entrada.ConsultarResponsable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Roles y permisos efectivos de un usuario para otro proceso ({@code contratos/roles/openapi.yaml},
 * ADR-018, ADR-021). Solo lo alcanza un token de proceso ({@code /interno/**}); dentro de {@code
 * identidad}, Usuarios usa directamente {@link ConsultarAutorizacion}.
 */
@RestController
class AutorizacionControlador implements AutorizacionApi {

    private final ConsultarAutorizacion consultar;
    private final ConsultarFirmantes firmantes;
    private final ConsultarResponsable responsable;

    AutorizacionControlador(
            ConsultarAutorizacion consultar,
            ConsultarFirmantes firmantes,
            ConsultarResponsable responsable) {
        this.consultar = consultar;
        this.firmantes = firmantes;
        this.responsable = responsable;
    }

    @Override
    public ResponseEntity<Firmantes> consultarFirmantes(String periodo) {
        ec.mdd.roles.puertos.entrada.Firmantes delPeriodo = firmantes.firmantes(periodo);
        return ResponseEntity.ok(
                new Firmantes(delPeriodo.rolesConUsuario(), delPeriodo.responsableDesignado()));
    }

    // F13-H02: para el bloque de firmas del documento firmable.
    @Override
    public ResponseEntity<ResponsableDelPeriodo> consultarResponsableDelPeriodo(String periodo) {
        ec.mdd.roles.puertos.entrada.ResponsableDelPeriodo del = responsable.responsable(periodo);
        return ResponseEntity.ok(new ResponsableDelPeriodo(del.usuario(), del.nombre()));
    }

    @Override
    public ResponseEntity<Autorizacion> consultarAutorizacion(String usuario) {
        ec.mdd.roles.puertos.entrada.Autorizacion autorizacion = consultar.consultar(usuario);
        Autorizacion respuesta =
                new Autorizacion(
                        autorizacion.usuario(), autorizacion.roles(), autorizacion.permisos());
        respuesta.setCarrera(autorizacion.carrera());
        return ResponseEntity.ok(respuesta);
    }
}
