package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.CarreraSinRol;
import ec.mdd.roles.puertos.entrada.UsuarioSinCarrera;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * La carrera de cada coordinador, con su registro. El código no se comprueba en Académico, que está
 * en otro proceso (D-76): la pantalla ofrece las vigentes, y un código sin carrera no abre nada.
 */
public final class AsignarCarreraServicio implements AsignarCarrera {

    private final RepositorioCarrerasDeCoordinadores carreras;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida y el reloj. */
    public AsignarCarreraServicio(
            RepositorioCarrerasDeCoordinadores carreras,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.carreras = carreras;
        this.asignaciones = asignaciones;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Map<String, String> carreras() {
        return carreras.todas();
    }

    @Override
    public void indicar(String usuario, String carrera, String quien) {
        String nombre = NombreDeUsuario.normalizar(usuario);
        if (!asignaciones.rolesDe(nombre).contains(ROL)) {
            throw new CarreraSinRol();
        }
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    Optional<String> previa = carreras.de(nombre);
                    // La misma carrera no cambia nada: no se guarda ni se registra (D-94).
                    if (previa.filter(carrera::equals).isPresent()) {
                        return carrera;
                    }
                    Map<String, Object> anterior =
                            previa.<Map<String, Object>>map(una -> Map.of("carrera", una))
                                    .orElse(Map.of());
                    carreras.guardar(nombre, carrera, quien, ahora);
                    registro.registrar(
                            new CambioDeRoles(
                                    ahora,
                                    quien,
                                    CambioDeRoles.Accion.INDICAR_CARRERA,
                                    Map.of("usuario", nombre, "carrera", carrera),
                                    anterior,
                                    Map.of("carrera", carrera)));
                    return carrera;
                });
    }

    @Override
    public void quitar(String usuario, String quien) {
        String nombre = NombreDeUsuario.normalizar(usuario);
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    String previa = carreras.de(nombre).orElseThrow(UsuarioSinCarrera::new);
                    if (!carreras.quitar(nombre)) {
                        throw new UsuarioSinCarrera();
                    }
                    registro.registrar(
                            new CambioDeRoles(
                                    ahora,
                                    quien,
                                    CambioDeRoles.Accion.QUITAR_CARRERA,
                                    Map.of("usuario", nombre),
                                    Map.of("carrera", previa),
                                    Map.of()));
                    return nombre;
                });
    }
}
