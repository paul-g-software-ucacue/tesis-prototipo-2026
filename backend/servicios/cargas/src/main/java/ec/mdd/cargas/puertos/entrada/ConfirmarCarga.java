package ec.mdd.cargas.puertos.entrada;

import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDesactualizada;

/**
 * Caso de uso: confirmar una carga (RF-10, MD-13, F01-H04). Refleja la operación {@code
 * confirmarCarga} del contrato de Cargas. El archivo no se guarda; cada intento queda en el
 * historial.
 */
public interface ConfirmarCarga {

    /**
     * Valida otra vez el archivo y pide a su dueño que aplique la vista previa revisada.
     *
     * @param codigo el código de la carga
     * @param archivo el archivo, el mismo de la vista previa
     * @param huella la huella de la vista previa revisada
     * @param quien el usuario que confirma
     * @return aplicada, o repetida si el mismo archivo ya estaba aplicado
     * @throws CargaNoEncontrada si no es una de las 10
     * @throws CargaConErrores si sus filas tienen errores
     * @throws VistaPreviaDesactualizada si los datos cambiaron desde la vista previa
     * @throws RechazoDelLote si una regla del dueño falla en una fila
     */
    Confirmacion confirmar(String codigo, ArchivoSubido archivo, String huella, String quien);
}
