package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import ec.mdd.roles.adaptadores.entrada.api.Designacion;
import ec.mdd.roles.adaptadores.entrada.api.Responsable;
import ec.mdd.roles.adaptadores.entrada.api.ResponsablesApi;
import ec.mdd.roles.dominio.DesignacionInvalida;
import ec.mdd.roles.puertos.entrada.ConsultarResponsables;
import ec.mdd.roles.puertos.entrada.DesignarResponsable;
import ec.mdd.roles.puertos.entrada.PeriodoDesconocido;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * El Responsable de consolidación de cada periodo ({@code contratos/roles/openapi.yaml}, F05-H03,
 * D-13, ADR-003). Leer exige «Distributivo del periodo: lectura» y designar, «Distributivo del
 * periodo: escritura», como dice su {@code x-mdd-permiso} (ADR-021, D-69); {@code
 * PermisosRolesTest} lo comprueba.
 *
 * <p>Un periodo que Roles todavía no conoce responde 404 con el código común; un usuario que no se
 * puede designar, 400 con el detalle en {@code usuario}; un periodo cerrado, su 409.
 */
@RestController
class ResponsablesControlador implements ResponsablesApi {

    private static final String LECTURA = "hasAuthority('PERMISO_DISTRIBUTIVO:LECTURA')";
    private static final String ESCRITURA = "hasAuthority('PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final DesignarResponsable designar;
    private final ConsultarResponsables consultar;

    ResponsablesControlador(DesignarResponsable designar, ConsultarResponsables consultar) {
        this.designar = designar;
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Responsable>> listarResponsables() {
        return ResponseEntity.ok(
                consultar.responsables().stream().map(ResponsablesControlador::respuesta).toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Responsable> designarResponsable(
            String periodo, Designacion designacion) {
        try {
            return ResponseEntity.ok(
                    respuesta(designar.designar(periodo, designacion.getUsuario(), quien())));
        } catch (PeriodoDesconocido error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (DesignacionInvalida error) {
            throw new SolicitudInvalida(
                    List.of(new DetalleRespuesta(error.campo(), error.motivo())));
        }
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<List<String>> listarDocentes() {
        return ResponseEntity.ok(consultar.docentes());
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static Responsable respuesta(ec.mdd.roles.dominio.Designacion designacion) {
        return new Responsable(
                designacion.periodo(),
                designacion.usuario(),
                designacion.designadoEn().atOffset(ZoneOffset.UTC));
    }
}
