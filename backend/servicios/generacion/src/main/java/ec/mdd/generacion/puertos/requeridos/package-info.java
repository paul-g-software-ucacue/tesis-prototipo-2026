/**
 * Puertos que Generación requiere de otro servicio del mismo proceso y que ese servicio implementa
 * (D-99). Es una interfaz publicada porque la implementación vive en otro módulo: así Generación no
 * depende de él y no hay ciclo entre los dos (ADR-012). Solo tipos simples.
 */
@NamedInterface("requeridos")
package ec.mdd.generacion.puertos.requeridos;

import org.springframework.modulith.NamedInterface;
