package ec.mdd.firma.puertos.entrada;

import java.util.UUID;

/**
 * Rechaza en firma el documento con una observación (F14-H04, RF-36, D-105): el firmante en turno
 * no firma y dice qué hay que corregir.
 *
 * <p>Solo el firmante en turno, con la regla de la subida: a cualquier otro, y por un documento que
 * no existe, no está generado, ya se rechazó o tiene la cadena completa, {@link
 * ec.mdd.firma.dominio.SinTurnoDeFirma}, sin mirar la observación. El rechazo cierra el documento:
 * nadie queda en turno, y las firmas aceptadas se conservan.
 */
public interface RechazarEnFirma {

    /**
     * Registra el rechazo, su entrada en la bitácora y el aviso a Aprobación.
     *
     * @param documento el documento firmable
     * @param observacion qué hay que corregir, de 10 a 500 caracteres sin los espacios de los
     *     extremos
     * @param quien quien rechaza
     * @return el rechazo registrado
     * @throws ec.mdd.firma.dominio.SinTurnoDeFirma si no es el firmante en turno, también si una
     *     firma del paso se registró entretanto
     * @throws ec.mdd.firma.dominio.ObservacionInvalida si la observación no cabe
     */
    RechazoRegistrado rechazar(UUID documento, String observacion, Solicitante quien);
}
