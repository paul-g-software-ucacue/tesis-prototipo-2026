package ec.mdd.validacionocs.dominio;

import java.util.OptionalInt;

/**
 * Una etapa del flujo en la configuración de un periodo.
 *
 * @param tipo cuál es
 * @param activa si el distributivo pasa por ella
 * @param nombre el nombre que ve el usuario
 * @param rol el rol responsable
 * @param plazoDias días corridos desde que el distributivo llega; PUBLICADO no tiene
 * @param admiteDevolucion si desde ella se puede devolver
 */
public record Etapa(
        TipoDeEtapa tipo,
        boolean activa,
        String nombre,
        String rol,
        OptionalInt plazoDias,
        boolean admiteDevolucion) {}
