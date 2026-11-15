package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.firma.puertos.entrada.AnularFirmas;
import ec.mdd.firma.puertos.entrada.MotivoDeAnulacion;
import ec.mdd.validacionocs.puertos.salida.AvisoDeAnulacion.FirmasPorAnular;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Aprobación le ordena a Firma anular las firmas de la versión rechazada en firma (F12-H04, MD-05,
 * D-106), como le entrega la cadena al entrar en firma ({@link PreparacionDelDocumento}).
 *
 * <p>Con {@link ApplicationModuleListener} corre después de confirmar la versión nueva, en otro
 * hilo y en una transacción nueva. Si Firma falla, el reenvío de publicaciones (ADR-018) lo repite,
 * y anular es idempotente.
 */
public class AnulacionDeLasFirmas {

    private final AnularFirmas firma;

    /** Recibe la API publicada de Firma. */
    public AnulacionDeLasFirmas(AnularFirmas firma) {
        this.firma = firma;
    }

    /**
     * @param aviso el periodo, la versión rechazada y cuándo
     */
    @ApplicationModuleListener
    public void alPedirseLaAnulacion(FirmasPorAnular aviso) {
        firma.anular(
                aviso.periodo(),
                aviso.version(),
                MotivoDeAnulacion.RECHAZO_EN_FIRMA,
                aviso.momento());
    }
}
