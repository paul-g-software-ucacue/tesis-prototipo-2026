package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.DatosDelPeriodo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeDestino;
import ec.mdd.academico.adaptadores.entrada.api.EstadoPeriodo;
import ec.mdd.academico.adaptadores.entrada.api.PeriodoAcademico;
import ec.mdd.academico.adaptadores.entrada.api.PeriodoNuevo;
import ec.mdd.academico.adaptadores.entrada.api.PeriodosApi;
import ec.mdd.academico.adaptadores.entrada.api.VersionDelPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Los periodos académicos ({@code contratos/academico/openapi.yaml}, RF-13, F02-H01): consultarlos,
 * registrarlos, corregirlos, abrirlos con su versión de normativa y cambiar su estado. Leer exige
 * «Distributivo del periodo: lectura» y escribir, «Distributivo del periodo: escritura», como dice
 * su {@code x-mdd-permiso} (ADR-021, D-69); {@code PermisosAcademicoTest} lo comprueba.
 *
 * <p>Un periodo que no existe responde 404 con el código común. Un dato inválido o una versión que
 * no existe responde 400 con un detalle por campo, en español. Las reglas del catálogo responden su
 * 409 (research §5).
 */
@RestController
class AcademicoControlador implements PeriodosApi {

    private static final String LECTURA = "hasAuthority('PERMISO_DISTRIBUTIVO:LECTURA')";
    private static final String ESCRITURA = "hasAuthority('PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final ConsultarPeriodos consultar;

    AcademicoControlador(ConsultarPeriodos consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<PeriodoAcademico>> listarPeriodos() {
        return ResponseEntity.ok(
                consultar.todos().stream().map(AcademicoControlador::respuesta).toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<PeriodoAcademico> consultarPeriodo(String codigo) {
        return ResponseEntity.ok(respuesta(existente(() -> consultar.porCodigo(codigo))));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> registrarPeriodo(PeriodoNuevo periodoNuevo) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> corregirPeriodo(
            String codigo, DatosDelPeriodo datosDelPeriodo) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> abrirPeriodo(
            String codigo, VersionDelPeriodo versionDelPeriodo) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> cambiarEstadoPeriodo(
            String codigo, EstadoDeDestino estadoDeDestino) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    private static PeriodoConsultado existente(Supplier<PeriodoConsultado> caso) {
        try {
            return caso.get();
        } catch (PeriodoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static PeriodoAcademico respuesta(PeriodoConsultado consultado) {
        ec.mdd.academico.dominio.PeriodoAcademico periodo = consultado.periodo();
        PeriodoAcademico respuesta =
                new PeriodoAcademico(
                        periodo.codigo(),
                        periodo.nombre(),
                        periodo.inicio(),
                        periodo.fin(),
                        EstadoPeriodo.fromValue(periodo.estado().name()));
        // Sin versión, el campo se omite (contrato).
        consultado.version().ifPresent(respuesta::setVersion);
        return respuesta;
    }
}
