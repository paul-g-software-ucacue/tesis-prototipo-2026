package ec.mdd.roles.puertos.salida;

/** Guarda cada cambio de roles y de asignaciones, para la bitácora (FR-011). */
public interface RegistroDeCambios {

    /** Guarda el cambio; nunca con datos sensibles. */
    void registrar(CambioDeRoles cambio);
}
