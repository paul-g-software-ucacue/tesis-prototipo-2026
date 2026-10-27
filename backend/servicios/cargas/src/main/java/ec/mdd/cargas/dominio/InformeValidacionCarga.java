package ec.mdd.cargas.dominio;

import java.util.List;

/**
 * El informe de validación de la carga: los errores y las advertencias de cada fila (RF-09,
 * glosario «Informe de validación de la carga»). No se guarda (FR-008).
 *
 * @param registros las filas de datos, sin el encabezado ni las filas vacías
 * @param problemas los problemas, en orden de fila y de columna
 */
public record InformeValidacionCarga(int registros, List<ProblemaDeFila> problemas) {

    /** Copia los problemas. */
    public InformeValidacionCarga {
        problemas = List.copyOf(problemas);
    }

    /** Cuántos errores tiene. */
    public int errores() {
        return (int) problemas.stream().filter(p -> p.tipo() == TipoDeProblema.ERROR).count();
    }

    /** Cuántas advertencias tiene. */
    public int advertencias() {
        return problemas.size() - errores();
    }

    /** Si no tiene errores; las advertencias no lo impiden. */
    public boolean valido() {
        return errores() == 0;
    }

    /** Cuántas filas distintas tienen algún error. */
    public int filasConErrores() {
        return (int)
                problemas.stream()
                        .filter(p -> p.tipo() == TipoDeProblema.ERROR)
                        .map(ProblemaDeFila::fila)
                        .distinct()
                        .count();
    }

    /** Los primeros problemas, para la respuesta de la pantalla; el CSV los trae todos. */
    public List<ProblemaDeFila> primeros(int cuantos) {
        return problemas.subList(0, Math.min(cuantos, problemas.size()));
    }
}
