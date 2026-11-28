package ec.mdd.reporting.dominio;

/**
 * Por qué no se exporta una versión publicada (F19-H02, D-120), en el orden en que se comprueban:
 * primero las firmas, después las inconsistencias.
 */
public enum MotivoDeRechazo {
    /** La versión no tiene documento firmable. */
    SIN_DOCUMENTO_FIRMADO,
    /** El documento se generó de otra versión congelada. */
    DOCUMENTO_DE_OTRA_VERSION,
    /** Sus firmas se anularon o se rechazó en firma. */
    FIRMAS_ANULADAS_O_RECHAZADAS,
    /** No tiene pasos, o le falta la firma de alguno. */
    FIRMAS_INCOMPLETAS,
    /** Generación no registró sus inconsistencias al congelarla. */
    INCONSISTENCIAS_SIN_REGISTRO,
    /** Tiene inconsistencias duras o blandas sin justificar. */
    INCONSISTENCIAS_ABIERTAS
}
