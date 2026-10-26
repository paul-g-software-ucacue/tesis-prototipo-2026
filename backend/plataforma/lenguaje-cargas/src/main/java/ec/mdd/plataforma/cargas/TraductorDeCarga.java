package ec.mdd.plataforma.cargas;

import java.util.List;
import java.util.Map;

/**
 * Lo que un servicio dueño escribe por cada carga que atiende (ADR-022, FR-012): la traducción de
 * las columnas de su archivo a sus datos, en un solo lugar. Registrar y corregir llaman a sus casos
 * de uso de siempre, así que se aplican sus reglas y su registro de cambios.
 *
 * @param <T> la entidad del dueño que corresponde a una fila
 */
public interface TraductorDeCarga<T> {

    /** El código de la carga, como {@code carreras}. */
    String carga();

    /** Bloquea las tablas de la carga, dentro de la transacción del lote, antes de recalcular. */
    void bloquear();

    /**
     * Las entidades que ya existen, por su clave, dentro del alcance del lote: todas, o solo las de
     * los periodos o las versiones que trae el archivo.
     */
    Map<String, T> existentes(LoteDeCarga lote);

    /** La clave de una fila: los valores de la clave de la carga, unidos con « · ». */
    String clave(FilaDelLote fila);

    /**
     * Los campos que se comparan de una entidad, por encabezado, con su texto canónico: el mismo
     * que da {@link #campos(FilaDelLote)} para el mismo valor. Las columnas que el dueño no guarda
     * no están.
     */
    Map<String, String> campos(T existente);

    /** Los mismos campos, con el texto canónico de los valores de la fila. */
    Map<String, String> campos(FilaDelLote fila);

    /** Registra la entidad de la fila con el caso de uso del dueño. */
    void registrar(FilaDelLote fila, String quien);

    /** Corrige la entidad con los valores de la fila, con el caso de uso del dueño. */
    void corregir(FilaDelLote fila, T actual, String quien);

    /**
     * El orden en que se aplican las filas: el del archivo, salvo que la carga lo necesite de otra
     * forma, como los padres antes que los hijos en la estructura orgánica.
     */
    default List<FilaDelLote> enOrden(List<FilaDelLote> filas) {
        return filas;
    }
}
