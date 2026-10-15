package ec.mdd.generacion.puertos.entrada;

import java.util.UUID;

/** Publica el evento de prueba hacia Usuarios, para demostrar la entrega entre procesos (D-48). */
public interface PublicarEventoDePrueba {

    /**
     * Guarda una fila y publica {@code generacion.EventoDePrueba} en la misma transacción.
     *
     * @return el identificador del evento publicado
     */
    UUID publicar();
}
