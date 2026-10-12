/**
 * Reglas de arquitectura que comparten todos los servicios del MDD.
 *
 * <p>Las reglas hacen cumplir el sentido de las cinco capas y prohíben que el dominio dependa de
 * detalles técnicos (principios I y V de la constitución). Cada servicio las aplica desde su prueba
 * de arquitectura; los límites entre servicios de un mismo proceso los verifica Spring Modulith en
 * el proceso.
 */
package ec.mdd.arquitectura;
