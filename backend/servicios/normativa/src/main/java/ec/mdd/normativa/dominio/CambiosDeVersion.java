package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una edición completa de una versión: la cabecera, las horas de todas sus filas y los valores de
 * todos sus parámetros. Es la única forma de cambiar una versión: la pantalla y la carga 7 por
 * archivo la usan igual (FR-010).
 *
 * @param datos la cabecera
 * @param filas las horas de cada fila
 * @param parametros los valores de cada parámetro, por regla
 */
public record CambiosDeVersion(
        DatosDeVersion datos,
        List<HorasDeFila> filas,
        Map<String, Map<String, BigDecimal>> parametros) {

    /** Exige todo y copia las listas. */
    public CambiosDeVersion {
        Objects.requireNonNull(datos, "datos");
        filas = List.copyOf(filas);
        Map<String, Map<String, BigDecimal>> copia = new LinkedHashMap<>();
        parametros.forEach((regla, valores) -> copia.put(regla, Map.copyOf(valores)));
        parametros = java.util.Collections.unmodifiableMap(copia);
    }
}
