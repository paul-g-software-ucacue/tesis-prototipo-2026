package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import java.util.List;

/**
 * La bitácora de auditoría (F17-H01, RF-44). Solo admite agregar: ninguna implementación modifica
 * ni borra una entrada (FR-009, FR-010). Desde F17-H02 también se consulta, solo con lecturas.
 */
public interface RepositorioBitacora {

    /**
     * Guarda la entrada en la transacción en curso.
     *
     * @param entrada una entrada válida, con un {@code idEvento} que no estaba registrado
     */
    void registrar(EntradaBitacora entrada);

    /**
     * Las entradas que cumplen el filtro y se recibieron hasta el corte de la página, de la más
     * reciente a la más antigua por {@code momento}, con {@code idEvento} como desempate (F17-H02,
     * research §2). Solo lee. Ningún valor del filtro se concatena en la consulta: todos van como
     * parámetros.
     *
     * @param filtro los criterios, ya validados
     * @param pagina la página, con su corte
     * @return las entradas de la página y el total que cumple el filtro hasta el corte
     */
    PaginaDeEntradas consultar(FiltroDeBitacora filtro, PaginaPedida pagina);

    /** Los pares distintos de servicio y acción, ordenados por servicio y acción. Solo lee. */
    List<AccionConocida> acciones();
}
