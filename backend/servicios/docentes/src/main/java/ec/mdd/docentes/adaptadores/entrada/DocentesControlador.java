package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.adaptadores.entrada.api.AsignaturaAfin;
import ec.mdd.docentes.adaptadores.entrada.api.AsignaturaDeReferencia;
import ec.mdd.docentes.adaptadores.entrada.api.CampoAmplio;
import ec.mdd.docentes.adaptadores.entrada.api.CarreraDeReferencia;
import ec.mdd.docentes.adaptadores.entrada.api.CategoriaDeEscalafon;
import ec.mdd.docentes.adaptadores.entrada.api.CondicionEspecial;
import ec.mdd.docentes.adaptadores.entrada.api.Curso;
import ec.mdd.docentes.adaptadores.entrada.api.DatosDeDocente;
import ec.mdd.docentes.adaptadores.entrada.api.Dedicacion;
import ec.mdd.docentes.adaptadores.entrada.api.Docente;
import ec.mdd.docentes.adaptadores.entrada.api.DocenteNuevo;
import ec.mdd.docentes.adaptadores.entrada.api.DocentesApi;
import ec.mdd.docentes.adaptadores.entrada.api.EstadoLaboral;
import ec.mdd.docentes.adaptadores.entrada.api.EstadoLaboralNuevo;
import ec.mdd.docentes.adaptadores.entrada.api.ExperienciaLaboral;
import ec.mdd.docentes.adaptadores.entrada.api.FichaAcademica;
import ec.mdd.docentes.adaptadores.entrada.api.PeriodoDeReferencia;
import ec.mdd.docentes.adaptadores.entrada.api.ReferenciasDeDocentes;
import ec.mdd.docentes.adaptadores.entrada.api.ResumenDeDocente;
import ec.mdd.docentes.adaptadores.entrada.api.TipoDeIdentificacion;
import ec.mdd.docentes.adaptadores.entrada.api.TipoDeTituloDeCuartoNivel;
import ec.mdd.docentes.adaptadores.entrada.api.TipoDeUnidad;
import ec.mdd.docentes.adaptadores.entrada.api.TipoPersonal;
import ec.mdd.docentes.adaptadores.entrada.api.TituloDeCuartoNivel;
import ec.mdd.docentes.adaptadores.entrada.api.TituloDeTercerNivel;
import ec.mdd.docentes.adaptadores.entrada.api.UnidadDeReferencia;
import ec.mdd.docentes.dominio.BusquedaDeDocentes;
import ec.mdd.docentes.dominio.CampoConocimiento;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.dominio.TituloTercerNivel;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Los docentes con su ficha académica ({@code contratos/docentes/openapi.yaml}, RF-16, RF-17,
 * F03-H04). Los leen quienes tienen «Docentes», «Distributivo del periodo» o «Catálogos y datos
 * maestros» de lectura; los mantienen quienes tienen esos mismos de escritura, como dice su {@code
 * x-mdd-permiso} (ADR-021, D-70); {@code PermisosDocentesTest} lo comprueba.
 *
 * <p>Un docente que no existe responde 404 con el código común; un dato inválido, 400 con su campo;
 * una identificación o un correo repetidos, su 409. Los modelos del contrato se llaman como los del
 * dominio: los del dominio van con su nombre completo.
 */
@RestController
class DocentesControlador implements DocentesApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DOCENTES:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";
    private static final String ESCRITURA =
            "hasAnyAuthority('PERMISO_DOCENTES:ESCRITURA', 'PERMISO_CATALOGOS:ESCRITURA',"
                    + " 'PERMISO_DISTRIBUTIVO:ESCRITURA')";

    private final ConsultarDocentes consultar;
    private final MantenerDocentes mantener;

    DocentesControlador(ConsultarDocentes consultar, MantenerDocentes mantener) {
        this.consultar = consultar;
        this.mantener = mantener;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<ResumenDeDocente>> buscarDocentes(
            String texto, String carrera, EstadoLaboral estado) {
        Referencias referencias = consultar.referencias();
        return ResponseEntity.ok(
                consultar
                        .buscar(
                                new BusquedaDeDocentes(
                                        texto,
                                        carrera,
                                        estado == null
                                                ? null
                                                : ec.mdd.docentes.dominio.EstadoLaboral.valueOf(
                                                        estado.getValue())))
                        .stream()
                        .map(docente -> resumen(docente, referencias))
                        .toList());
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Docente> registrarDocente(DocenteNuevo nuevo) {
        ec.mdd.docentes.dominio.Docente registrado =
                valida(
                        () ->
                                mantener.registrar(
                                        new Identificacion(
                                                ec.mdd.docentes.dominio.TipoDeIdentificacion
                                                        .valueOf(
                                                                nuevo.getTipoIdentificacion()
                                                                        .getValue()),
                                                nuevo.getIdentificacion()),
                                        datos(comoDatos(nuevo)),
                                        quien()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta(registrado, consultar.referencias()));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Docente> consultarDocente(String identificacion) {
        return ResponseEntity.ok(
                respuesta(
                        existente(() -> consultar.docente(identificacion)),
                        consultar.referencias()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Docente> corregirDocente(String identificacion, DatosDeDocente datos) {
        ec.mdd.docentes.dominio.Docente corregido =
                existente(
                        () ->
                                valida(
                                        () ->
                                                mantener.corregir(
                                                        identificacion, datos(datos), quien())));
        return ResponseEntity.ok(respuesta(corregido, consultar.referencias()));
    }

    @Override
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<Docente> cambiarEstadoDocente(
            String identificacion, EstadoLaboralNuevo estado) {
        ec.mdd.docentes.dominio.Docente cambiado =
                existente(
                        () ->
                                mantener.cambiarEstado(
                                        identificacion,
                                        ec.mdd.docentes.dominio.EstadoLaboral.valueOf(
                                                estado.getEstado().getValue()),
                                        quien()));
        return ResponseEntity.ok(respuesta(cambiado, consultar.referencias()));
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<ReferenciasDeDocentes> consultarReferenciasDeDocentes() {
        Referencias referencias = consultar.referencias();
        return ResponseEntity.ok(
                new ReferenciasDeDocentes(
                        referencias.carreras().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                        carrera ->
                                                new CarreraDeReferencia(
                                                        carrera.getKey(),
                                                        carrera.getValue().nombre(),
                                                        carrera.getValue().siglas()))
                                .toList(),
                        referencias.asignaturas().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                        asignatura ->
                                                new AsignaturaDeReferencia(
                                                        asignatura.getKey(),
                                                        asignatura.getValue().nombre(),
                                                        asignatura.getValue().carrera()))
                                .toList(),
                        camposAmplios(referencias, referencias.camposAmplios().keySet()),
                        referencias.unidades().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                        unidad ->
                                                new UnidadDeReferencia(
                                                                unidad.getKey(),
                                                                unidad.getValue().nombre(),
                                                                TipoDeUnidad.fromValue(
                                                                        unidad.getValue().tipo()),
                                                                unidad.getValue().activa())
                                                        .gestionAutorizada(
                                                                unidad.getValue().gestion()))
                                .toList(),
                        referencias.periodos().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                        periodo ->
                                                new PeriodoDeReferencia(
                                                        periodo.getKey(),
                                                        periodo.getValue().nombre(),
                                                        periodo.getValue().inicio(),
                                                        periodo.getValue().fin()))
                                .toList()));
    }

    // DocenteNuevo trae los mismos campos que DatosDeDocente, al mismo nivel.
    private static DatosDeDocente comoDatos(DocenteNuevo nuevo) {
        return new DatosDeDocente()
                .primerApellido(nuevo.getPrimerApellido())
                .segundoApellido(nuevo.getSegundoApellido())
                .primerNombre(nuevo.getPrimerNombre())
                .segundoNombre(nuevo.getSegundoNombre())
                .correoInstitucional(nuevo.getCorreoInstitucional())
                .telefono(nuevo.getTelefono())
                .tipoPersonal(nuevo.getTipoPersonal())
                .categoriaEscalafon(nuevo.getCategoriaEscalafon())
                .dedicacion(nuevo.getDedicacion())
                .horasContratadas(nuevo.getHorasContratadas())
                .fechaIngreso(nuevo.getFechaIngreso())
                .inicioContrato(nuevo.getInicioContrato())
                .finContrato(nuevo.getFinContrato())
                .condicionEspecial(nuevo.getCondicionEspecial())
                .carreraBase(nuevo.getCarreraBase())
                .ficha(nuevo.getFicha());
    }

    private static DatosDelDocente datos(DatosDeDocente datos) {
        FichaAcademica ficha = datos.getFicha();
        TituloDeTercerNivel tercero = ficha.getTercerNivel();
        return new DatosDelDocente(
                datos.getPrimerApellido(),
                datos.getSegundoApellido(),
                datos.getPrimerNombre(),
                datos.getSegundoNombre(),
                datos.getCorreoInstitucional(),
                datos.getTelefono(),
                ec.mdd.docentes.dominio.TipoPersonal.valueOf(datos.getTipoPersonal().getValue()),
                ec.mdd.docentes.dominio.CategoriaDeEscalafon.valueOf(
                        datos.getCategoriaEscalafon().getValue()),
                ec.mdd.docentes.dominio.Dedicacion.valueOf(datos.getDedicacion().getValue()),
                datos.getHorasContratadas(),
                datos.getFechaIngreso(),
                datos.getInicioContrato(),
                datos.getFinContrato(),
                datos.getCondicionEspecial() == null
                        ? null
                        : ec.mdd.docentes.dominio.CondicionEspecial.valueOf(
                                datos.getCondicionEspecial().getValue()),
                datos.getCarreraBase(),
                new ec.mdd.docentes.dominio.FichaAcademica(
                        new TituloTercerNivel(
                                tercero.getTitulo(),
                                tercero.getRegistroSenescyt(),
                                new CampoConocimiento(
                                        tercero.getCampoAmplio(), tercero.getCampoEspecifico())),
                        lista(ficha.getCuartoNivel()).stream()
                                .map(
                                        titulo ->
                                                new ec.mdd.docentes.dominio.TituloCuartoNivel(
                                                        titulo.getTitulo(),
                                                        ec.mdd.docentes.dominio
                                                                .TipoDeTituloDeCuartoNivel.valueOf(
                                                                titulo.getTipo().getValue()),
                                                        titulo.getCampoEspecifico(),
                                                        titulo.getAnio()))
                                .toList(),
                        lista(ficha.getCursos()).stream()
                                .map(
                                        curso ->
                                                new ec.mdd.docentes.dominio.Curso(
                                                        curso.getNombre(),
                                                        curso.getCampoEspecifico(),
                                                        curso.getHoras(),
                                                        curso.getAnio()))
                                .toList(),
                        lista(ficha.getExperiencia()).stream()
                                .map(
                                        una ->
                                                new ec.mdd.docentes.dominio.ExperienciaLaboral(
                                                        una.getCargo(),
                                                        una.getInstitucion(),
                                                        una.getCampoEspecifico(),
                                                        una.getAnios()))
                                .toList(),
                        lista(ficha.getAsignaturasAfines())));
    }

    private static Docente respuesta(
            ec.mdd.docentes.dominio.Docente docente, Referencias referencias) {
        DatosDelDocente datos = docente.datos();
        ec.mdd.docentes.dominio.FichaAcademica ficha = datos.ficha();
        return new Docente()
                .tipoIdentificacion(
                        TipoDeIdentificacion.fromValue(docente.identificacion().tipo().name()))
                .identificacion(docente.identificacion().numero())
                .nombreCompleto(datos.nombreCompleto())
                .primerApellido(datos.primerApellido())
                .segundoApellido(datos.segundoApellido())
                .primerNombre(datos.primerNombre())
                .segundoNombre(datos.segundoNombre())
                .correoInstitucional(datos.correo())
                .telefono(datos.telefono())
                .tipoPersonal(TipoPersonal.fromValue(datos.tipoPersonal().name()))
                .categoriaEscalafon(CategoriaDeEscalafon.fromValue(datos.categoria().name()))
                .dedicacion(Dedicacion.fromValue(datos.dedicacion().name()))
                .horasContratadas(datos.horasContratadas())
                .fechaIngreso(datos.fechaIngreso())
                .inicioContrato(datos.inicioContrato())
                .finContrato(datos.finContrato())
                .condicionEspecial(CondicionEspecial.fromValue(datos.condicionEspecial().name()))
                .carreraBase(datos.carreraBase())
                .nombreDeCarreraBase(referencias.carrera(datos.carreraBase()).orElse(null))
                .ficha(
                        new FichaAcademica(
                                        new TituloDeTercerNivel(
                                                        ficha.tercerNivel().titulo(),
                                                        ficha.tercerNivel().campo().amplio(),
                                                        ficha.tercerNivel().campo().especifico())
                                                .registroSenescyt(
                                                        ficha.tercerNivel().registroSenescyt()))
                                .cuartoNivel(
                                        ficha.cuartoNivel().stream()
                                                .map(
                                                        titulo ->
                                                                new TituloDeCuartoNivel(
                                                                        titulo.titulo(),
                                                                        TipoDeTituloDeCuartoNivel
                                                                                .fromValue(
                                                                                        titulo.tipo()
                                                                                                .name()),
                                                                        titulo.campoEspecifico(),
                                                                        titulo.anio()))
                                                .toList())
                                .cursos(
                                        ficha.cursos().stream()
                                                .map(
                                                        curso ->
                                                                new Curso(
                                                                        curso.nombre(),
                                                                        curso.campoEspecifico(),
                                                                        curso.horas(),
                                                                        curso.anio()))
                                                .toList())
                                .experiencia(
                                        ficha.experiencia().stream()
                                                .map(
                                                        una ->
                                                                new ExperienciaLaboral(
                                                                        una.cargo(),
                                                                        una.institucion(),
                                                                        una.campoEspecifico(),
                                                                        una.anios()))
                                                .toList())
                                .asignaturasAfines(ficha.asignaturasAfines()))
                .estado(EstadoLaboral.fromValue(docente.estado().name()))
                .camposAmplios(camposAmplios(referencias, ficha.camposAmplios()))
                .asignaturasAfines(
                        ficha.asignaturasAfines().stream()
                                .map(
                                        codigo ->
                                                new AsignaturaAfin(codigo)
                                                        .nombre(
                                                                referencias
                                                                        .asignatura(codigo)
                                                                        .orElse(null)))
                                .toList());
    }

    private static ResumenDeDocente resumen(
            ec.mdd.docentes.dominio.Docente docente, Referencias referencias) {
        DatosDelDocente datos = docente.datos();
        return new ResumenDeDocente(
                        docente.identificacion().numero(),
                        datos.nombreCompleto(),
                        datos.carreraBase(),
                        TipoPersonal.fromValue(datos.tipoPersonal().name()),
                        Dedicacion.fromValue(datos.dedicacion().name()),
                        datos.horasContratadas(),
                        CondicionEspecial.fromValue(datos.condicionEspecial().name()),
                        EstadoLaboral.fromValue(docente.estado().name()))
                .nombreDeCarreraBase(referencias.carrera(datos.carreraBase()).orElse(null));
    }

    // Los campos amplios con su nombre; uno que Académico ya no tenga no se muestra.
    private static List<CampoAmplio> camposAmplios(
            Referencias referencias, java.util.Collection<String> codigos) {
        return codigos.stream()
                .sorted(Comparator.naturalOrder())
                .flatMap(
                        codigo ->
                                referencias.campoAmplio(codigo).stream()
                                        .map(nombre -> new CampoAmplio(codigo, nombre)))
                .toList();
    }

    private static <T> List<T> lista(List<T> elementos) {
        return elementos == null ? List.of() : elementos;
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
        } catch (DocenteNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
