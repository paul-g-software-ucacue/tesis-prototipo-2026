package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;
import ec.mdd.firma.puertos.entrada.RechazoEnFirma;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import ec.mdd.validacionocs.puertos.entrada.RegistrarRechazoEnFirma;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Aprobación escucha que la cadena de firmas quedó completa (F14-H03, D-104) o que se rechazó en
 * firma (F14-H04, D-105), sin que Firma conozca el flujo (MD-05).
 *
 * <p>Firma publica el aviso en su API y no conoce el flujo; este oyente decide qué pasa con el
 * distributivo. Con {@link ApplicationModuleListener} corre después de confirmar la firma, en otro
 * hilo y en una transacción nueva; si falla, el registro de publicaciones lo reintenta (ADR-018), y
 * completar y rechazar son idempotentes.
 */
public class CierreDeLaFirma {

    private final CompletarFirmas completar;
    private final RegistrarRechazoEnFirma rechazar;

    /** Recibe los casos de uso de Aprobación. */
    public CierreDeLaFirma(CompletarFirmas completar, RegistrarRechazoEnFirma rechazar) {
        this.completar = completar;
        this.rechazar = rechazar;
    }

    /**
     * @param aviso el periodo y la versión firmados
     */
    @ApplicationModuleListener
    public void alCompletarseLaCadena(CadenaDeFirmasCompleta aviso) {
        completar.completar(aviso.periodo(), aviso.version(), aviso.momento());
    }

    /**
     * El firmante en turno rechazó en firma (F14-H04, D-105): el distributivo vuelve a la etapa
     * activa anterior con la versión nueva, y las firmas de la rechazada se anulan (F12-H04,
     * D-106). La observación ya la validó Firma con la misma regla (RF-36).
     *
     * @param aviso el periodo, la versión, el paso, su rol, quién, cuándo y la observación
     */
    @ApplicationModuleListener
    public void alRechazarseEnFirma(RechazoEnFirma aviso) {
        rechazar.registrar(
                aviso.periodo(),
                aviso.version(),
                new RechazoDelFirmante(
                        aviso.orden(),
                        aviso.rol(),
                        aviso.rechazadoPor(),
                        aviso.rechazadoEn(),
                        new Observacion(aviso.observacion())));
    }
}
