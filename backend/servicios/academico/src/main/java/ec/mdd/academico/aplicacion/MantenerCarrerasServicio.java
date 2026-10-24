package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CambioDeValor;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.DatosMaestros;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoDeMalla;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.puertos.entrada.AsignaturaNoEncontrada;
import ec.mdd.academico.puertos.entrada.AsignaturaRepetida;
import ec.mdd.academico.puertos.entrada.CarreraNoEncontrada;
import ec.mdd.academico.puertos.entrada.CarreraRepetida;
import ec.mdd.academico.puertos.entrada.DatosDeAsignatura;
import ec.mdd.academico.puertos.entrada.DatosDeCarrera;
import ec.mdd.academico.puertos.entrada.MallaNoEncontrada;
import ec.mdd.academico.puertos.entrada.MallaRepetida;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.SiglasRepetidas;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mantener las carreras, sus mallas y sus asignaturas: en una transacción se bloquean, se cargan
 * con la estructura orgánica y los campos amplios, el dominio valida el cambio y, si algo cambió,
 * se guarda y se registra. Los códigos y las siglas repetidos se deciden con lo que se leyó bajo el
 * candado (research §4). Sin cambios, no se registra.
 */
public final class MantenerCarrerasServicio implements MantenerCarreras {

    private final RepositorioCarreras repositorio;
    private final RepositorioEstructura estructura;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerCarrerasServicio(
            RepositorioCarreras repositorio,
            RepositorioEstructura estructura,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.estructura = estructura;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Carrera registrarCarrera(Carrera nueva, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    if (datos.carrera(nueva.codigo()).isPresent()) {
                        throw new CarreraRepetida(nueva.codigo());
                    }
                    validarSiglas(datos, nueva.siglas(), nueva.codigo());
                    datos.registrarCarrera(nueva);
                    Instant ahora = reloj.instant();
                    repositorio.registrar(nueva, quien, ahora);
                    registrar(
                            ahora,
                            quien,
                            Accion.CARRERA_REGISTRADA,
                            Map.of(
                                    "codigo", nueva.codigo(),
                                    "nombre", nueva.nombre(),
                                    "siglas", nueva.siglas(),
                                    "sede", nueva.sede()));
                    return nueva;
                });
    }

    @Override
    public Carrera corregirCarrera(String codigo, DatosDeCarrera nuevos, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    Carrera actual =
                            datos.carrera(codigo)
                                    .orElseThrow(() -> new CarreraNoEncontrada(codigo));
                    validarSiglas(datos, nuevos.siglas(), codigo);
                    Carrera.Correccion correccion =
                            datos.corregirCarrera(
                                    actual,
                                    nuevos.nombre(),
                                    nuevos.siglas(),
                                    nuevos.sede(),
                                    nuevos.nivelFormacion(),
                                    nuevos.modalidad(),
                                    nuevos.jornadas(),
                                    nuevos.campo(),
                                    nuevos.campoDetallado(),
                                    nuevos.numeroCiclos());
                    if (!correccion.cambios().isEmpty()) {
                        repositorio.guardar(correccion.carrera());
                        registrarCambios(
                                quien, Accion.CARRERA_CORREGIDA, codigo, correccion.cambios());
                    }
                    return correccion.carrera();
                });
    }

    @Override
    public Carrera cambiarEstadoCarrera(String codigo, EstadoDeCarrera estado, String quien) {
        return transaccion.ejecutar(
                () -> {
                    Carrera actual =
                            bloqueados()
                                    .carrera(codigo)
                                    .orElseThrow(() -> new CarreraNoEncontrada(codigo));
                    if (actual.estado() == estado) {
                        return actual;
                    }
                    Carrera nueva = actual.conEstado(estado);
                    repositorio.guardar(nueva);
                    registrarEstado(
                            quien,
                            Accion.CARRERA_ESTADO_CAMBIADO,
                            codigo,
                            actual.estado().name(),
                            estado.name());
                    return nueva;
                });
    }

    @Override
    public Malla registrarMalla(String carrera, String codigo, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    if (datos.carrera(carrera).isEmpty()) {
                        throw new CarreraNoEncontrada(carrera);
                    }
                    Malla nueva = Malla.nueva(codigo, carrera);
                    if (datos.malla(codigo).isPresent()) {
                        throw new MallaRepetida(codigo);
                    }
                    datos.registrarMalla(nueva);
                    Instant ahora = reloj.instant();
                    repositorio.registrar(nueva, quien, ahora);
                    registrar(
                            ahora,
                            quien,
                            Accion.MALLA_REGISTRADA,
                            Map.of("codigo", codigo, "carrera", carrera));
                    return nueva;
                });
    }

    @Override
    public Malla cambiarEstadoMalla(String codigo, EstadoDeMalla estado, String quien) {
        return transaccion.ejecutar(
                () -> {
                    Malla actual =
                            bloqueados()
                                    .malla(codigo)
                                    .orElseThrow(() -> new MallaNoEncontrada(codigo));
                    if (actual.estado() == estado) {
                        return actual;
                    }
                    Malla nueva = actual.conEstado(estado);
                    repositorio.guardar(nueva);
                    registrarEstado(
                            quien,
                            Accion.MALLA_ESTADO_CAMBIADO,
                            codigo,
                            actual.estado().name(),
                            estado.name());
                    return nueva;
                });
    }

    @Override
    public Asignatura registrarAsignatura(Asignatura nueva, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    if (datos.asignatura(nueva.codigo()).isPresent()) {
                        throw new AsignaturaRepetida(nueva.codigo());
                    }
                    datos.registrarAsignatura(nueva);
                    Instant ahora = reloj.instant();
                    repositorio.registrar(nueva, quien, ahora);
                    Map<String, Object> detalle = new LinkedHashMap<>();
                    detalle.put("codigo", nueva.codigo());
                    detalle.put("carrera", nueva.carrera());
                    detalle.put("malla", nueva.malla());
                    detalle.put("nombre", nueva.nombre());
                    detalle.put("ciclo", nueva.ciclo());
                    detalle.put(
                            "horasSemanalesClase",
                            nueva.horasSemanalesClase().stripTrailingZeros().toPlainString());
                    registrar(ahora, quien, Accion.ASIGNATURA_REGISTRADA, detalle);
                    return nueva;
                });
    }

    @Override
    public Asignatura corregirAsignatura(String codigo, DatosDeAsignatura nuevos, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    Asignatura actual =
                            datos.asignatura(codigo)
                                    .orElseThrow(() -> new AsignaturaNoEncontrada(codigo));
                    Asignatura.Correccion correccion =
                            datos.corregirAsignatura(
                                    actual,
                                    nuevos.nombre(),
                                    nuevos.ciclo(),
                                    nuevos.unidadOrganizacionCurricular(),
                                    nuevos.horasSemanalesClase(),
                                    nuevos.horasPracticasSemanales(),
                                    nuevos.creditos(),
                                    nuevos.troncoComun(),
                                    nuevos.campo(),
                                    nuevos.palabrasClave());
                    if (!correccion.cambios().isEmpty()) {
                        repositorio.guardar(correccion.asignatura());
                        registrarCambios(
                                quien, Accion.ASIGNATURA_CORREGIDA, codigo, correccion.cambios());
                    }
                    return correccion.asignatura();
                });
    }

    @Override
    public Asignatura cambiarEstadoAsignatura(String codigo, boolean activa, String quien) {
        return transaccion.ejecutar(
                () -> {
                    DatosMaestros datos = bloqueados();
                    Asignatura actual =
                            datos.asignatura(codigo)
                                    .orElseThrow(() -> new AsignaturaNoEncontrada(codigo));
                    Asignatura nueva = datos.cambiarEstadoAsignatura(actual, activa);
                    if (nueva.activa() != actual.activa()) {
                        repositorio.guardar(nueva);
                        registrar(
                                reloj.instant(),
                                quien,
                                activa
                                        ? Accion.ASIGNATURA_REACTIVADA
                                        : Accion.ASIGNATURA_INACTIVADA,
                                Map.of("codigo", codigo));
                    }
                    return nueva;
                });
    }

    private DatosMaestros bloqueados() {
        repositorio.bloquear();
        return new DatosMaestros(
                repositorio.carreras(),
                repositorio.mallas(),
                repositorio.asignaturas(),
                estructura.todas(),
                repositorio.camposAmplios().stream().map(CampoAmplio::codigo).toList());
    }

    private static void validarSiglas(DatosMaestros datos, String siglas, String codigo) {
        boolean usadas =
                datos.carreras().stream()
                        .anyMatch(
                                otra ->
                                        otra.siglas().equals(siglas)
                                                && !otra.codigo().equals(codigo));
        if (usadas) {
            throw new SiglasRepetidas(siglas);
        }
    }

    private void registrarCambios(
            String quien, Accion accion, String codigo, List<CambioDeValor> cambios) {
        registrar(
                reloj.instant(),
                quien,
                accion,
                Map.of("codigo", codigo, "cambios", cambios.stream().map(Detalles::de).toList()));
    }

    private void registrarEstado(
            String quien, Accion accion, String codigo, String anterior, String nuevo) {
        registrar(
                reloj.instant(),
                quien,
                accion,
                Map.of("codigo", codigo, "anterior", anterior, "nuevo", nuevo));
    }

    private void registrar(
            Instant momento, String quien, Accion accion, Map<String, Object> detalle) {
        registro.registrar(new CambioRegistrado(momento, quien, accion, detalle));
    }
}
