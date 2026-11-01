package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CalculadoraAfinidad;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.HorasDelPeriodo;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.ProponerDocente;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDePropuesta;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
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
 * Propone y quita docentes. El puntaje se recalcula aquí: la justificación no depende de lo que
 * diga la pantalla. Sin horas suficientes se guarda igual, con un aviso, hasta F07-H02 (D-78).
 * Primero consulta y después guarda, para no abrir una transacción durante una llamada a otro
 * proceso.
 */
public final class ProponerDocenteServicio implements ProponerDocente {

    private final DatosParaSugerencia datos;
    private final DatosParaAfinidad afinidad;
    private final RepositorioDistributivos distributivos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;
    private final CalculadoraAfinidad calculadora = new CalculadoraAfinidad();

    /** Recibe los puertos de salida y el reloj. */
    public ProponerDocenteServicio(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.datos = datos;
        this.afinidad = afinidad;
        this.distributivos = distributivos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public PropuestaVista proponer(SolicitudDePropuesta solicitud) {
        String periodo = solicitud.periodo();
        AsignaturaParalelo uno =
                paraleloDeSuCarrera(
                        periodo, solicitud.asignatura(), solicitud.paralelo(), solicitud.carrera());
        if (uno.retirada()) {
            throw new ParaleloRetirado();
        }
        if (uno.periodoCerrado()) {
            throw new PeriodoCerrado();
        }
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(SinDistributivo::new);
        version.exigirEditable(uno.carrera());
        PesosDelPeriodo pesos = afinidad.pesos(periodo).orElseThrow(PeriodoSinAbrir::new);
        DocenteActivo docente =
                datos.docentesActivos().stream()
                        .filter(activo -> activo.identificacion().equals(solicitud.docente()))
                        .findFirst()
                        .orElseThrow(DocenteNoActivo::new);
        BigDecimal puntaje =
                calculadora.calcular(docente.formacion(), uno.asignatura(), pesos.pesos()).total();
        Justificacion justificacion =
                Justificacion.para(
                        puntaje, pesos.minimoAfinidad(), uno.troncoComun(), solicitud.texto());
        List<AsignaturaParalelo> oferta = datos.ofertaDelPeriodo(periodo).orElse(List.of());
        HorasDelDocente horas =
                HorasDelPeriodo.calcular(version.propuestas(), version.actividades(), oferta, uno)
                        .getOrDefault(docente.identificacion(), HorasDelDocente.NINGUNA);
        Instant ahora = reloj.instant();
        Propuesta propuesta =
                new Propuesta(
                        uno.asignatura().codigo(),
                        uno.paralelo(),
                        uno.carrera(),
                        docente.identificacion(),
                        docente.nombre(),
                        justificacion,
                        ahora,
                        solicitud.quien());
        Optional<Propuesta> anterior =
                version.propuestaDe(propuesta.asignatura(), propuesta.paralelo());
        transaccion.ejecutar(
                () -> {
                    if (version.parcialDe(uno.carrera()).isEmpty()) {
                        distributivos.agregarParcial(
                                version.id(), ParcialCarrera.nuevo(uno.carrera()));
                    }
                    distributivos.guardarPropuesta(version.id(), propuesta);
                    registro.registrar(
                            cambioGuardado(periodo, propuesta, anterior, solicitud.quien(), ahora));
                    return propuesta;
                });
        return new PropuestaVista(
                propuesta,
                false,
                horas.disponibles(docente.horasContratadas()).compareTo(uno.horasSemanales()) >= 0);
    }

    @Override
    public void quitar(
            String periodo,
            String asignatura,
            String paralelo,
            Optional<String> carrera,
            String quien) {
        AsignaturaParalelo uno = paraleloDeSuCarrera(periodo, asignatura, paralelo, carrera);
        if (uno.periodoCerrado()) {
            throw new PeriodoCerrado();
        }
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(PropuestaNoEncontrada::new);
        version.exigirEditable(uno.carrera());
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    if (!distributivos.quitarPropuesta(version.id(), asignatura, paralelo)) {
                        throw new PropuestaNoEncontrada();
                    }
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.PROPUESTA_QUITADA,
                                    Map.of(
                                            "periodo", periodo,
                                            "asignatura", asignatura,
                                            "paralelo", paralelo)));
                    return asignatura;
                });
    }

    private AsignaturaParalelo paraleloDeSuCarrera(
            String periodo, String asignatura, String paralelo, Optional<String> carrera) {
        AsignaturaParalelo uno =
                datos.paralelo(periodo, asignatura, paralelo)
                        .orElseThrow(ParaleloNoEncontrado::new);
        if (carrera.isPresent() && !carrera.get().equals(uno.carrera())) {
            throw new OtraCarrera();
        }
        return uno;
    }

    private static CambioDeDistributivo cambioGuardado(
            String periodo,
            Propuesta propuesta,
            Optional<Propuesta> anterior,
            String quien,
            Instant ahora) {
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("periodo", periodo);
        detalle.put("asignatura", propuesta.asignatura());
        detalle.put("paralelo", propuesta.paralelo());
        detalle.put("docente", propuesta.docente());
        detalle.put("justificacion", propuesta.justificacion().tipo().name());
        anterior.ifPresent(una -> detalle.put("docenteAnterior", una.docente()));
        return new CambioDeDistributivo(
                ahora, quien, CambioDeDistributivo.Accion.PROPUESTA_GUARDADA, detalle);
    }
}
