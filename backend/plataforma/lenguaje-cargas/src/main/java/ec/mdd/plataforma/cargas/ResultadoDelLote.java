package ec.mdd.plataforma.cargas;

/**
 * Lo que el dueño aplicó de un lote (FR-004).
 *
 * @param nuevos cuántos registró
 * @param cambiados cuántos corrigió
 * @param bajasSinAplicar cuántos se daban de baja y quedaron como estaban (aclaración 2)
 */
public record ResultadoDelLote(int nuevos, int cambiados, int bajasSinAplicar) {}
