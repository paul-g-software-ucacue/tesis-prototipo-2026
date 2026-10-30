package ec.mdd.cargas.puertos.salida;

import ec.mdd.cargas.dominio.ConfirmacionSinProyectos;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import java.util.List;
import java.util.Optional;

/** Dónde queda el historial de las cargas (RF-10, FR-008). */
public interface HistorialDeCargas {

    /** Guarda un intento de confirmar. */
    void registrar(EntradaDeHistorial entrada);

    /** Los intentos de una carga, del más reciente al más antiguo. */
    List<EntradaDeHistorial> deLaCarga(String carga);

    /** La primera vez que ese archivo se aplicó en esa carga, si se aplicó (MD-13). */
    Optional<EntradaDeHistorial> aplicada(String carga, String hash);

    /** Las confirmaciones «sin proyectos» de la carga, por periodo (F01-H05). */
    List<EntradaDeHistorial> sinProyectos(String carga);

    /**
     * Guarda la confirmación «sin proyectos» de su periodo, o devuelve la que ya estaba sin
     * duplicarla (F01-H05).
     */
    ConfirmacionSinProyectos confirmarSinProyectos(EntradaDeHistorial confirmacion);
}
