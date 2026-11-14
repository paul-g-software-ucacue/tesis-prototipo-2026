/**
 * Puertos que Roles requiere de otro servicio del mismo proceso y que ese servicio implementa
 * (patrón de D-99). Es una interfaz publicada porque la implementación vive en otro módulo:
 * Usuarios ya depende de Roles, así que Roles no puede depender de Usuarios sin un ciclo (ADR-012).
 * Solo tipos simples.
 */
@NamedInterface("requeridos")
package ec.mdd.roles.puertos.requeridos;

import org.springframework.modulith.NamedInterface;
