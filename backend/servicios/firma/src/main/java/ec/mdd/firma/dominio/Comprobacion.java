package ec.mdd.firma.dominio;

/**
 * Las comprobaciones de la validación básica que hace F14-H02 sobre un PDF subido (D-23, D-103), en
 * el orden en que se evalúan. Cada una depende de la anterior: si una no se aprueba, las siguientes
 * no se evalúan. La vigencia al subir, la persona por el CN y el orden por la persona son de
 * F14-H03.
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

    /** Esqueleto (F14-H03, T006). */
    VIGENCIA,

    /** Esqueleto (F14-H03, T006). */
    ORDEN,

    /** Esqueleto (F14-H03, T006). */
    FIRMANTE
}
