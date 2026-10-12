/**
 * Puertos de entrada del servicio: lo que el servicio ofrece. Es su API publicada dentro del
 * proceso; los demás servicios del mismo proceso solo pueden usar este paquete (ADR-012).
 *
 * <p>Obligatorio en los servicios centrales (5, 6, 10 y 12); en los de soporte y genéricos el
 * controlador puede llamar directamente a la aplicación, salvo que otro servicio del proceso
 * necesite llamarlos. NO DEBE exponer detalles de infraestructura.
 */
@NamedInterface("api")
package ec.mdd.reporting.puertos.entrada;

import org.springframework.modulith.NamedInterface;
