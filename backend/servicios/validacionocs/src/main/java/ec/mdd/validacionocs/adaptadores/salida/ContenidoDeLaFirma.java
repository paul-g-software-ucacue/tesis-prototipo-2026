package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.generacion.puertos.entrada.GuardarContenidoCongelado;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Aprobación le ordena a Generación guardar el contenido congelado al entrar en firma (MD-04,
 * F20-H01, D-115), con el mismo aviso que prepara el documento firmable: los dos se calculan con la
 * oferta del momento de congelar.
 *
 * <p>Con {@link ApplicationModuleListener} corre después de confirmar el paso, en otro hilo y en
 * una transacción nueva: la aprobación no espera. Si termina con una excepción, el reenvío de
 * publicaciones (ADR-018) lo repite, y guardar es idempotente. Si nunca llega a guardarse, la
 * primera consulta de Reporting lo calcula como tardío.
 */
public class ContenidoDeLaFirma {

    private final GuardarContenidoCongelado generacion;

    /** Recibe la API publicada de Generación. */
    public ContenidoDeLaFirma(GuardarContenidoCongelado generacion) {
        this.generacion = generacion;
    }

    /**
     * @param aviso la versión que entró en firma
     */
    @ApplicationModuleListener
    public void alEntrarEnFirma(AvisoDeFirma.DistributivoEnFirma aviso) {
        generacion.guardar(aviso.periodo(), aviso.version());
    }
}
