package ec.mdd.firma.dominio;

/**
 * Las comprobaciones de la validación básica sobre un PDF subido (D-23), en el orden en que se
 * evalúan: las cuatro primeras de F14-H02 (D-103) y las tres últimas de F14-H03 (D-104). Cada una
 * depende de la anterior: si una no se aprueba, las siguientes no se evalúan.
 */
public enum Comprobacion {

    /** El archivo subido empieza con todos los bytes del vigente: es el mismo documento. */
    MISMO_DOCUMENTO,

    /** Tiene exactamente una firma más que el vigente. */
    UNA_FIRMA_NUEVA,

    /** La firma nueva es criptográficamente válida sobre los bytes que cubre (su ByteRange). */
    INTEGRIDAD,

    /** El certificado de la firma nueva llega a una raíz de la lista de confianza. */
    CONFIANZA,

    /** El certificado de la firma nueva está vigente al momento de subir. */
    VIGENCIA,

    /** La firma no es de la persona de un paso posterior al paso en turno. */
    ORDEN,

    /** El CN del certificado es el nombre que el PDF imprime en el paso en turno. */
    FIRMANTE
}
