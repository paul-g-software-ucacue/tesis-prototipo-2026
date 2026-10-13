/**
 * Núcleo del modelo de errores (ADR-016): el modelo público, el catálogo, los códigos comunes, el
 * saneo de las trazas y el {@code idIncidente}.
 *
 * <p>Es la API que la plataforma publica a los servicios para registrar sus errores: Spring
 * Modulith ve {@code ec.mdd.plataforma} como un módulo en cada proceso, y esta interfaz nombrada
 * permite que los adaptadores de los servicios la usen sin violar sus límites.
 */
@NamedInterface("errores")
package ec.mdd.plataforma.errores.nucleo;

import org.springframework.modulith.NamedInterface;
