package ec.mdd.cargas.dominio;

/**
 * El resultado de confirmar «sin proyectos» un periodo (F01-H05).
 *
 * @param entrada la confirmación en el historial
 * @param nueva si se guardó ahora; falso si el periodo ya estaba confirmado
 */
public record ConfirmacionSinProyectos(EntradaDeHistorial entrada, boolean nueva) {}
