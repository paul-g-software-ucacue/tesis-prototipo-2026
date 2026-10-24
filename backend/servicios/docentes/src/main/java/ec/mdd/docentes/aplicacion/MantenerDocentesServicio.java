package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.puertos.entrada.CorreoRepetido;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.DocenteRepetido;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mantener los docentes. Las referencias de Académico se piden antes de la transacción, porque cada
 * servicio tiene la suya (principio V); el dominio las verifica. Dentro de la transacción se
 * bloquean los docentes, se deciden la identificación y el correo repetidos con lo que se leyó
 * (research §5) y, si algo cambió, se guarda y se registra. Sin cambios, no se registra.
 */
public final class MantenerDocentesServicio implements MantenerDocentes {

    private final RepositorioDocentes repositorio;
    private final ReferenciasAcademicas referencias;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerDocentesServicio(
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.referencias = referencias;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Docente registrar(Identificacion identificacion, DatosDelDocente datos, String quien) {
        Docente nuevo = Docente.nuevo(identificacion, datos, Year.now(reloj));
        referencias.referencias().verificar(datos);
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    if (repositorio.buscar(identificacion.numero()).isPresent()) {
                        throw new DocenteRepetido();
                    }
                    if (repositorio.existeCorreo(datos.correo(), null)) {
                        throw new CorreoRepetido();
                    }
                    Instant ahora = reloj.instant();
                    repositorio.registrar(nuevo, quien, ahora);
                    Map<String, Object> detalle = clave(nuevo);
                    detalle.put("tipoPersonal", datos.tipoPersonal().name());
                    detalle.put("dedicacion", datos.dedicacion().name());
                    detalle.put("carreraBase", datos.carreraBase());
                    registrar(ahora, quien, Accion.DOCENTE_REGISTRADO, detalle);
                    return nuevo;
                });
    }

    @Override
    public Docente corregir(String identificacion, DatosDelDocente datos, String quien) {
        referencias.referencias().verificar(datos);
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    Docente actual = existente(identificacion);
                    Docente.Correccion correccion = actual.corregido(datos, Year.now(reloj));
                    if (correccion.cambios().isEmpty()) {
                        return actual;
                    }
                    if (repositorio.existeCorreo(datos.correo(), identificacion)) {
                        throw new CorreoRepetido();
                    }
                    repositorio.guardar(correccion.docente());
                    Map<String, Object> detalle = clave(actual);
                    detalle.put(
                            "cambios", correccion.cambios().stream().map(Detalles::de).toList());
                    registrar(reloj.instant(), quien, Accion.DOCENTE_CORREGIDO, detalle);
                    return correccion.docente();
                });
    }

    @Override
    public Docente cambiarEstado(String identificacion, EstadoLaboral estado, String quien) {
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    Docente actual = existente(identificacion);
                    if (actual.estado() == estado) {
                        return actual;
                    }
                    Docente cambiado = actual.conEstado(estado);
                    repositorio.guardar(cambiado);
                    Map<String, Object> detalle = clave(actual);
                    detalle.put("anterior", actual.estado().name());
                    detalle.put("nuevo", estado.name());
                    registrar(reloj.instant(), quien, Accion.DOCENTE_ESTADO_CAMBIADO, detalle);
                    return cambiado;
                });
    }

    private Docente existente(String identificacion) {
        return repositorio.buscar(identificacion).orElseThrow(DocenteNoEncontrado::new);
    }

    private static Map<String, Object> clave(Docente docente) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("identificacion", docente.identificacion().numero());
        return detalle;
    }

    private void registrar(
            Instant momento, String quien, Accion accion, Map<String, Object> detalle) {
        registro.registrar(new CambioRegistrado(momento, quien, accion, detalle));
    }
}
