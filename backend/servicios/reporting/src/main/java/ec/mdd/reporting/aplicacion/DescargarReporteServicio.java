package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.EscritorCsv;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import ec.mdd.reporting.puertos.entrada.DescargarReporte;
import ec.mdd.reporting.puertos.salida.GeneradorDePdf;
import ec.mdd.reporting.puertos.salida.RegistroDeDescargas;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Clock;

/**
 * Descarga el reporte individual (F20-H01, RF-48, D-115). Primero genera el archivo completo, fuera
 * de la transacción; después registra la descarga en una transacción, que guarda el evento de la
 * bitácora en el registro de publicaciones; recién entonces lo entrega. Así, una descarga que llega
 * al usuario siempre queda registrada, y una que falla al generarse no deja entrada.
 */
public final class DescargarReporteServicio implements DescargarReporte {

    private final ConsultarReportes consultar;
    private final GeneradorDePdf pdf;
    private final RegistroDeDescargas registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * @param consultar el reporte de la versión publicada
     * @param pdf el generador del PDF/A
     * @param registro la bitácora de las descargas
     * @param transaccion la transacción del registro
     * @param reloj cuándo se descarga
     */
    public DescargarReporteServicio(
            ConsultarReportes consultar,
            GeneradorDePdf pdf,
            RegistroDeDescargas registro,
            Transaccion transaccion,
            Clock reloj) {
        this.consultar = consultar;
        this.pdf = pdf;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ArchivoDelReporte descargar(
            String periodo, String identificacion, Formato formato, String quien) {
        ReporteIndividual reporte = consultar.reporte(periodo, identificacion, quien);
        byte[] contenido =
                switch (formato) {
                    case PDF -> pdf.generar(reporte);
                    case CSV -> EscritorCsv.escribir(reporte);
                };
        DescargaDeReporte descarga =
                new DescargaDeReporte(
                        quien,
                        reloj.instant(),
                        periodo,
                        reporte.publicacion().version(),
                        identificacion,
                        formato);
        transaccion.ejecutar(
                () -> {
                    registro.registrar(descarga);
                    return null;
                });
        return new ArchivoDelReporte(
                contenido,
                formato,
                "reporte-individual-" + periodo + "-" + identificacion + "." + formato.extension());
    }
}
