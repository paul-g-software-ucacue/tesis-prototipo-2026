package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.adaptadores.entrada.api.CarreraDeReferencia;
import ec.mdd.docentes.adaptadores.entrada.api.DatosDeProyecto;
import ec.mdd.docentes.adaptadores.entrada.api.EstadoDeProyecto;
import ec.mdd.docentes.adaptadores.entrada.api.MotivoNoAsignable;
import ec.mdd.docentes.adaptadores.entrada.api.Participante;
import ec.mdd.docentes.adaptadores.entrada.api.ParticipanteDeProyecto;
import ec.mdd.docentes.adaptadores.entrada.api.Proyecto;
import ec.mdd.docentes.adaptadores.entrada.api.ProyectoNuevo;
import ec.mdd.docentes.adaptadores.entrada.api.ProyectosApi;
import ec.mdd.docentes.adaptadores.entrada.api.RolDeParticipante;
import ec.mdd.docentes.adaptadores.entrada.api.TipoDeProyecto;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectoNoEncontrado;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Los proyectos aprobados ({@code contratos/docentes/openapi.yaml}, tag {@code Proyectos}, RF-18,
 * F03-H05). Los leen y los mantienen los mismos que los docentes (D-70); «Proyectos y horas de
 * participantes: lectura» no basta, porque su «solo lo propio» todavía no se puede aplicar (D-64).
 * Con un periodo, cada proyecto dice si es asignable en él y por qué no.
 *
 * <p>Un dato inválido, también un periodo que no existe, responde 400 con su campo; un proyecto que
 * no existe, 404; uno repetido, {@code DOCENTES_PROYECTO_REPETIDO}. Los modelos del contrato se
 * llaman como los del dominio: los del dominio van con su nombre completo.
 */
@RestController
class ProyectosControlador implements ProyectosApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DOCENTES:LECTURA', 'PERMISO_CATALOGOS:LECTURA',"
                    + " 'PERMISO_DISTRIBUTIVO:LECTURA')";
    private static final String ESCRITURA =
            "hasAnyAuthority('PERMISO_DOCENTES:ESCRITURA', 'PERMISO_CATALOGOS:ESCRITURA',"
                    + " 'PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final ConsultarProyectos consultar;
    private final MantenerProyectos mantener;
    private final ConsultarDocentes docentes;

    ProyectosControlador(
            ConsultarProyectos consultar, MantenerProyectos mantener, ConsultarDocentes docentes) {
        this.consultar = consultar;
        this.mantener = mantener;
        this.docentes = docentes;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Proyecto>> listarProyectos(
            String periodo, TipoDeProyecto tipo, EstadoDeProyecto estado) {
        List<ProyectoConsultado> proyectos =
                valida(
                        () ->
                                consultar.listar(
                                        periodo,
                                        tipo == null
                                                ? null
                                                : ec.mdd.docentes.dominio.TipoDeProyecto.valueOf(
                                                        tipo.getValue()),
                                        estado == null
                                                ? null
                                                : ec.mdd.docentes.dominio.EstadoDeProyecto.valueOf(
                                                        estado.getValue())));
        Referencias referencias = docentes.referencias();
        return ResponseEntity.ok(
                proyectos.stream().map(uno -> respuesta(uno, referencias)).toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Proyecto> consultarProyecto(String codigo, String periodo) {
        ProyectoConsultado proyecto =
                existente(() -> valida(() -> consultar.proyecto(codigo, periodo)));
        return ResponseEntity.ok(respuesta(proyecto, docentes.referencias()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Proyecto> registrarProyecto(ProyectoNuevo nuevo) {
        ProyectoConsultado registrado =
                valida(
                        () ->
                                mantener.registrar(
                                        proyecto(
                                                nuevo.getCodigo(),
                                                new DatosDeProyecto()
                                                        .nombre(nuevo.getNombre())
                                                        .tipo(nuevo.getTipo())
                                                        .carreras(nuevo.getCarreras())
                                                        .resolucionOcs(nuevo.getResolucionOcs())
                                                        .fechaResolucion(nuevo.getFechaResolucion())
                                                        .estado(nuevo.getEstado())
                                                        .vigenciaInicio(nuevo.getVigenciaInicio())
                                                        .vigenciaFin(nuevo.getVigenciaFin())
                                                        .director(nuevo.getDirector())
                                                        .participantes(nuevo.getParticipantes())
                                                        .horasAprobadas(nuevo.getHorasAprobadas())),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta(registrado, docentes.referencias()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Proyecto> corregirProyecto(String codigo, DatosDeProyecto datos) {
        ProyectoConsultado corregido =
                existente(() -> valida(() -> mantener.corregir(proyecto(codigo, datos), quien())));
        return ResponseEntity.ok(respuesta(corregido, docentes.referencias()));
    }

    private static ec.mdd.docentes.dominio.Proyecto proyecto(String codigo, DatosDeProyecto datos) {
        return new ec.mdd.docentes.dominio.Proyecto(
                codigo,
                datos.getNombre(),
                ec.mdd.docentes.dominio.TipoDeProyecto.valueOf(datos.getTipo().getValue()),
                datos.getCarreras(),
                datos.getResolucionOcs(),
                datos.getFechaResolucion(),
                ec.mdd.docentes.dominio.EstadoDeProyecto.valueOf(datos.getEstado().getValue()),
                datos.getVigenciaInicio(),
                datos.getVigenciaFin(),
                datos.getDirector(),
                datos.getParticipantes().stream().map(ProyectosControlador::participante).toList(),
                datos.getHorasAprobadas());
    }

    private static ec.mdd.docentes.dominio.Participante participante(Participante uno) {
        return new ec.mdd.docentes.dominio.Participante(
                uno.getDocente(),
                uno.getRol() == null
                        ? null
                        : ec.mdd.docentes.dominio.RolDeParticipante.valueOf(
                                uno.getRol().getValue()),
                uno.getHoras());
    }

    private static Proyecto respuesta(ProyectoConsultado consultado, Referencias referencias) {
        ec.mdd.docentes.dominio.Proyecto proyecto = consultado.proyecto();
        Proyecto respuesta =
                new Proyecto(
                                proyecto.codigo(),
                                proyecto.nombre(),
                                TipoDeProyecto.fromValue(proyecto.tipo().name()),
                                proyecto.carreras(),
                                proyecto.carreras().stream()
                                        .filter(carrera -> referencias.carrera(carrera).isPresent())
                                        .map(
                                                carrera ->
                                                        new CarreraDeReferencia(
                                                                carrera,
                                                                referencias
                                                                        .carreras()
                                                                        .get(carrera)
                                                                        .nombre(),
                                                                referencias
                                                                        .carreras()
                                                                        .get(carrera)
                                                                        .siglas()))
                                        .toList(),
                                proyecto.resolucionOcs(),
                                proyecto.fechaResolucion(),
                                EstadoDeProyecto.fromValue(proyecto.estado().name()),
                                proyecto.vigenciaInicio(),
                                proyecto.vigenciaFin(),
                                proyecto.director(),
                                proyecto.participantes().stream()
                                        .map(
                                                uno ->
                                                        new ParticipanteDeProyecto(
                                                                        uno.docente(),
                                                                        RolDeParticipante.fromValue(
                                                                                uno.rol().name()),
                                                                        uno.horas())
                                                                .nombre(
                                                                        consultado
                                                                                .nombresDeDocentes()
                                                                                .get(
                                                                                        uno
                                                                                                .docente())))
                                        .toList(),
                                proyecto.horasAprobadas())
                        .nombreDelDirector(consultado.nombresDeDocentes().get(proyecto.director()));
        consultado
                .asignabilidad()
                .ifPresent(
                        asignabilidad ->
                                respuesta
                                        .asignable(asignabilidad.asignable())
                                        .motivos(
                                                asignabilidad.motivos().stream()
                                                        .sorted(Comparator.naturalOrder())
                                                        .map(
                                                                motivo ->
                                                                        MotivoNoAsignable.fromValue(
                                                                                motivo.name()))
                                                        .toList()));
        return respuesta;
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
        } catch (ProyectoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
