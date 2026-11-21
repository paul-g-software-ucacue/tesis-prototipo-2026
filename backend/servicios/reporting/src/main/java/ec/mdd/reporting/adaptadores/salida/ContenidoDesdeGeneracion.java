package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.FilaDeActividad;
import ec.mdd.reporting.dominio.FilaDeAsignatura;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * El contenido congelado por las rutas internas de Generación, en el proceso distributivo (ADR-023,
 * F20-H01): {@code /interno/generacion/periodos/{periodo}/versiones/{numero}/cargas}. Un 404 es un
 * resultado vacío.
 *
 * <p>La unidad de una actividad es su proyecto, si lo tiene, o la carrera que la registró.
 */
public final class ContenidoDesdeGeneracion implements ContenidoDeLaVersion {

    private final ConsultasEntreProcesos consultas;
    private final URI distributivo;

    /**
     * @param consultas el cliente con token de proceso
     * @param distributivo la dirección del proceso distributivo
     */
    public ContenidoDesdeGeneracion(ConsultasEntreProcesos consultas, URI distributivo) {
        this.consultas = consultas;
        this.distributivo = distributivo;
    }

    @Override
    public Optional<DocentesDeLaVersion> docentes(String periodo, int version) {
        return consultas
                .obtener(cargas(periodo, version).build().encode().toUri(), Cargas.class)
                .map(
                        cargas ->
                                new DocentesDeLaVersion(
                                        cargas.hash(),
                                        cargas.docentes().stream()
                                                .map(
                                                        una ->
                                                                new DocentesDeLaVersion.Docente(
                                                                        una.identificacion(),
                                                                        una.nombre()))
                                                .toList()));
    }

    @Override
    public Optional<ContenidoDelDocente> contenido(
            String periodo, int version, String identificacion) {
        return consultas
                .obtener(
                        cargas(periodo, version)
                                .pathSegment(identificacion)
                                .build()
                                .encode()
                                .toUri(),
                        Carga.class)
                .map(ContenidoDesdeGeneracion::contenido);
    }

    private UriComponentsBuilder cargas(String periodo, int version) {
        return UriComponentsBuilder.fromUri(distributivo)
                .pathSegment(
                        "interno",
                        "generacion",
                        "periodos",
                        periodo,
                        "versiones",
                        String.valueOf(version),
                        "cargas");
    }

    private static ContenidoDelDocente contenido(Carga carga) {
        return new ContenidoDelDocente(
                carga.hash(),
                new DatosDelDocente(
                        carga.docente().identificacion(),
                        carga.docente().nombre(),
                        carga.docente().dedicacion() == null
                                ? null
                                : Dedicacion.valueOf(carga.docente().dedicacion()),
                        carga.docente().horasContratadas()),
                carga.asignaturas().stream()
                        .map(
                                una ->
                                        new FilaDeAsignatura(
                                                una.nombreCarrera(),
                                                una.codigo(),
                                                una.nombre(),
                                                una.paralelo(),
                                                una.horas()))
                        .toList(),
                carga.actividades().stream()
                        .map(
                                una ->
                                        new FilaDeActividad(
                                                Componente.valueOf(una.componente()),
                                                una.codigo(),
                                                una.nombre(),
                                                una.nombreProyecto() != null
                                                        ? una.nombreProyecto()
                                                        : una.carrera(),
                                                una.horas()))
                        .toList(),
                new HorasPorComponente(
                        carga.horas().clase(),
                        carga.horas().otrasDocencia(),
                        carga.horas().investigacion(),
                        carga.horas().vinculacion(),
                        carga.horas().gestion()));
    }

    /** Los docentes de una versión, como los responde Generación. */
    record Cargas(String hash, List<Docente> docentes) {}

    /** Un docente de la lista. */
    record Docente(String identificacion, String nombre) {}

    /** La carga congelada de un docente, como la responde Generación. */
    record Carga(
            String hash,
            DocenteDeLaCarga docente,
            List<Asignatura> asignaturas,
            List<Actividad> actividades,
            Horas horas) {}

    /** Los datos del docente. */
    record DocenteDeLaCarga(
            String identificacion, String nombre, String dedicacion, Integer horasContratadas) {}

    /** Una asignatura-paralelo. */
    record Asignatura(
            String codigo,
            String nombre,
            String paralelo,
            String carrera,
            String nombreCarrera,
            BigDecimal horas) {}

    /** Una actividad no lectiva. */
    record Actividad(
            String codigo,
            String nombre,
            String componente,
            String carrera,
            String proyecto,
            String nombreProyecto,
            BigDecimal horas) {}

    /** Las horas por componente. */
    record Horas(
            BigDecimal clase,
            BigDecimal otrasDocencia,
            BigDecimal investigacion,
            BigDecimal vinculacion,
            BigDecimal gestion) {}
}
