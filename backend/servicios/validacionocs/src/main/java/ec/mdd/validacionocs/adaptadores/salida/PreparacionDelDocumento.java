package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.firma.puertos.entrada.PasoDeLaCadena;
import ec.mdd.firma.puertos.entrada.PrepararDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.SolicitudDeDocumento;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Aprobación le entrega la cadena a Firma al entrar en firma (MD-05, F13-H02, D-101).
 *
 * <p>Con {@link ApplicationModuleListener} corre después de confirmar el paso, en otro hilo y en
 * una transacción nueva: la aprobación no espera al PDF. Firma registra un fallo de generación y no
 * lo relanza; si este método terminara con una excepción, el reenvío de publicaciones (ADR-018) lo
 * repetiría, y preparar es idempotente.
 */
public class PreparacionDelDocumento {

    private final PrepararDocumentoFirmable firma;

    /** Recibe la API publicada de Firma. */
    public PreparacionDelDocumento(PrepararDocumentoFirmable firma) {
        this.firma = firma;
    }

    /**
     * @param aviso la versión, su hash, la cadena y el rol de la etapa
     */
    @ApplicationModuleListener
    public void alEntrarEnFirma(AvisoDeFirma.DistributivoEnFirma aviso) {
        firma.preparar(
                new SolicitudDeDocumento(
                        aviso.periodo(),
                        aviso.version(),
                        aviso.hash(),
                        aviso.cadena().stream()
                                .map(
                                        paso ->
                                                new PasoDeLaCadena(
                                                        paso.orden(), paso.etiqueta(), paso.rol()))
                                .toList(),
                        aviso.rolDeLaEtapa(),
                        aviso.momento()));
    }
}
