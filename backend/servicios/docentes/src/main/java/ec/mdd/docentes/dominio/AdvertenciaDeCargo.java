package ec.mdd.docentes.dominio;

/**
 * Lo que hay que revisar de un cargo (glosario «Requisito del cargo»). Se calcula cada vez que se
 * consulta, con el docente y la fecha de hoy; no se guarda ni bloquea nada.
 */
public enum AdvertenciaDeCargo {
    /** Un coordinador o un gestor cuyo docente no tiene ningún título de cuarto nivel (RN-20). */
    CUARTO_NIVEL("Le falta un título de cuarto nivel registrado (RN-20)."),
    /** Un cargo vigente cuya fecha de fin ya pasó. */
    FECHA_FIN_VENCIDA("Su fecha de fin ya pasó: finalízalo."),
    /** Un cargo vigente cuyo docente dejó de estar activo a tiempo completo (POL 5.4). */
    DOCENTE_NO_HABILITADO("El docente ya no está activo a tiempo completo (POL 5.4).");

    private final String descripcion;

    AdvertenciaDeCargo(String descripcion) {
        this.descripcion = descripcion;
    }

    /** Qué hay que revisar, en español. */
    public String descripcion() {
        return descripcion;
    }
}
