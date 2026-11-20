package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Publicar el distributivo aprobado (F18-H01, RF-40, MD-11, D-111): el rol de la etapa APROBADO,
 * por defecto la Secretaría general académica. La versión publicada es la congelada.
 */
@FunctionalInterface
public interface PublicarDistributivo {

    /**
     * @param periodo el código del periodo
     * @param quien quien publica
     * @return el estado del flujo, ya en PUBLICADO
     * @throws PeriodoCerrado si el periodo está cerrado
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws ec.mdd.validacionocs.dominio.EtapaNoAdmitePublicacion si no está en APROBADO
     * @throws ec.mdd.validacionocs.dominio.RolDeOtraEtapa si no tiene el rol de APROBADO
     */
    FlujoDelDistributivo publicar(String periodo, Solicitante quien);
}
