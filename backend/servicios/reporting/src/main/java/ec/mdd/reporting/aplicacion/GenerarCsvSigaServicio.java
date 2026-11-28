package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CargasDeLaVersion;
import ec.mdd.reporting.dominio.CondicionesDeExportacion;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.CsvExportado;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.ExportacionRechazada;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.RechazoDeExportacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.ExportacionesSiga;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
import ec.mdd.reporting.puertos.salida.FirmasDeLasVersiones;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Genera el CSV para SIGA (RF-52, F19-H01, D-119).
 *
 * <p>Lee la publicación vigente una sola vez y, con su versión, el reporte general y el contenido
 * completo: si se publicara una reforma en medio, el CSV no mezcla dos versiones. El layout también
 * se lee una vez, al empezar: uno que se carga mientras tanto rige para el siguiente CSV. El
 * archivo se registra en la bitácora, en una transacción, antes de entregarse.
 *
 * <p>Desde F19-H02 (D-120), antes de generar valida que la versión esté firmada por toda su cadena
 * y sin inconsistencias abiertas registradas al congelarla ({@link CondicionesDeExportacion}).
 */
public final class GenerarCsvSigaServicio implements GenerarCsvSiga {

    private static final Logger LOG = LoggerFactory.getLogger(GenerarCsvSigaServicio.class);

    private final PublicacionesDelDistributivo publicaciones;
    private final ContenidoDeLaVersion contenido;
    private final FirmasDeLasVersiones firmas;
    private final LayoutVigente layouts;
    private final ExportadorSiga exportador;
    private final ExportacionesSiga exportaciones;
    private final RegistroEnBitacora registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    GenerarCsvSigaServicio(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            FirmasDeLasVersiones firmas,
            LayoutVigente layouts,
            ExportadorSiga exportador,
            ExportacionesSiga exportaciones,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        this.publicaciones = publicaciones;
        this.contenido = contenido;
        this.firmas = firmas;
        this.layouts = layouts;
        this.exportador = exportador;
        this.exportaciones = exportaciones;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    /**
     * @param publicaciones las publicaciones vigentes, de Aprobación
     * @param contenido el contenido congelado, de Generación
     * @param firmas la cadena de firmas de cada versión, de Firma (F19-H02)
     * @param guardados las versiones del layout de exportación
     * @param lector el lector del archivo de mapeo
     * @param exportador el puerto «Exportación a SIGA»
     * @param exportaciones las exportaciones registradas (F19-H02)
     * @param registro la bitácora
     * @param transaccion la transacción del registro
     * @param reloj cuándo
     * @return el servicio
     */
    public static GenerarCsvSigaServicio de(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            FirmasDeLasVersiones firmas,
            LayoutsGuardados guardados,
            LectorDeLayout lector,
            ExportadorSiga exportador,
            ExportacionesSiga exportaciones,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        return new GenerarCsvSigaServicio(
                publicaciones,
                contenido,
                firmas,
                new LayoutVigente(guardados, lector),
                exportador,
                exportaciones,
                registro,
                transaccion,
                reloj);
    }

    @Override
    public CsvExportado generar(String periodo, String quien) {
        Publicacion publicacion =
                publicaciones.vigentes().stream()
                        .filter(una -> una.periodo().equals(periodo))
                        .findFirst()
                        .orElseThrow(() -> new SinPublicacion(periodo));
        LayoutExportacion layout = layouts.vigente();
        CargasDeLaVersion cargas =
                contenido
                        .cargas(periodo, publicacion.version())
                        .orElseThrow(() -> sinContenido(publicacion));
        validar(publicacion, cargas, quien);
        DocentesDeLaVersion docentes =
                contenido
                        .docentes(periodo, publicacion.version())
                        .orElseThrow(() -> sinContenido(publicacion));
        ReporteGeneral general = ReporteGeneral.de(publicacion, docentes, quien, reloj.instant());
        FilasParaSiga filas = FilasParaSiga.de(publicacion, cargas, general);
        ArchivoSiga archivo = exportador.exportar(filas, layout);
        ExportacionSiga exportacion =
                ExportacionSiga.de(
                        publicacion,
                        layout.numero(),
                        archivo,
                        filas.filas().size(),
                        quien,
                        reloj.instant());
        // F19-H02 (FR-013): el registro y su entrada, juntos o ninguno, antes de entregar.
        transaccion.ejecutar(
                () -> {
                    exportaciones.guardar(exportacion);
                    registro.registrar(exportacion);
                    return null;
                });
        return new CsvExportado(archivo, exportacion);
    }

    /**
     * Las firmas y las inconsistencias de la versión (F19-H02, D-120), antes que las comprobaciones
     * de F19-H01. El rechazo se anota en la bitácora en su propia transacción; si no se puede
     * anotar, se rechaza igual: el aviso de una anomalía no debe ocultar la anomalía.
     */
    private void validar(Publicacion publicacion, CargasDeLaVersion cargas, String quien) {
        CondicionesDeExportacion condiciones =
                CondicionesDeExportacion.de(
                        publicacion,
                        firmas.firmas(publicacion.periodo(), publicacion.version()),
                        cargas.inconsistencias());
        try {
            condiciones.exigir();
        } catch (ExportacionRechazada rechazo) {
            try {
                transaccion.ejecutar(
                        () -> {
                            registro.registrar(
                                    RechazoDeExportacion.de(rechazo, quien, reloj.instant()));
                            return null;
                        });
            } catch (RuntimeException fallo) {
                LOG.error(
                        "No se pudo anotar en la bitácora el rechazo del CSV para SIGA del"
                                + " periodo {}, versión {}: {}",
                        publicacion.periodo(),
                        publicacion.version(),
                        condiciones.motivos(),
                        fallo);
            }
            throw rechazo;
        }
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
