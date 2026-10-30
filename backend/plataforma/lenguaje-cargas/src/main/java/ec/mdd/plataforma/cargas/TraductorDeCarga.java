package ec.mdd.plataforma.cargas;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Si el dueño tiene al menos un registro vigente de la carga, entrado por archivo o por
     * pantalla (F01-H05, glosario «Estado de la carga»). Si la entidad tiene estado, solo cuentan
     * los activos. Las cargas que dependen de esta no se pueden subir mientras sea falso.
     */
    boolean tieneDatos();

    /**
     * Si el dueño tiene al menos un registro vigente de la carga en ese periodo (F02-H02, glosario
     * «Tablero de preparación»). Por defecto, la carga no depende del periodo y es {@link
     * #tieneDatos()}; lo reemplazan las cargas con datos del periodo, como la oferta.
     *
     * @param periodo el código de un periodo académico
     */
    default boolean tieneDatosEn(String periodo) {
        return tieneDatos();
    }

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

    /**
     * Aplica la vista previa, ya comprobada con las tablas bloqueadas: por defecto, fila por fila,
     * registrando los nuevos y corrigiendo los que cambian ({@link MotorDelLote#filaPorFila}). Una
     * carga que su dueño cambia entera, como la política en Normativa, lo reemplaza.
     *
     * @throws RechazoDelLote si una regla del dueño falla en una fila
     */
    default ResultadoDelLote aplicar(LoteDeCarga lote, VistaPreviaDelLote vista, String quien) {
        return MotorDelLote.filaPorFila(this, lote, vista, quien);
    }

    /**
     * El rechazo de una fila cuando el caso de uso del dueño lanza una excepción de validación que
     * no está en el catálogo, como su {@code DatoInvalido} con su campo. Vacío si la excepción va
     * tal cual, como causa del rechazo.
     */
    default Optional<RechazoDelLote> rechazo(FilaDelLote fila, RuntimeException error) {
        return Optional.empty();
    }
}
