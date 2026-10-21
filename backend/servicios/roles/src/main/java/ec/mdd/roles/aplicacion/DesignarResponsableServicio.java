package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.entrada.ConsultarResponsables;
import ec.mdd.roles.puertos.entrada.DesignarResponsable;
import ec.mdd.roles.puertos.entrada.PeriodoDesconocido;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Designar al Responsable de consolidación: se bloquea la copia del periodo, para que dos
 * designaciones del mismo periodo no se crucen, y se guarda y se registra en la misma transacción.
 * Las reglas son de {@link Designacion}.
 */
public final class DesignarResponsableServicio
        implements DesignarResponsable, ConsultarResponsables {

    private static final String DOCENTE = "DOCENTE";

    private final RepositorioPeriodosConocidos periodos;
    private final RepositorioDesignaciones designaciones;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public DesignarResponsableServicio(
            RepositorioPeriodosConocidos periodos,
            RepositorioDesignaciones designaciones,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.periodos = periodos;
        this.designaciones = designaciones;
        this.asignaciones = asignaciones;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Designacion designar(String periodo, String usuario, String quien) {
        String designado = NombreDeUsuario.normalizar(usuario);
        return transaccion.ejecutar(
                () -> {
                    PeriodoConocido conocido =
                            periodos.bloquear(periodo)
                                    .orElseThrow(() -> new PeriodoDesconocido(periodo));
                    Optional<Designacion> anterior = designaciones.dePeriodo(periodo);
                    Instant ahora = reloj.instant();
                    Designacion nueva =
                            Designacion.de(
                                    conocido,
                                    designado,
                                    quien,
                                    asignaciones.rolesDe(designado),
                                    anterior,
                                    ahora);
                    if (anterior.isPresent() && anterior.get() == nueva) {
                        return nueva;
                    }
                    designaciones.guardar(nueva);
                    Map<String, Object> detalle = new LinkedHashMap<>();
                    detalle.put("periodo", periodo);
                    anterior.ifPresent(previa -> detalle.put("anterior", previa.usuario()));
                    detalle.put("nuevo", nueva.usuario());
                    registro.registrar(
                            new CambioDeRoles(
                                    ahora,
                                    quien,
                                    CambioDeRoles.Accion.DESIGNAR_RESPONSABLE,
                                    detalle));
                    return nueva;
                });
    }

    @Override
    public List<Designacion> responsables() {
        return designaciones.todas();
    }

    @Override
    public List<String> docentes() {
        return asignaciones.todas().entrySet().stream()
                .filter(roles -> roles.getValue().contains(DOCENTE))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
