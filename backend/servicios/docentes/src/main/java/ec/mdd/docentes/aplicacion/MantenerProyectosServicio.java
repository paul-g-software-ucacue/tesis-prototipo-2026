package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ProyectoRepetido;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import ec.mdd.docentes.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mantener los proyectos aprobados (F03-H05, RF-18). Las carreras se verifican con Académico antes
 * de la transacción; dentro de ella se bloquean los proyectos, se verifican el director y los
 * participantes con Docentes y el código repetido, y se guarda y se registra.
 */
public final class MantenerProyectosServicio implements MantenerProyectos {

    private final RepositorioProyectos repositorio;
    private final RepositorioDocentes docentes;
    private final ReferenciasAcademicas referencias;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerProyectosServicio(
            RepositorioProyectos repositorio,
            RepositorioDocentes docentes,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.docentes = docentes;
        this.referencias = referencias;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ProyectoConsultado registrar(Proyecto proyecto, String quien) {
        referencias.referencias().verificar(proyecto);
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    proyecto.verificarDocentes(uno -> docentes.buscar(uno).isPresent());
                    if (repositorio.buscar(proyecto.codigo()).isPresent()) {
                        throw new ProyectoRepetido();
                    }
                    Instant ahora = reloj.instant();
                    repositorio.registrar(proyecto, quien, ahora);
                    Map<String, Object> detalle = clave(proyecto);
                    detalle.put("tipo", proyecto.tipo().name());
                    detalle.put("estado", proyecto.estado().name());
                    registrar(ahora, quien, Accion.PROYECTO_REGISTRADO, detalle);
                    return consultado(proyecto);
                });
    }

    @Override
    public ProyectoConsultado corregir(Proyecto proyecto, String quien) {
        referencias.referencias().verificar(proyecto);
        return transaccion.ejecutar(
                () -> {
                    repositorio.bloquear();
                    Proyecto actual =
                            repositorio
                                    .buscar(proyecto.codigo())
                                    .orElseThrow(ProyectoNoEncontrado::new);
                    Proyecto.Correccion correccion = actual.corregido(proyecto);
                    if (correccion.cambios().isEmpty()) {
                        return consultado(actual);
                    }
                    proyecto.verificarDocentes(uno -> docentes.buscar(uno).isPresent());
                    repositorio.guardar(proyecto);
                    Map<String, Object> detalle = clave(proyecto);
                    detalle.put(
                            "cambios", correccion.cambios().stream().map(Detalles::de).toList());
                    registrar(reloj.instant(), quien, Accion.PROYECTO_CORREGIDO, detalle);
                    return consultado(proyecto);
                });
    }

    private ProyectoConsultado consultado(Proyecto proyecto) {
        return new ProyectoConsultado(
                proyecto, NombresDeDocentes.de(proyecto, docentes::buscar), Optional.empty());
    }

    private static Map<String, Object> clave(Proyecto proyecto) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("codigo", proyecto.codigo());
        return detalle;
    }

    private void registrar(
            Instant momento, String quien, Accion accion, Map<String, Object> detalle) {
        registro.registrar(new CambioRegistrado(momento, quien, accion, detalle));
    }
}
