package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.ConsultarDestinatarios;
import ec.mdd.roles.puertos.entrada.Destinatario;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * A quién toca un mensaje de Notificaciones (F16-H01, D-108): todos los usuarios con el rol; para
 * el Responsable de consolidación, además, el designado del periodo; y con una carrera, solo sus
 * coordinadores. Solo lee.
 */
public final class ConsultarDestinatariosServicio implements ConsultarDestinatarios {

    private final RepositorioAsignacionesDeRol asignaciones;
    private final RepositorioDesignaciones designaciones;
    private final RepositorioCarrerasDeCoordinadores carreras;

    /** Recibe los repositorios. */
    public ConsultarDestinatariosServicio(
            RepositorioAsignacionesDeRol asignaciones,
            RepositorioDesignaciones designaciones,
            RepositorioCarrerasDeCoordinadores carreras) {
        this.asignaciones = asignaciones;
        this.designaciones = designaciones;
        this.carreras = carreras;
    }

    @Override
    public List<Destinatario> destinatarios(String rol, String periodo, Optional<String> carrera) {
        List<String> conElRol =
                asignaciones.todas().entrySet().stream()
                        .filter(usuario -> usuario.getValue().contains(rol))
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();
        if (Designacion.ROL.equals(rol)) {
            // El rol fijo y, además, el designado del periodo, que tiene el rol por la designación.
            TreeSet<String> responsables = new TreeSet<>(conElRol);
            designaciones.dePeriodo(periodo).ifPresent(una -> responsables.add(una.usuario()));
            return responsables.stream()
                    .map(usuario -> new Destinatario(usuario, Optional.empty()))
                    .toList();
        }
        if (AsignarCarrera.ROL.equals(rol) && carrera.isPresent()) {
            Map<String, String> deCadaUno = carreras.todas();
            return conElRol.stream()
                    .filter(usuario -> carrera.get().equals(deCadaUno.get(usuario)))
                    .map(usuario -> new Destinatario(usuario, carrera))
                    .toList();
        }
        return conElRol.stream()
                .map(usuario -> new Destinatario(usuario, Optional.empty()))
                .toList();
    }
}
