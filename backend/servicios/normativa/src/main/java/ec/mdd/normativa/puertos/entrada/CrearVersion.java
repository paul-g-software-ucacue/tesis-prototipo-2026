package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;

/**
 * Caso de uso: crear una versión nueva de la normativa copiando otra, con el siguiente número
 * (RF-19, F04-H02). Refleja la operación {@code crearVersion} del contrato de Normativa. Queda en
 * el registro como {@code VERSION_CREADA}.
 */
public interface CrearVersion {

    /**
     * @param copiarDe el número de la versión que se copia
     * @param datos la cabecera de la nueva
     * @param quien el usuario de la sesión
     * @return la versión creada, que no rige ningún periodo
     * @throws VersionNoEncontrada si no existe la versión que se copia
     * @throws CodigoRepetido si otra versión ya usa ese código
     */
    VersionNormativa crear(int copiarDe, DatosDeVersion datos, String quien);
}
