package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una edición completa de una versión: la cabecera, las horas de todas sus filas, los valores de
 * todos sus parámetros y, si se piden, las severidades de sus reglas del motor (F04-H03, MD-08). Es
 * la única forma de cambiar una versión: la pantalla y la carga 7 por archivo la usan igual
 * (FR-010); la carga 7 no trae severidades, y una regla sin severidad pedida no cambia la suya.
 *
 * @param datos la cabecera
 * @param filas las horas de cada fila
 * @param parametros los valores de cada parámetro, por regla
 * @param severidades la severidad pedida, solo de las reglas que la traen
 */
public record CambiosDeVersion(
        DatosDeVersion datos,
        List<HorasDeFila> filas,
        Map<String, Map<String, BigDecimal>> parametros,
        Map<String, Severidad> severidades) {

    /** Exige todo y copia las listas. */
    public CambiosDeVersion {
        Objects.requireNonNull(datos, "datos");
        filas = List.copyOf(filas);
        Map<String, Map<String, BigDecimal>> copia = new LinkedHashMap<>();
        parametros.forEach((regla, valores) -> copia.put(regla, Map.copyOf(valores)));
        parametros = java.util.Collections.unmodifiableMap(copia);
        severidades = Map.copyOf(severidades);
    }

    /** Una edición sin severidades: las de la versión no cambian. */
    public CambiosDeVersion(
            DatosDeVersion datos,
            List<HorasDeFila> filas,
            Map<String, Map<String, BigDecimal>> parametros) {
        this(datos, filas, parametros, Map.of());
    }
}
