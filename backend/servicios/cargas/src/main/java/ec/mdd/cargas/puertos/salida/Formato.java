package ec.mdd.cargas.puertos.salida;

/** Los formatos de archivo que admite una carga (RF-06). */
public enum Formato {
    /** Texto separado por comas o por punto y coma, en UTF-8 (RFC 4180). */
    CSV,
    /** Libro de Excel de Office Open XML, con una sola hoja. */
    XLSX
}
