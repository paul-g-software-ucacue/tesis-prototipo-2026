package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.Asignatura;
import ec.mdd.academico.adaptadores.entrada.api.AsignaturaDeReferencia;
import ec.mdd.academico.adaptadores.entrada.api.AsignaturaNueva;
import ec.mdd.academico.adaptadores.entrada.api.CampoAmplio;
import ec.mdd.academico.adaptadores.entrada.api.Carrera;
import ec.mdd.academico.adaptadores.entrada.api.CarreraDeReferencia;
import ec.mdd.academico.adaptadores.entrada.api.CarreraNueva;
import ec.mdd.academico.adaptadores.entrada.api.CarrerasApi;
import ec.mdd.academico.adaptadores.entrada.api.DatosDeAsignatura;
import ec.mdd.academico.adaptadores.entrada.api.DatosDeCarrera;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeAsignatura;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeCarrera;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeCarreraNuevo;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeMalla;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeMallaNuevo;
import ec.mdd.academico.adaptadores.entrada.api.Jornada;
import ec.mdd.academico.adaptadores.entrada.api.MallaNueva;
import ec.mdd.academico.adaptadores.entrada.api.Modalidad;
import ec.mdd.academico.adaptadores.entrada.api.NivelDeFormacion;
import ec.mdd.academico.adaptadores.entrada.api.PeriodoDeReferencia;
import ec.mdd.academico.adaptadores.entrada.api.ReferenciasDeAcademico;
import ec.mdd.academico.adaptadores.entrada.api.ResumenDeMalla;
import ec.mdd.academico.adaptadores.entrada.api.TipoDeUnidad;
import ec.mdd.academico.adaptadores.entrada.api.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.adaptadores.entrada.api.UnidadDeReferencia;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.puertos.entrada.AsignaturaNoEncontrada;
import ec.mdd.academico.puertos.entrada.CarreraConMallas;
import ec.mdd.academico.puertos.entrada.CarreraNoEncontrada;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.MallaNoEncontrada;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las carreras, sus mallas y sus asignaturas ({@code contratos/academico/openapi.yaml}, RF-12,
 * RF-14, F03-H02). Las leen quienes tienen «Distributivo del periodo: lectura» o «Catálogos y datos
 * maestros: lectura»; las mantienen quienes tienen «Catálogos y datos maestros: escritura» o
 * «Distributivo del periodo: escritura», como dice su {@code x-mdd-permiso} (ADR-021, D-70); {@code
 * PermisosAcademicoTest} lo comprueba.
 *
 * <p>Las referencias para Docentes (F03-H04) las leen, además, quienes tienen «Docentes: lectura».
 *
 * <p>Lo que no existe responde 404 con el código común; un dato inválido, 400 con su campo; un
 * código o unas siglas repetidos, su 409 (research §4). Los modelos del contrato se llaman como los
 * del dominio: los del dominio van con su nombre completo.
 */
@RestController
class CarrerasControlador implements CarrerasApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA', 'PERMISO_CATALOGOS:LECTURA')";
    private static final String ESCRITURA =
            "hasAnyAuthority('PERMISO_CATALOGOS:ESCRITURA', 'PERMISO_DISTRIBUTIVO:ESCRITURA')";
    private static final String LECTURA_DE_REFERENCIAS =
            "hasAnyAuthority('PERMISO_CATALOGOS:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_DOCENTES:LECTURA')";

    private final ConsultarCarreras consultar;
    private final MantenerCarreras mantener;
    private final ConsultarReferencias referencias;

    CarrerasControlador(
            ConsultarCarreras consultar,
            MantenerCarreras mantener,
            ConsultarReferencias referencias) {
        this.consultar = consultar;
        this.mantener = mantener;
        this.referencias = referencias;
    }

    @Override
    @PreAuthorize(LECTURA_DE_REFERENCIAS)
    public ResponseEntity<ReferenciasDeAcademico> consultarReferencias() {
        ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico todas = referencias.referencias();
        return ResponseEntity.ok(
                new ReferenciasDeAcademico(
                        todas.carreras().stream()
                                .map(
                                        c ->
                                                new CarreraDeReferencia(
                                                        c.codigo(), c.nombre(), c.siglas()))
                                .toList(),
                        todas.asignaturas().stream()
                                .map(
                                        a ->
                                                new AsignaturaDeReferencia(
                                                        a.codigo(), a.nombre(), a.carrera()))
                                .toList(),
                        todas.camposAmplios().stream()
                                .map(c -> new CampoAmplio(c.codigo(), c.nombre()))
                                .toList(),
                        todas.unidades().stream().map(CarrerasControlador::unidad).toList(),
                        todas.periodos().stream()
                                .map(
                                        p ->
                                                new PeriodoDeReferencia(
                                                        p.codigo(),
                                                        p.nombre(),
                                                        p.inicio(),
                                                        p.fin()))
                                .toList()));
    }

    private static UnidadDeReferencia unidad(
            ec.mdd.academico.puertos.entrada.UnidadDeReferencia unidad) {
        return new UnidadDeReferencia(
                        unidad.codigo(),
                        unidad.nombre(),
                        TipoDeUnidad.fromValue(unidad.tipo()),
                        unidad.activa())
                .gestionAutorizada(unidad.gestionAutorizada());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<CampoAmplio>> listarCamposAmplios() {
        return ResponseEntity.ok(
                consultar.camposAmplios().stream()
                        .map(campo -> new CampoAmplio(campo.codigo(), campo.nombre()))
                        .toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Carrera>> listarCarreras() {
        return ResponseEntity.ok(
                consultar.carreras().stream().map(CarrerasControlador::respuesta).toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Carrera> registrarCarrera(CarreraNueva nueva) {
        ec.mdd.academico.dominio.Carrera registrada =
                valida(
                        () ->
                                mantener.registrarCarrera(
                                        ec.mdd.academico.dominio.Carrera.nueva(
                                                nueva.getCodigo(),
                                                nueva.getNombre(),
                                                nueva.getSiglas(),
                                                nueva.getSede(),
                                                nivel(nueva.getNivelFormacion()),
                                                modalidad(nueva.getModalidad()),
                                                jornadas(nueva.getJornadas()),
                                                campo(
                                                        nueva.getCampoAmplio(),
                                                        nueva.getCampoEspecifico()),
                                                nueva.getCampoDetallado(),
                                                nueva.getNumeroCiclos()),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta(new CarreraConMallas(registrada, List.of())));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Carrera> consultarCarrera(String codigo) {
        return ResponseEntity.ok(respuesta(existente(() -> consultar.carrera(codigo))));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Carrera> corregirCarrera(String codigo, DatosDeCarrera datos) {
        existente(
                () ->
                        valida(
                                () ->
                                        mantener.corregirCarrera(
                                                codigo,
                                                new ec.mdd.academico.puertos.entrada.DatosDeCarrera(
                                                        datos.getNombre(),
                                                        datos.getSiglas(),
                                                        datos.getSede(),
                                                        nivel(datos.getNivelFormacion()),
                                                        modalidad(datos.getModalidad()),
                                                        jornadas(datos.getJornadas()),
                                                        campo(
                                                                datos.getCampoAmplio(),
                                                                datos.getCampoEspecifico()),
                                                        datos.getCampoDetallado(),
                                                        datos.getNumeroCiclos()),
                                                quien())));
        return ResponseEntity.ok(respuesta(consultar.carrera(codigo)));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Carrera> cambiarEstadoCarrera(
            String codigo, EstadoDeCarreraNuevo estado) {
        existente(
                () ->
                        mantener.cambiarEstadoCarrera(
                                codigo,
                                ec.mdd.academico.dominio.EstadoDeCarrera.valueOf(
                                        estado.getEstado().getValue()),
                                quien()));
        return ResponseEntity.ok(respuesta(consultar.carrera(codigo)));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<ResumenDeMalla> registrarMalla(String codigo, MallaNueva nueva) {
        ec.mdd.academico.dominio.Malla malla =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.registrarMalla(
                                                        codigo, nueva.getCodigo(), quien())));
        return ResponseEntity.status(HttpStatus.CREATED).body(resumen(malla));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<ResumenDeMalla> cambiarEstadoMalla(
            String codigo, EstadoDeMallaNuevo estado) {
        ec.mdd.academico.dominio.Malla malla =
                existente(
                        () ->
                                mantener.cambiarEstadoMalla(
                                        codigo,
                                        ec.mdd.academico.dominio.EstadoDeMalla.valueOf(
                                                estado.getEstado().getValue()),
                                        quien()));
        return ResponseEntity.ok(resumen(malla));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<Asignatura>> listarAsignaturasDeCarrera(String codigo) {
        List<ec.mdd.academico.dominio.Asignatura> asignaturas =
                existente(() -> consultar.asignaturasDe(codigo));
        Map<String, String> nombres = nombresDeLosCampos();
        return ResponseEntity.ok(
                asignaturas.stream().map(asignatura -> respuesta(asignatura, nombres)).toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Asignatura> registrarAsignatura(AsignaturaNueva nueva) {
        ec.mdd.academico.dominio.Asignatura registrada =
                valida(
                        () ->
                                mantener.registrarAsignatura(
                                        ec.mdd.academico.dominio.Asignatura.nueva(
                                                nueva.getCodigo(),
                                                nueva.getCarrera(),
                                                nueva.getMalla(),
                                                nueva.getNombre(),
                                                nueva.getCiclo(),
                                                unidad(nueva.getUnidadOrganizacionCurricular()),
                                                nueva.getHorasSemanalesClase(),
                                                nueva.getHorasPracticasSemanales(),
                                                nueva.getCreditos(),
                                                nueva.getTroncoComun(),
                                                campo(
                                                        nueva.getCampoAmplio(),
                                                        nueva.getCampoEspecifico()),
                                                nueva.getPalabrasClave()),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta(registrada, nombresDeLosCampos()));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Asignatura> consultarAsignatura(String codigo) {
        return ResponseEntity.ok(
                respuesta(existente(() -> consultar.asignatura(codigo)), nombresDeLosCampos()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Asignatura> corregirAsignatura(String codigo, DatosDeAsignatura datos) {
        ec.mdd.academico.dominio.Asignatura corregida =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.corregirAsignatura(
                                                        codigo,
                                                        new ec.mdd.academico.puertos.entrada
                                                                .DatosDeAsignatura(
                                                                datos.getNombre(),
                                                                datos.getCiclo(),
                                                                unidad(
                                                                        datos
                                                                                .getUnidadOrganizacionCurricular()),
                                                                datos.getHorasSemanalesClase(),
                                                                datos.getHorasPracticasSemanales(),
                                                                datos.getCreditos(),
                                                                datos.getTroncoComun(),
                                                                campo(
                                                                        datos.getCampoAmplio(),
                                                                        datos.getCampoEspecifico()),
                                                                datos.getPalabrasClave()),
                                                        quien())));
        return ResponseEntity.ok(respuesta(corregida, nombresDeLosCampos()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Asignatura> cambiarEstadoAsignatura(
            String codigo, EstadoDeAsignatura estado) {
        ec.mdd.academico.dominio.Asignatura nueva =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.cambiarEstadoAsignatura(
                                                        codigo, estado.getActiva(), quien())));
        return ResponseEntity.ok(respuesta(nueva, nombresDeLosCampos()));
    }

    private Map<String, String> nombresDeLosCampos() {
        return consultar.camposAmplios().stream()
                .collect(
                        Collectors.toMap(
                                ec.mdd.academico.dominio.CampoAmplio::codigo,
                                ec.mdd.academico.dominio.CampoAmplio::nombre));
    }

    private static Carrera respuesta(CarreraConMallas conMallas) {
        ec.mdd.academico.dominio.Carrera carrera = conMallas.carrera();
        return new Carrera(
                        carrera.codigo(),
                        carrera.nombre(),
                        carrera.siglas(),
                        carrera.sede(),
                        NivelDeFormacion.fromValue(carrera.nivelFormacion().name()),
                        Modalidad.fromValue(carrera.modalidad().name()),
                        carrera.jornadas().stream()
                                .map(jornada -> Jornada.fromValue(jornada.name()))
                                .collect(Collectors.toCollection(LinkedHashSet::new)),
                        carrera.campo().amplio(),
                        carrera.campo().especifico(),
                        carrera.numeroCiclos(),
                        EstadoDeCarrera.fromValue(carrera.estado().name()),
                        conMallas.mallas().stream()
                                .map(
                                        malla ->
                                                new ResumenDeMalla(
                                                        malla.malla().codigo(),
                                                        EstadoDeMalla.fromValue(
                                                                malla.malla().estado().name()),
                                                        malla.asignaturas(),
                                                        malla.horas()))
                                .toList())
                .campoDetallado(carrera.campoDetallado());
    }

    private ResumenDeMalla resumen(ec.mdd.academico.dominio.Malla malla) {
        CarreraConMallas.Resumen resumen =
                consultar.carrera(malla.carrera()).mallas().stream()
                        .filter(una -> una.malla().codigo().equals(malla.codigo()))
                        .findFirst()
                        .orElseThrow();
        return new ResumenDeMalla(
                malla.codigo(),
                EstadoDeMalla.fromValue(malla.estado().name()),
                resumen.asignaturas(),
                resumen.horas());
    }

    private static Asignatura respuesta(
            ec.mdd.academico.dominio.Asignatura asignatura, Map<String, String> nombres) {
        return new Asignatura(
                        asignatura.codigo(),
                        asignatura.carrera(),
                        asignatura.malla(),
                        asignatura.nombre(),
                        asignatura.ciclo(),
                        UnidadDeOrganizacionCurricular.fromValue(
                                asignatura.unidadOrganizacionCurricular().name()),
                        asignatura.horasSemanalesClase(),
                        asignatura.troncoComun(),
                        asignatura.campo().especifico(),
                        asignatura.palabrasClave(),
                        new CampoAmplio(
                                asignatura.campo().amplio(),
                                nombres.getOrDefault(asignatura.campo().amplio(), "")),
                        asignatura.activa())
                .horasPracticasSemanales(asignatura.horasPracticasSemanales())
                .creditos(asignatura.creditos());
    }

    private static ec.mdd.academico.dominio.NivelDeFormacion nivel(NivelDeFormacion nivel) {
        return ec.mdd.academico.dominio.NivelDeFormacion.valueOf(nivel.getValue());
    }

    private static ec.mdd.academico.dominio.Modalidad modalidad(Modalidad modalidad) {
        return ec.mdd.academico.dominio.Modalidad.valueOf(modalidad.getValue());
    }

    private static ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular unidad(
            UnidadDeOrganizacionCurricular unidad) {
        return ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular.valueOf(unidad.getValue());
    }

    // El contrato las trae como conjunto; van en el orden de la enumeración.
    private static List<ec.mdd.academico.dominio.Jornada> jornadas(Collection<Jornada> jornadas) {
        return jornadas.stream()
                .map(jornada -> ec.mdd.academico.dominio.Jornada.valueOf(jornada.getValue()))
                .sorted()
                .toList();
    }

    private static CampoConocimiento campo(String amplio, String especifico) {
        return new CampoConocimiento(amplio, especifico);
    }

    private static String quien() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Un dato que no cumple su regla del dominio responde 400 con su campo (FR-005).
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
        } catch (CarreraNoEncontrada | MallaNoEncontrada | AsignaturaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
