package ec.mdd.arquitectura.fixtures.capas.dominioconpdf.dominio;

import org.apache.pdfbox.fixtures.DocumentoPdfSimulado;

/**
 * Violación: el dominio usa una clase de una librería de PDF (sustituta, ver DocumentoPdfSimulado).
 */
public class ConPdf {
    private final DocumentoPdfSimulado documento = new DocumentoPdfSimulado();

    public DocumentoPdfSimulado documento() {
        return documento;
    }
}
