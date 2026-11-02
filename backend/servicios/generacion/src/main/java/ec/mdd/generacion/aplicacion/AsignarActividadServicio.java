package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadGuardada;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.ActividadQuitada;
import ec.mdd.generacion.puertos.entrada.AsignarActividad;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDeActividad;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Asigna y quita actividades no lectivas. Lo que exige cada una lo decide el dominio, con los datos
 * de sus dueños. Sin horas suficientes se guarda igual, con un aviso, hasta F07-H02 (D-78). Primero
 * consulta y después guarda, para no abrir una transacción durante una llamada a otro proceso.
 */
public final class AsignarActividadServicio implements AsignarActividad {

    private final DatosParaSugerencia datos;
    private final DatosParaActividades actividades;
    private final RepositorioDistributivos distributivos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida y el reloj. */
    public AsignarActividadServicio(
            DatosParaSugerencia datos,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.datos = datos;
        this.actividades = actividades;
        this.distributivos = distributivos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ActividadGuardada asignar(SolicitudDeActividad solicitud) {
        String periodo = solicitud.periodo();
        String carrera = solicitud.carrera();
        exigirSuCarrera(carrera, solicitud.carreraPropia());
        exigirPeriodoAbierto(periodo);
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(SinDistributivo::new);
        version.exigirEditable(carrera);
        ActividadDelCatalogo delCatalogo =
                actividades.catalogo(periodo).orElseThrow(PeriodoSinAbrir::new).stream()
                        .filter(una -> una.codigo().equals(solicitud.actividad()))
                        .findFirst()
                        .orElseThrow(ActividadFueraDelCatalogo::new);
        DocenteActivo docente =
                datos.docentesActivos().stream()
                        .filter(activo -> activo.identificacion().equals(solicitud.docente()))
                        .findFirst()
                        .orElseThrow(DocenteNoActivo::new);
        ProyectosYCargosDelDocente delDocente =
                actividades
                        .proyectosYCargos(docente.identificacion(), periodo)
                        .orElseThrow(DocenteNoActivo::new);
        Optional<ProyectoDelDocente> proyecto =
                delCatalogo.exigir(solicitud.proyecto(), delDocente);
        Instant ahora = reloj.instant();
        ActividadNoLectiva actividad =
                new ActividadNoLectiva(
                        carrera,
                        docente.identificacion(),
                        docente.nombre(),
                        delCatalogo.codigo(),
                        delCatalogo.nombre(),
                        delCatalogo.componente(),
                        proyecto.map(ProyectoDelDocente::codigo).orElse(null),
                        proyecto.map(ProyectoDelDocente::nombre).orElse(null),
                        solicitud.horas(),
                        ahora,
                        solicitud.quien());
        ReglasDeLaAsignacion reglas =
                new ReglasDeLaAsignacion(
                        docente,
                        delDocente,
                        datos.ofertaDelPeriodo(periodo).orElse(List.of()),
                        actividades.reglas(periodo).orElseThrow(PeriodoSinAbrir::new));
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo actual =
                            distributivos
                                    .bloquearVigente(periodo)
                                    .orElseThrow(SinDistributivo::new);
                    actual.exigirEditable(carrera);
                    reglas.exigir(actual, actual.conActividad(actividad));
                    if (actual.parcialDe(carrera).isEmpty()) {
                        distributivos.agregarParcial(actual.id(), ParcialCarrera.nuevo(carrera));
                    }
                    Optional<ActividadNoLectiva> anterior =
                            actual.actividadDe(
                                    carrera,
                                    docente.identificacion(),
                                    delCatalogo.codigo(),
                                    actividad.proyecto());
                    distributivos.guardarActividad(actual.id(), actividad);
                    registro.registrar(cambioGuardado(periodo, actividad, anterior));
                    return new ActividadGuardada(actividad);
                });
    }

    @Override
    public void quitar(ActividadQuitada quitada) {
        String periodo = quitada.periodo();
        exigirSuCarrera(quitada.carrera(), quitada.carreraPropia());
        exigirPeriodoAbierto(periodo);
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(ActividadNoEncontrada::new);
        version.exigirEditable(quitada.carrera());
        Optional<ActividadNoLectiva> anterior =
                version.actividadDe(
                        quitada.carrera(),
                        quitada.docente(),
                        quitada.actividad(),
                        quitada.proyecto());
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    if (!distributivos.quitarActividad(
                            version.id(),
                            quitada.carrera(),
                            quitada.docente(),
                            quitada.actividad(),
                            quitada.proyecto())) {
                        throw new ActividadNoEncontrada();
                    }
                    registro.registrar(cambioQuitado(quitada, anterior, ahora));
                    return quitada;
                });
    }

    private static void exigirSuCarrera(String carrera, Optional<String> carreraPropia) {
        if (carreraPropia.isPresent() && !carreraPropia.get().equals(carrera)) {
            throw new OtraCarrera();
        }
    }

    private void exigirPeriodoAbierto(String periodo) {
        if (datos.periodoListo(periodo).orElseThrow(PeriodoNoEncontrado::new).cerrado()) {
            throw new PeriodoCerrado();
        }
    }

    private static CambioDeDistributivo cambioGuardado(
            String periodo, ActividadNoLectiva actividad, Optional<ActividadNoLectiva> anterior) {
        Map<String, Object> detalle =
                detalle(
                        periodo,
                        actividad.carrera(),
                        actividad.docente(),
                        actividad.actividad(),
                        actividad.proyecto());
        detalle.put("horas", texto(actividad.horas()));
        anterior.ifPresent(una -> detalle.put("horasAnteriores", texto(una.horas())));
        return new CambioDeDistributivo(
                actividad.asignadaEn(),
                actividad.asignadaPor(),
                CambioDeDistributivo.Accion.ACTIVIDAD_GUARDADA,
                detalle);
    }

    private static CambioDeDistributivo cambioQuitado(
            ActividadQuitada quitada, Optional<ActividadNoLectiva> anterior, Instant ahora) {
        Map<String, Object> detalle =
                detalle(
                        quitada.periodo(),
                        quitada.carrera(),
                        quitada.docente(),
                        quitada.actividad(),
                        quitada.proyecto());
        anterior.ifPresent(una -> detalle.put("horasAnteriores", texto(una.horas())));
        return new CambioDeDistributivo(
                ahora, quitada.quien(), CambioDeDistributivo.Accion.ACTIVIDAD_QUITADA, detalle);
    }

    private static Map<String, Object> detalle(
            String periodo, String carrera, String docente, String actividad, String proyecto) {
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("periodo", periodo);
        detalle.put("carrera", carrera);
        detalle.put("docente", docente);
        detalle.put("actividad", actividad);
        if (proyecto != null) {
            detalle.put("proyecto", proyecto);
        }
        return detalle;
    }

    private static String texto(BigDecimal horas) {
        return horas.stripTrailingZeros().toPlainString();
    }
}
