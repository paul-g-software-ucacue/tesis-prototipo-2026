package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.adaptadores.entrada.api.AdvertenciaDeCargo;
import ec.mdd.docentes.adaptadores.entrada.api.Cargo;
import ec.mdd.docentes.adaptadores.entrada.api.CargoNuevo;
import ec.mdd.docentes.adaptadores.entrada.api.CargosApi;
import ec.mdd.docentes.adaptadores.entrada.api.CodigoDeAdvertencia;
import ec.mdd.docentes.adaptadores.entrada.api.DatosDeCargo;
import ec.mdd.docentes.adaptadores.entrada.api.EstadoDeCargo;
import ec.mdd.docentes.adaptadores.entrada.api.TipoDeCargo;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.puertos.entrada.CargoConsultado;
import ec.mdd.docentes.puertos.entrada.CargoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Los cargos de los docentes ({@code contratos/docentes/openapi.yaml}, tag {@code Cargos},
 * F03-H05). Los leen y los mantienen los mismos que los docentes (D-70); «Proyectos y horas de
 * participantes: lectura» no basta, porque su «solo lo propio» todavía no se puede aplicar (D-64).
 * {@code PermisosDocentesTest} lo comprueba.
 *
 * <p>Un dato inválido responde 400 con su campo; un cargo que no existe, 404; uno repetido, un
 * segundo coordinador vigente o un docente no habilitado, su 409 de {@code ErroresDocentes}. Los
 * modelos del contrato se llaman como los del dominio: los del dominio van con su nombre completo.
 */
@RestController
class CargosControlador implements CargosApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DOCENTES:LECTURA', 'PERMISO_CATALOGOS:LECTURA',"
                    + " 'PERMISO_DISTRIBUTIVO:LECTURA')";
    private static final String ESCRITURA =
            "hasAnyAuthority('PERMISO_DOCENTES:ESCRITURA', 'PERMISO_CATALOGOS:ESCRITURA',"
                    + " 'PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final ConsultarCargos consultar;
    private final MantenerCargos mantener;
    private final ConsultarDocentes docentes;

    CargosControlador(
            ConsultarCargos consultar, MantenerCargos mantener, ConsultarDocentes docentes) {
        this.consultar = consultar;
        this.mantener = mantener;
        this.docentes = docentes;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Cargo>> listarCargos(EstadoDeCargo estado, String docente) {
        Referencias referencias = docentes.referencias();
        return ResponseEntity.ok(
                consultar
                        .listar(
                                estado == null
                                        ? null
                                        : ec.mdd.docentes.dominio.EstadoDeCargo.valueOf(
                                                estado.getValue()),
                                docente)
                        .stream()
                        .map(uno -> respuesta(uno, referencias))
                        .toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Cargo> registrarCargo(CargoNuevo nuevo) {
        CargoConsultado registrado =
                valida(
                        () ->
                                mantener.registrar(
                                        cargo(
                                                nuevo.getDocente(),
                                                nuevo.getTipo(),
                                                nuevo.getFechaDesignacion(),
                                                nuevo.getUnidad(),
                                                nuevo.getCarrera(),
                                                nuevo.getFechaFin(),
                                                nuevo.getDocumento(),
                                                nuevo.getEstado()),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta(registrado, docentes.referencias()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Cargo> corregirCargo(
            String docente, TipoDeCargo tipo, LocalDate fechaDesignacion, DatosDeCargo datos) {
        CargoConsultado corregido =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.corregir(
                                                        cargo(
                                                                docente,
                                                                tipo,
                                                                fechaDesignacion,
                                                                datos.getUnidad(),
                                                                datos.getCarrera(),
                                                                datos.getFechaFin(),
                                                                datos.getDocumento(),
                                                                datos.getEstado()),
                                                        quien())));
        return ResponseEntity.ok(respuesta(corregido, docentes.referencias()));
    }

    private static ec.mdd.docentes.dominio.Cargo cargo(
            String docente,
            TipoDeCargo tipo,
            LocalDate fechaDesignacion,
            String unidad,
            String carrera,
            LocalDate fechaFin,
            String documento,
            EstadoDeCargo estado) {
        return new ec.mdd.docentes.dominio.Cargo(
                docente,
                ec.mdd.docentes.dominio.TipoDeCargo.valueOf(tipo.getValue()),
                fechaDesignacion,
                unidad,
                carrera,
                fechaFin,
                documento,
                estado == null
                        ? null
                        : ec.mdd.docentes.dominio.EstadoDeCargo.valueOf(estado.getValue()));
    }

    private static Cargo respuesta(CargoConsultado consultado, Referencias referencias) {
        ec.mdd.docentes.dominio.Cargo cargo = consultado.cargo();
        return new Cargo(
                        cargo.docente(),
                        TipoDeCargo.fromValue(cargo.tipo().name()),
                        cargo.fechaDesignacion(),
                        cargo.documento(),
                        EstadoDeCargo.fromValue(cargo.estado().name()),
                        consultado.advertencias().stream()
                                .map(
                                        una ->
                                                new AdvertenciaDeCargo(
                                                        CodigoDeAdvertencia.fromValue(una.name()),
                                                        una.descripcion()))
                                .toList())
                .nombreDelDocente(consultado.nombreDelDocente())
                .unidad(cargo.unidad())
                .nombreDeUnidad(referencias.unidad(cargo.unidad()).orElse(null))
                .carrera(cargo.carrera())
                .nombreDeCarrera(
                        cargo.carrera() == null
                                ? null
                                : referencias.carrera(cargo.carrera()).orElse(null))
                .fechaFin(cargo.fechaFin());
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Un dato que no cumple su regla del dominio responde 400 con su campo (FR-010).
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
        } catch (CargoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
