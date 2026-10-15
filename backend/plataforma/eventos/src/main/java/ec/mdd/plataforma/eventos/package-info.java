/**
 * Eventos entre procesos (principio V, ADR-018): el sobre {@link
 * ec.mdd.plataforma.eventos.EventoEntreProcesos}, el oyente que lo entrega por HTTP con el token de
 * proceso y la tarea programada que reenvía lo que falló.
 *
 * <p>Es la API que la plataforma publica a los servicios para publicar eventos hacia otros
 * procesos: Spring Modulith ve {@code ec.mdd.plataforma} como un módulo en cada proceso, y esta
 * interfaz nombrada permite que los adaptadores de los servicios usen el sobre sin violar sus
 * límites, como {@code ec.mdd.plataforma.errores.nucleo}.
 */
@NamedInterface("eventos")
package ec.mdd.plataforma.eventos;

import org.springframework.modulith.NamedInterface;
