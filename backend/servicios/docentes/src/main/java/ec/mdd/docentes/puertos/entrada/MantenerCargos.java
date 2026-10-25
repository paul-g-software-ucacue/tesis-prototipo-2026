package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DocenteNoHabilitado;

/**
 * Caso de uso: registrar y corregir los cargos (F03-H05, carga 8). Refleja {@code registrarCargo} y
 * {@code corregirCargo}. Ninguno se borra: se finaliza.
 */
public interface MantenerCargos {

    /**
     * Registra un cargo.
     *
     * @param quien el usuario que lo hace
     * @return el cargo con sus advertencias, como la de RN-20
     * @throws DatoInvalido si el docente no está registrado, o la unidad o la carrera no
     *     corresponden
     * @throws DocenteNoHabilitado si el docente no está activo a tiempo completo
     * @throws CargoRepetido si ya existe un cargo con esa clave
     * @throws CoordinadorVigente si la carrera ya tiene un coordinador vigente
     */
    CargoConsultado registrar(Cargo cargo, String quien);

    /**
     * Corrige o finaliza un cargo; su clave no cambia.
     *
     * @param cargo el cargo con su clave y sus datos nuevos
     * @throws CargoNoEncontrado si no hay un cargo con esa clave
     * @throws DatoInvalido si la unidad o la carrera no corresponden
     * @throws DocenteNoHabilitado si el docente no está activo a tiempo completo
     * @throws CoordinadorVigente si lo deja vigente en una carrera que ya tiene otro
     */
    CargoConsultado corregir(Cargo cargo, String quien);
}
