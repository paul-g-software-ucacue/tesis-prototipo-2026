package ec.mdd.generacion.puertos.entrada;

/**
 * Por qué se creó una versión nueva del distributivo (glosario, {@code VersionDistributivo}). Está
 * en la interfaz publicada y no en el dominio: lo usa Aprobación al ordenar la versión nueva
 * (MD-04, principio V). Los otros motivos del glosario (ajuste, devolución, reforma) los agregan sus
 * historias.
 */
public enum MotivoDeVersion {
    /** El firmante en turno rechazó en firma la versión congelada (F12-H04, D-106). */
    RECHAZO_EN_FIRMA
}
