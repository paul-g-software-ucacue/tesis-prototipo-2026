package ec.mdd.academico.dominio;

/**
 * Una de las 21 gestiones autorizadas de POL 6 (RN-10, glosario §7.3). El catálogo es un dato
 * precargado: nadie lo amplía, tampoco el Administrador (V-02, D-07, D-70).
 *
 * @param codigo de {@code G01} a {@code G21}
 * @param nombre el de POL 6, por ejemplo «Rector»
 */
public record GestionAutorizada(String codigo, String nombre) {}
