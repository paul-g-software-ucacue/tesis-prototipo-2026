package ec.mdd.usuarios.puertos.entrada;

/**
 * Una acción que aparece en la bitácora, con su servicio (esquema {@code AccionDeBitacora}), para
 * elegir el filtro por tipo de cambio (research §4).
 *
 * @param servicio el servicio de origen
 * @param accion el nombre de la acción en ese servicio
 */
public record AccionConocida(String servicio, String accion) {}
