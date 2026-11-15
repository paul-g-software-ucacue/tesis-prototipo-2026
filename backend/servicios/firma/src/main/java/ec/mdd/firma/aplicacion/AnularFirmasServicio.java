package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.AnulacionDeFirmas;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.puertos.entrada.AnularFirmas;
import ec.mdd.firma.puertos.entrada.FirmasAnuladas;
import ec.mdd.firma.puertos.entrada.MotivoDeAnulacion;
import ec.mdd.firma.puertos.salida.AvisoDeFirmasAnuladas;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anula las firmas del documento de una versión por orden de Aprobación (F12-H04, D-106).
 *
 * <p>En una transacción, con la generación bloqueada como el rechazo y la subida, registra la
 * anulación con las firmas que quedaron, la anota en la bitácora y publica {@link FirmasAnuladas}.
 * Las firmas, sus archivos y sus validaciones no cambian: quedan anuladas porque el documento lo
 * está. Una orden repetida, o una versión sin documento, no hace nada y no lanza: el registro de
 * publicaciones la reintentaría para siempre (ADR-018).
 */
public final class AnularFirmasServicio implements AnularFirmas {

    private static final Logger LOG = LoggerFactory.getLogger(AnularFirmasServicio.class);

    /** La anulación la ordena Aprobación como el sistema. */
    private static final String SISTEMA = "sistema";

    private final RepositorioDocumentos documentos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final AvisoDeFirmasAnuladas aviso;

    /**
     * @param documentos los documentos y el registro de la anulación
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de la anulación
     * @param aviso el aviso de firmas anuladas
     */
    public AnularFirmasServicio(
            RepositorioDocumentos documentos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            AvisoDeFirmasAnuladas aviso) {
        this.documentos = documentos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.aviso = aviso;
    }

    @Override
    public void anular(String periodo, int version, MotivoDeAnulacion motivo, Instant momento) {
        Optional<DocumentoFirmable> documento = documentos.documento(periodo, version);
        if (documento.isEmpty()) {
            LOG.warn(
                    "Anular las firmas de {} v{}: la versión no tiene documento", periodo, version);
            return;
        }
        transaccion.ejecutar(
                () -> {
                    Optional<AnulacionDeFirmas> anulacion =
                            documentos.registrarAnulacion(
                                    documento.get(), motivo.name(), SISTEMA, momento);
                    if (anulacion.isEmpty()) {
                        LOG.info("Las firmas de {} v{} ya estaban anuladas", periodo, version);
                        return null;
                    }
                    registro.registrar(cambio(periodo, version, anulacion.get()));
                    aviso.avisar(
                            new FirmasAnuladas(
                                    periodo,
                                    version,
                                    anulacion.get().firmasAnuladas(),
                                    motivo,
                                    momento));
                    return null;
                });
    }

    // El cambio para la bitácora: el documento, y cuántas firmas y por qué como valor nuevo.
    private static CambioDeFirma cambio(String periodo, int version, AnulacionDeFirmas anulacion) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", periodo);
        detalle.put("version", version);
        return new CambioDeFirma(
                anulacion.anuladaEn(),
                anulacion.anuladaPor(),
                CambioDeFirma.FIRMAS_ANULADAS,
                detalle,
                Map.of(
                        "firmasAnuladas", anulacion.firmasAnuladas(),
                        "motivo", anulacion.motivo()));
    }
}
