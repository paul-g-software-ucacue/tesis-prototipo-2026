package ec.mdd.reporting.dominio;

/** Los formatos en que se descarga un reporte (RF-48): PDF no editable y CSV; sin XLSX (D-01). */
public enum Formato {
    /** El PDF/A-2b. */
    PDF("pdf"),
    /** El CSV con punto y coma. */
    CSV("csv");

    private final String extension;

    Formato(String extension) {
        this.extension = extension;
    }

    /** La extensión del archivo, sin el punto. */
    public String extension() {
        return extension;
    }
}
