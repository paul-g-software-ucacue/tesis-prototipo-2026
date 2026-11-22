package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Clock;

/**
 * Genera el CSV para SIGA (RF-52, F19-H01, D-119).
 *
 * <p>Lee la publicación vigente una sola vez y, con su versión, el reporte general y el contenido
 * completo: si se publicara una reforma en medio, el CSV no mezcla dos versiones. El layout también
 * se lee una vez, al empezar: uno que se carga mientras tanto rige para el siguiente CSV. El
 * archivo se registra en la bitácora, en una transacción, antes de entregarse.
 */
public final class GenerarCsvSigaServicio implements GenerarCsvSiga {

    private final PublicacionesDelDistributivo publicaciones;
    private final ContenidoDeLaVersion contenido;
    private final LayoutVigente layouts;
    private final ExportadorSiga exportador;
    private final RegistroEnBitacora registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    GenerarCsvSigaServicio(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            LayoutVigente layouts,
            ExportadorSiga exportador,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        this.publicaciones = publicaciones;
        this.contenido = contenido;
        this.layouts = layouts;
        this.exportador = exportador;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    /**
     * @param publicaciones las publicaciones vigentes, de Aprobación
     * @param contenido el contenido congelado, de Generación
     * @param guardados las versiones del layout de exportación
     * @param lector el lector del archivo de mapeo
     * @param exportador el puerto «Exportación a SIGA»
     * @param registro la bitácora
     * @param transaccion la transacción del registro
     * @param reloj cuándo
     * @return el servicio
     */
    public static GenerarCsvSigaServicio de(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            LayoutsGuardados guardados,
            LectorDeLayout lector,
            ExportadorSiga exportador,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        return new GenerarCsvSigaServicio(
                publicaciones,
                contenido,
                new LayoutVigente(guardados, lector),
                exportador,
                registro,
                transaccion,
                reloj);
    }

    @Override
    public ArchivoSiga generar(String periodo, String quien) {
        Publicacion publicacion =
                publicaciones.vigentes().stream()
                        .filter(una -> una.periodo().equals(periodo))
                        .findFirst()
                        .orElseThrow(() -> new SinPublicacion(periodo));
        LayoutExportacion layout = layouts.vigente();
        DocentesDeLaVersion docentes =
                contenido
                        .docentes(periodo, publicacion.version())
                        .orElseThrow(() -> sinContenido(publicacion));
        ReporteGeneral general = ReporteGeneral.de(publicacion, docentes, quien, reloj.instant());
        FilasParaSiga filas =
                FilasParaSiga.de(
                        publicacion,
                        contenido
                                .cargas(periodo, publicacion.version())
                                .orElseThrow(() -> sinContenido(publicacion)),
                        general);
        ArchivoSiga archivo = exportador.exportar(filas, layout);
        CsvGenerado generado =
                new CsvGenerado(
                        quien, reloj.instant(), periodo, publicacion.version(), layout.numero());
        transaccion.ejecutar(
                () -> {
                    registro.registrar(generado);
                    return null;
                });
        return archivo;
    }

    private static ContenidoInconsistente sinContenido(Publicacion publicacion) {
        return new ContenidoInconsistente(
                "Generación no tiene la versión publicada "
                        + publicacion.version()
                        + " del periodo "
                        + publicacion.periodo()
                        + ".");
    }
}
