package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Iniciar una reforma del distributivo publicado (F18-H02, RF-40, D-113): el rol que aprueba el
 * contenido, por defecto el Vicerrector académico. Crea una versión nueva, marcada como reforma, y
 * el distributivo vuelve a consolidación; la publicada sigue vigente hasta que se publique la
 * nueva.
 */
@FunctionalInterface
public interface IniciarReforma {

    /**
     * @param periodo el código del periodo
     * @param motivo por qué hay que cambiar el distributivo publicado
     * @param quien quien la inicia
     * @return el estado del flujo, ya en la etapa de llegada con la versión nueva
     * @throws ec.mdd.validacionocs.dominio.MotivoDeReformaInvalido si el motivo no tiene de 10 a
     *     500 caracteres sin los espacios de los extremos
     * @throws PeriodoCerrado si el periodo está cerrado
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws ec.mdd.validacionocs.dominio.EtapaNoAdmiteReforma si no está en PUBLICADO
     * @throws ec.mdd.validacionocs.dominio.RolDeOtraEtapa si no tiene el rol de EN_REVISION_VRA
     */
    FlujoDelDistributivo iniciar(String periodo, String motivo, Solicitante quien);
}
