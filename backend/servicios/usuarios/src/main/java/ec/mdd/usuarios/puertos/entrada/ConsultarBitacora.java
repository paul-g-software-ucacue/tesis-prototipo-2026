package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: consultar la bitácora para responder a una auditoría (F17-H02, RF-44). Solo lee:
 * ninguna operación modifica una entrada (FR-006). El permiso {@code BITACORA:LECTURA} lo exige el
 * adaptador de entrada (ADR-021).
 */
public interface ConsultarBitacora {

    /**
     * Las entradas que cumplen todos los filtros presentes, de la más reciente a la más antigua. Un
     * parámetro {@code null} no filtra o toma su valor por defecto.
     *
     * @param filtro los criterios, tal como llegaron
     * @param pagina el número de página, desde 1; por defecto, 1
     * @param tamano las entradas por página, de 1 a 100; por defecto, 50
     * @param corte el corte de la primera respuesta; por defecto, ahora
     * @return la página, con el total y el corte
     * @throws ec.mdd.usuarios.dominio.FiltroDeBitacoraInvalido si un filtro no cumple su regla
     */
    PaginaDeEntradas consultar(Filtro filtro, Integer pagina, Integer tamano, Instant corte);

    /** Las acciones distintas que hay en la bitácora, ordenadas por servicio y acción. */
    List<AccionConocida> acciones();

    /**
     * Los criterios de una consulta, sin validar todavía.
     *
     * @param desde el primer día, inclusivo
     * @param hasta el último día, inclusivo
     * @param usuario el nombre de usuario exacto
     * @param servicio el servicio de origen
     * @param accion la acción, solo con su servicio
     * @param identificador un valor del objeto
     * @param periodo el código del periodo
     */
    record Filtro(
            LocalDate desde,
            LocalDate hasta,
            String usuario,
            String servicio,
            String accion,
            String identificador,
            String periodo) {}
}
