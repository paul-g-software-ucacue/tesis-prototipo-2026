package ec.mdd.generacion.dominio;

/**
 * Lo que Generación sabe del tablero de preparación de un periodo (RF-56). Es un tipo propio de
 * este contexto, no el de Cargas (principio V).
 *
 * @param abierto si tiene su versión de normativa
 * @param cerrado si se cerró
 * @param listo si tiene todo lo que hace falta para su distributivo
 */
public record PeriodoListo(boolean abierto, boolean cerrado, boolean listo) {}
