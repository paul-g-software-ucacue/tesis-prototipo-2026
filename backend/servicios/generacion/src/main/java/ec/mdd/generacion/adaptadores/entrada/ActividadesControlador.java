package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.ActividadAsignada;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadGuardada;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadNueva;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadPosible;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadesApi;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadesDeCarrera;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadesDelDocente;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadesDelPeriodo;
import ec.mdd.generacion.adaptadores.entrada.api.ActividadesPosibles;
import ec.mdd.generacion.adaptadores.entrada.api.Componente;
import ec.mdd.generacion.adaptadores.entrada.api.HorasPorComponente;
import ec.mdd.generacion.adaptadores.entrada.api.ProyectoPosible;
import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.HorasNoValidas;
import ec.mdd.generacion.dominio.SumaPorComponente;
import ec.mdd.generacion.puertos.entrada.ActividadQuitada;
import ec.mdd.generacion.puertos.entrada.AsignarActividad;
import ec.mdd.generacion.puertos.entrada.ConsultarActividades;
import ec.mdd.generacion.puertos.entrada.ConsultarActividadesPosibles;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.SolicitudDeActividad;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las actividades no lectivas ({@code contratos/generacion/openapi.yaml}, RF-26). Las asigna quien
 * escribe la oferta, y su alcance se mira sobre ese permiso (D-64). {@code PermisosGeneracionTest}
 * lo comprueba. Algunos modelos del contrato se llaman como los de los puertos: esos van con su
 * nombre completo.
 */
@RestController
class ActividadesControlador implements ActividadesApi {

    private static final String LEER =
            "hasAnyAuthority('PERMISO_OFERTA:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";
    private static final String ESCRIBIR = "hasAuthority('PERMISO_OFERTA:ESCRITURA')";
    private static final String OTRA_CARRERA = "El parcial es de otra carrera";

    private final AsignarActividad asignar;
    private final ConsultarActividades consultar;
    private final ConsultarActividadesPosibles posibles;

    ActividadesControlador(
            AsignarActividad asignar,
            ConsultarActividades consultar,
            ConsultarActividadesPosibles posibles) {
        this.asignar = asignar;
        this.consultar = consultar;
        this.posibles = posibles;
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<ActividadesDelPeriodo> consultarActividades(String periodo) {
        ec.mdd.generacion.puertos.entrada.ActividadesDelPeriodo delPeriodo =
                consultar.actividades(periodo, AfinidadControlador.carreraPropia());
        return ResponseEntity.ok(
                new ActividadesDelPeriodo(
                        delPeriodo.creado(),
                        delPeriodo.carreras().stream()
                                .map(ActividadesControlador::carrera)
                                .toList()));
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<ActividadesPosibles> consultarActividadesPosibles(
            String periodo, String docente) {
        return ResponseEntity.ok(
                new ActividadesPosibles(
                        docente,
                        posibles.posibles(periodo, docente).stream()
                                .map(ActividadesControlador::posible)
                                .toList()));
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<ActividadGuardada> asignarActividad(
            String periodo,
            String carrera,
            String docente,
            String actividad,
            ActividadNueva nueva) {
        try {
            ec.mdd.generacion.puertos.entrada.ActividadGuardada guardada =
                    asignar.asignar(
                            new SolicitudDeActividad(
                                    periodo,
                                    carrera,
                                    docente,
                                    actividad,
                                    nueva.getHoras(),
                                    nueva.getProyecto(),
                                    DistributivoControlador.carreraDeQuienEscribe(),
                                    DistributivoControlador.autenticacion().getName()));
            return ResponseEntity.ok(
                    new ActividadGuardada(
                            asignada(guardada.actividad()), guardada.horasSuficientes()));
        } catch (OtraCarrera error) {
            throw new AccessDeniedException(OTRA_CARRERA, error);
        } catch (HorasNoValidas error) {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    "horas",
                                    "Escribe unas horas mayores que 0 y hasta 40, con hasta dos"
                                            + " decimales.")));
        }
    }

    @Override
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<Void> quitarActividad(
            String periodo, String carrera, String docente, String actividad, String proyecto) {
        try {
            asignar.quitar(
                    new ActividadQuitada(
                            periodo,
                            carrera,
                            docente,
                            actividad,
                            proyecto,
                            DistributivoControlador.carreraDeQuienEscribe(),
                            DistributivoControlador.autenticacion().getName()));
        } catch (OtraCarrera error) {
            throw new AccessDeniedException(OTRA_CARRERA, error);
        }
        return ResponseEntity.noContent().build();
    }

    private static ActividadesDeCarrera carrera(
            ec.mdd.generacion.puertos.entrada.ActividadesDeCarrera una) {
        return new ActividadesDeCarrera(
                una.carrera(),
                una.docentes().stream().map(ActividadesControlador::docente).toList());
    }

    private static ActividadesDelDocente docente(
            ec.mdd.generacion.puertos.entrada.ActividadesDelDocente uno) {
        return new ActividadesDelDocente(
                uno.docente(),
                uno.nombreDocente(),
                uno.actividades().stream().map(ActividadesControlador::asignada).toList(),
                horas(uno.suma()));
    }

    private static HorasPorComponente horas(SumaPorComponente suma) {
        return new HorasPorComponente(
                suma.otrasDocencia(), suma.investigacion(), suma.vinculacion(), suma.gestion());
    }

    private static ActividadAsignada asignada(ActividadNoLectiva actividad) {
        ActividadAsignada asignada =
                new ActividadAsignada(
                        actividad.actividad(),
                        actividad.nombreActividad(),
                        Componente.fromValue(actividad.componente().name()),
                        actividad.horas());
        asignada.setProyecto(actividad.proyecto());
        asignada.setNombreProyecto(actividad.nombreProyecto());
        return asignada;
    }

    private static ActividadPosible posible(
            ec.mdd.generacion.puertos.entrada.ActividadPosible una) {
        ActividadPosible posible =
                new ActividadPosible(
                        una.actividad().codigo(),
                        una.actividad().nombre(),
                        Componente.fromValue(una.actividad().componente().name()),
                        una.disponible(),
                        una.actividad().exigeProyecto(),
                        una.proyectos().stream()
                                .map(
                                        proyecto ->
                                                new ProyectoPosible(
                                                        proyecto.codigo(), proyecto.nombre()))
                                .toList());
        posible.setMotivo(una.motivo().orElse(null));
        return posible;
    }
}
