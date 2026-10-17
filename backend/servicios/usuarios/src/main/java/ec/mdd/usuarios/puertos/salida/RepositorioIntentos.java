package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.EstadoDeIntentos;
import java.time.Instant;

/**
 * Los intentos fallidos de inicio de sesión por nombre intentado ({@code usuarios.intento_fallido},
 * D-60). El nombre llega siempre normalizado.
 */
public interface RepositorioIntentos {

    /** Los intentos del nombre; {@link EstadoDeIntentos#SIN_INTENTOS} si no tiene. */
    EstadoDeIntentos leer(String nombre);

    /**
     * Suma un intento fallido de forma atómica: con dos intentos simultáneos, los dos cuentan.
     *
     * @return los intentos fallidos seguidos, contando este
     */
    int sumarFallo(String nombre, Instant ahora);

    /** Bloquea el nombre hasta el instante dado. */
    void bloquear(String nombre, Instant hasta, Instant ahora);

    /** Borra los intentos del nombre: inicio correcto o bloqueo vencido. */
    void borrar(String nombre);

    /**
     * Borra los nombres sin bloqueo vigente y sin intentos desde antes del instante dado, para que
     * los nombres inventados no hagan crecer la tabla.
     *
     * @return cuántos borró
     */
    int limpiar(Instant sinActividadDesde, Instant ahora);
}
