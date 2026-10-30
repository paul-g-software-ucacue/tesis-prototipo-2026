package ec.mdd.cargas.dominio;

/** Cómo terminó un intento de confirmar una carga (RF-10, glosario «Historial de cargas»). */
public enum ResultadoDeHistorial {
    /** El dueño aplicó la carga completa. */
    APLICADA,
    /** No se aplicó nada; el motivo dice por qué. */
    RECHAZADA,
    /** El mismo archivo ya estaba aplicado en esa carga: no se aplicó otra vez (MD-13). */
    REPETIDA,
    /**
     * Un periodo confirmado «sin proyectos» (F01-H05, RF-18): no hay archivo ni hash, sino el
     * periodo.
     */
    SIN_PROYECTOS
}
