package ec.mdd.firma.puertos.entrada;

import ec.mdd.firma.dominio.ResultadoValidacion;
import java.util.UUID;

/** Esqueleto (F14-H02, T010). */
public interface SubirFirma {

    ResultadoValidacion subir(UUID documento, byte[] archivo, Solicitante quien);
}
