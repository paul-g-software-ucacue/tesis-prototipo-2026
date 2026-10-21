package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.DatosDelPeriodo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeDestino;
import ec.mdd.academico.adaptadores.entrada.api.EstadoPeriodo;
import ec.mdd.academico.adaptadores.entrada.api.PeriodoAcademico;
import ec.mdd.academico.adaptadores.entrada.api.PeriodoNuevo;
import ec.mdd.academico.adaptadores.entrada.api.PeriodosApi;
import ec.mdd.academico.adaptadores.entrada.api.VersionDelPeriodo;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.CambiarEstadoPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
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
    private final RegistrarPeriodo registrar;
    private final CorregirPeriodo corregir;
    private final AbrirPeriodo abrir;
    private final CambiarEstadoPeriodo cambiarEstado;

    AcademicoControlador(
            ConsultarPeriodos consultar,
            RegistrarPeriodo registrar,
            CorregirPeriodo corregir,
            AbrirPeriodo abrir,
            CambiarEstadoPeriodo cambiarEstado) {
        this.consultar = consultar;
        this.registrar = registrar;
        this.corregir = corregir;
        this.abrir = abrir;
        this.cambiarEstado = cambiarEstado;
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
        PeriodoConsultado registrado =
                valido(
                        () ->
                                registrar.registrar(
                                        periodoNuevo.getCodigo(),
                                        periodoNuevo.getNombre(),
                                        periodoNuevo.getInicio(),
                                        periodoNuevo.getFin(),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(registrado));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> corregirPeriodo(
            String codigo, DatosDelPeriodo datosDelPeriodo) {
        PeriodoConsultado corregido =
                existente(
                        () ->
                                valido(
                                        () ->
                                                corregir.corregir(
                                                        codigo,
                                                        datosDelPeriodo.getNombre(),
                                                        datosDelPeriodo.getInicio(),
                                                        datosDelPeriodo.getFin(),
                                                        quien())));
        return ResponseEntity.ok(respuesta(corregido));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> abrirPeriodo(
            String codigo, VersionDelPeriodo versionDelPeriodo) {
        int version = versionDelPeriodo.getVersion();
        try {
            return ResponseEntity.ok(
                    respuesta(existente(() -> abrir.abrir(codigo, version, quien()))));
        } catch (VersionInexistente error) {
            throw invalida("version", "La versión de normativa " + version + " no existe");
        }
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<PeriodoAcademico> cambiarEstadoPeriodo(
            String codigo, EstadoDeDestino estadoDeDestino) {
        ec.mdd.academico.dominio.EstadoPeriodo destino =
                ec.mdd.academico.dominio.EstadoPeriodo.valueOf(
                        estadoDeDestino.getEstado().getValue());
        return ResponseEntity.ok(
                respuesta(existente(() -> cambiarEstado.cambiar(codigo, destino, quien()))));
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Un dato que no cumple su regla del dominio responde 400 con su campo (FR-006).
    private static PeriodoConsultado valido(Supplier<PeriodoConsultado> caso) {
        try {
            return caso.get();
        } catch (DatoInvalido error) {
            throw invalida(error.campo(), error.motivo());
        }
    }

    private static SolicitudInvalida invalida(String campo, String descripcion) {
        return new SolicitudInvalida(List.of(new DetalleRespuesta(campo, descripcion)));
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
