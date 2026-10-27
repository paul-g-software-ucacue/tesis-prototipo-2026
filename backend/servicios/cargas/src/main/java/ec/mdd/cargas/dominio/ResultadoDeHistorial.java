package ec.mdd.cargas.dominio;

/** Cómo terminó un intento de confirmar una carga (RF-10, glosario «Historial de cargas»). */
public enum ResultadoDeHistorial {
    /** El dueño aplicó la carga completa. */
    APLICADA,
    /** No se aplicó nada; el motivo dice por qué. */
    RECHAZADA,
    /** El mismo archivo ya estaba aplicado en esa carga: no se aplicó otra vez (MD-13). */
    REPETIDA
}
