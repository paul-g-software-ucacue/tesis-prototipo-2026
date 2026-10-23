package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.DatosDeUnidad;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeUnidad;
import ec.mdd.academico.adaptadores.entrada.api.EstructuraApi;
import ec.mdd.academico.adaptadores.entrada.api.GestionAutorizada;
import ec.mdd.academico.adaptadores.entrada.api.TipoDeUnidad;
import ec.mdd.academico.adaptadores.entrada.api.UnidadNueva;
import ec.mdd.academico.adaptadores.entrada.api.UnidadOrganica;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.puertos.entrada.ConsultarEstructura;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * La estructura orgánica ({@code contratos/academico/openapi.yaml}, RF-12, F03-H01): consultarla
 * con el catálogo de gestiones autorizadas, registrar, corregir, inactivar y reactivar unidades.
 * Mantenerla exige «Catálogos y datos maestros: escritura»; leerla, «Distributivo del periodo:
 * lectura» o «Catálogos y datos maestros: lectura», como dice su {@code x-mdd-permiso} (ADR-021,
 * D-70); {@code PermisosAcademicoTest} lo comprueba.
 *
 * <p>Una unidad que no existe responde 404 con el código común. Un dato inválido responde 400 con
 * su campo, en español. Las reglas del catálogo responden su 409 (research §5).
 */
@RestController
class EstructuraControlador implements EstructuraApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA', 'PERMISO_CATALOGOS:LECTURA')";
    private static final String ESCRITURA = "hasAuthority('PERMISO_CATALOGOS:ESCRITURA')";

    private final ConsultarEstructura consultar;
    private final MantenerEstructura mantener;

    EstructuraControlador(ConsultarEstructura consultar, MantenerEstructura mantener) {
        this.consultar = consultar;
        this.mantener = mantener;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<GestionAutorizada>> listarGestionesAutorizadas() {
        return ResponseEntity.ok(
                consultar.gestionesAutorizadas().stream()
                        .map(gestion -> new GestionAutorizada(gestion.codigo(), gestion.nombre()))
                        .toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<UnidadOrganica>> listarUnidades() {
        return ResponseEntity.ok(
                consultar.unidades().stream().map(EstructuraControlador::respuesta).toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<UnidadOrganica> consultarUnidad(String codigo) {
        return ResponseEntity.ok(respuesta(existente(() -> consultar.unidad(codigo))));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<UnidadOrganica> registrarUnidad(UnidadNueva unidadNueva) {
        ec.mdd.academico.dominio.UnidadOrganica registrada =
                valida(
                        () ->
                                mantener.registrar(
                                        unidadNueva.getCodigo(),
                                        unidadNueva.getNombre(),
                                        ec.mdd.academico.dominio.TipoDeUnidad.valueOf(
                                                unidadNueva.getTipo().getValue()),
                                        unidadNueva.getPadre(),
                                        unidadNueva.getGestionAutorizada(),
                                        unidadNueva.getComisionPermanente(),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(registrada));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<UnidadOrganica> corregirUnidad(
            String codigo, DatosDeUnidad datosDeUnidad) {
        return ResponseEntity.ok(
                respuesta(
                        existente(
                                () ->
                                        valida(
                                                () ->
                                                        mantener.corregir(
                                                                codigo,
                                                                datosDeUnidad.getNombre(),
                                                                datosDeUnidad.getPadre(),
                                                                datosDeUnidad
                                                                        .getGestionAutorizada(),
                                                                datosDeUnidad
                                                                        .getComisionPermanente(),
                                                                quien())))));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<UnidadOrganica> cambiarEstadoUnidad(
            String codigo, EstadoDeUnidad estadoDeUnidad) {
        boolean activa = estadoDeUnidad.getActiva();
        return ResponseEntity.ok(
                respuesta(
                        existente(
                                () ->
                                        valida(
                                                () ->
                                                        mantener.cambiarEstado(
                                                                codigo, activa, quien())))));
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Un dato que no cumple su regla del dominio responde 400 con su campo (FR-007).
    private static <T> T valida(Supplier<T> caso) {
        try {
            return caso.get();
        } catch (DatoInvalido error) {
            throw new SolicitudInvalida(
                    List.of(new DetalleRespuesta(error.campo(), error.motivo())));
        }
    }

    private static <T> T existente(Supplier<T> caso) {
        try {
            return caso.get();
        } catch (UnidadNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static UnidadOrganica respuesta(ec.mdd.academico.dominio.UnidadOrganica unidad) {
        return new UnidadOrganica(
                        unidad.codigo(),
                        TipoDeUnidad.fromValue(unidad.tipo().name()),
                        unidad.nombre(),
                        unidad.activa())
                .padre(unidad.padre())
                .gestionAutorizada(unidad.gestionAutorizada())
                .comisionPermanente(unidad.comisionPermanente());
    }
}
