/**
 * El lenguaje publicado de las cargas (ADR-022, MD-13, F01-H04): el lote que Cargas envía al
 * servicio dueño de una carga, la vista previa con su huella, el traductor que cada dueño escribe
 * por carga, el motor que compara y aplica todo o nada, y el aplicador que ve Cargas.
 *
 * <p>Spring Modulith ve {@code ec.mdd.plataforma} como un módulo en cada proceso; esta interfaz
 * nombrada permite que Cargas y los dueños la usen sin violar sus límites, como {@code
 * ec.mdd.plataforma.eventos}.
 */
@NamedInterface("cargas")
package ec.mdd.plataforma.cargas;

import org.springframework.modulith.NamedInterface;
