package ec.mdd.generacion.puertos.entrada;

/**
 * Por qué se creó una versión nueva del distributivo (glosario, {@code VersionDistributivo}). Está
 * en la interfaz publicada y no en el dominio: lo usa Aprobación al ordenar la versión nueva
 * (MD-04, principio V). Los otros motivos del glosario (ajuste, devolución) los agregan sus
 * historias.
 */
public enum MotivoDeVersion {
    /** El firmante en turno rechazó en firma la versión congelada (F12-H04, D-106). */
    RECHAZO_EN_FIRMA,

    /**
     * El Vicerrector inició una reforma del distributivo publicado (F18-H02, D-113). La anterior no
     * se anula: sigue CONGELADA, como la publicada vigente hasta que se publique la nueva.
     */
    REFORMA
}
