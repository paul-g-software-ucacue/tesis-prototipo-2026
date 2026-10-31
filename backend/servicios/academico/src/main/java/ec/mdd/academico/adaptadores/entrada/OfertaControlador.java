package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.AsignaturaParalelo;
import ec.mdd.academico.adaptadores.entrada.api.AsignaturaParaleloNueva;
import ec.mdd.academico.adaptadores.entrada.api.CampoAmplio;
import ec.mdd.academico.adaptadores.entrada.api.DatosDeAsignaturaParalelo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeAsignaturaParaleloNuevo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoPeriodo;
import ec.mdd.academico.adaptadores.entrada.api.HorasDeAsignatura;
import ec.mdd.academico.adaptadores.entrada.api.HorasDeCarrera;
import ec.mdd.academico.adaptadores.entrada.api.Jornada;
import ec.mdd.academico.adaptadores.entrada.api.Modalidad;
import ec.mdd.academico.adaptadores.entrada.api.OfertaApi;
import ec.mdd.academico.adaptadores.entrada.api.OfertaDelPeriodo;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.HorasACubrir;
import ec.mdd.academico.puertos.entrada.AsignaturaParaleloNoEncontrada;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.plataforma.errores.nucleo.AlcanceDeCarrera;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * La oferta del periodo ({@code contratos/academico/openapi.yaml}, RF-15, F03-H03). La leen quienes
 * tienen «Oferta y propuesta de docentes: lectura», «Distributivo del periodo: lectura» o
 * «Catálogos y datos maestros: lectura»; la mantienen quienes tienen «Catálogos y datos maestros:
 * escritura» o «Distributivo del periodo: escritura», como dice su {@code x-mdd-permiso} (ADR-021,
 * D-70); {@code PermisosAcademicoTest} lo comprueba.
 *
 * <p>Lo que no existe responde 404 con el código común; un dato inválido, 400 con su campo; una
 * repetida o un periodo fuera de Planificación, su 409. Cada respuesta trae las horas y la sede
 * efectivas y los datos de la asignatura para la afinidad. Los modelos del contrato se llaman como
 * los del dominio: los del dominio van con su nombre completo.
 */
@RestController
class OfertaControlador implements OfertaApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_OFERTA:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";
    private static final String ESCRITURA =
            "hasAnyAuthority('PERMISO_CATALOGOS:ESCRITURA', 'PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final ConsultarOferta consultar;
    private final MantenerOferta mantener;
    private final ConsultarCarreras carreras;

    OfertaControlador(
            ConsultarOferta consultar, MantenerOferta mantener, ConsultarCarreras carreras) {
        this.consultar = consultar;
        this.mantener = mantener;
        this.carreras = carreras;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<OfertaDelPeriodo> consultarOferta(String codigo) {
        Optional<String> propia = carreraPropia();
        ec.mdd.academico.dominio.OfertaDelPeriodo completa =
                existente(() -> consultar.oferta(codigo));
        ec.mdd.academico.dominio.OfertaDelPeriodo oferta =
                propia.map(completa::deCarrera).orElse(completa);
        Map<String, String> nombres = nombresDeLosCampos();
        HorasACubrir horas = oferta.horasACubrir();
        return ResponseEntity.ok(
                new OfertaDelPeriodo(
                        codigo,
                        EstadoPeriodo.fromValue(oferta.periodo().estado().name()),
                        oferta.modificable(),
                        horas.total(),
                        horas.carreras().stream()
                                .map(
                                        carrera ->
                                                new HorasDeCarrera(
                                                        carrera.carrera(),
                                                        oferta.datos()
                                                                .carrera(carrera.carrera())
                                                                .orElseThrow()
                                                                .nombre(),
                                                        carrera.total(),
                                                        carrera.asignaturas().stream()
                                                                .map(
                                                                        una ->
                                                                                new HorasDeAsignatura(
                                                                                        una
                                                                                                .asignatura(),
                                                                                        oferta.datos()
                                                                                                .asignatura(
                                                                                                        una
                                                                                                                .asignatura())
                                                                                                .orElseThrow()
                                                                                                .nombre(),
                                                                                        una.ciclo(),
                                                                                        una
                                                                                                .paralelos(),
                                                                                        una
                                                                                                .total()))
                                                                .toList()))
                                .toList(),
                        oferta.asignaturasParalelo().stream()
                                .map(una -> respuesta(una, oferta, nombres))
                                .toList()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<AsignaturaParalelo> ofertarAsignatura(
            String codigo, AsignaturaParaleloNueva nueva) {
        ec.mdd.academico.dominio.AsignaturaParalelo ofertada =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.ofertar(
                                                        ec.mdd.academico.dominio.AsignaturaParalelo
                                                                .nueva(
                                                                        codigo,
                                                                        nueva.getAsignatura(),
                                                                        nueva.getParalelo(),
                                                                        jornada(nueva.getJornada()),
                                                                        modalidad(
                                                                                nueva
                                                                                        .getModalidad()),
                                                                        nueva.getSede(),
                                                                        nueva.getEstudiantes(),
                                                                        nueva.getHorasSemanales()),
                                                        quien())));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(codigo, ofertada));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<AsignaturaParalelo> consultarAsignaturaParalelo(
            String codigo, String asignatura, String paralelo) {
        Optional<String> propia = carreraPropia();
        ec.mdd.academico.dominio.OfertaDelPeriodo oferta =
                existente(() -> consultar.oferta(codigo));
        ec.mdd.academico.dominio.AsignaturaParalelo una =
                oferta.asignaturaParalelo(asignatura, paralelo)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String suCarrera = oferta.datos().asignatura(asignatura).orElseThrow().carrera();
        if (propia.isPresent() && !propia.get().equals(suCarrera)) {
            throw new AccessDeniedException("La asignatura-paralelo es de otra carrera");
        }
        return ResponseEntity.ok(respuesta(una, oferta, nombresDeLosCampos()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<AsignaturaParalelo> corregirAsignaturaParalelo(
            String codigo, String asignatura, String paralelo, DatosDeAsignaturaParalelo datos) {
        ec.mdd.academico.dominio.AsignaturaParalelo corregida =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.corregir(
                                                        codigo,
                                                        asignatura,
                                                        paralelo,
                                                        new ec.mdd.academico.puertos.entrada
                                                                .DatosDeAsignaturaParalelo(
                                                                jornada(datos.getJornada()),
                                                                modalidad(datos.getModalidad()),
                                                                datos.getSede(),
                                                                datos.getEstudiantes(),
                                                                datos.getHorasSemanales()),
                                                        quien())));
        return ResponseEntity.ok(respuesta(codigo, corregida));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<AsignaturaParalelo> cambiarEstadoAsignaturaParalelo(
            String codigo,
            String asignatura,
            String paralelo,
            EstadoDeAsignaturaParaleloNuevo estado) {
        ec.mdd.academico.dominio.AsignaturaParalelo cambiada =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.cambiarEstado(
                                                        codigo,
                                                        asignatura,
                                                        paralelo,
                                                        ec.mdd.academico.dominio
                                                                .EstadoDeAsignaturaParalelo.valueOf(
                                                                estado.getEstado().getValue()),
                                                        quien())));
        return ResponseEntity.ok(respuesta(codigo, cambiada));
    }

    private AsignaturaParalelo respuesta(
            String periodo, ec.mdd.academico.dominio.AsignaturaParalelo asignaturaParalelo) {
        return respuesta(asignaturaParalelo, consultar.oferta(periodo), nombresDeLosCampos());
    }

    /** La carrera a la que se limita quien solo tiene «lo propio» (D-64). */
    private static Optional<String> carreraPropia() {
        return AlcanceDeCarrera.propia(
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
    }

    private static AsignaturaParalelo respuesta(
            ec.mdd.academico.dominio.AsignaturaParalelo una,
            ec.mdd.academico.dominio.OfertaDelPeriodo oferta,
            Map<String, String> nombres) {
        Asignatura asignatura = oferta.datos().asignatura(una.asignatura()).orElseThrow();
        return new AsignaturaParalelo(
                una.periodo(),
                una.asignatura(),
                asignatura.nombre(),
                asignatura.carrera(),
                asignatura.malla(),
                asignatura.ciclo(),
                una.paralelo(),
                Jornada.fromValue(una.jornada().name()),
                Modalidad.fromValue(una.modalidad().name()),
                oferta.sedeDe(una),
                una.sede() != null,
                una.estudiantes(),
                oferta.horasDe(una),
                una.horasIndicadas() != null,
                oferta.horasDeLaAsignatura(una),
                oferta.porEncimaDeLaAsignatura(una),
                asignatura.troncoComun(),
                new CampoAmplio(
                        asignatura.campo().amplio(),
                        nombres.getOrDefault(asignatura.campo().amplio(), "")),
                asignatura.campo().especifico(),
                asignatura.palabrasClave(),
                EstadoDeAsignaturaParalelo.fromValue(una.estado().name()));
    }

    private Map<String, String> nombresDeLosCampos() {
        return carreras.camposAmplios().stream()
                .collect(
                        Collectors.toMap(
                                ec.mdd.academico.dominio.CampoAmplio::codigo,
                                ec.mdd.academico.dominio.CampoAmplio::nombre));
    }

    private static ec.mdd.academico.dominio.Jornada jornada(Jornada jornada) {
        return ec.mdd.academico.dominio.Jornada.valueOf(jornada.getValue());
    }

    private static ec.mdd.academico.dominio.Modalidad modalidad(Modalidad modalidad) {
        return ec.mdd.academico.dominio.Modalidad.valueOf(modalidad.getValue());
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Un dato que no cumple su regla del dominio responde 400 con su campo (FR-006).
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
        } catch (PeriodoNoEncontrado | AsignaturaParaleloNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
