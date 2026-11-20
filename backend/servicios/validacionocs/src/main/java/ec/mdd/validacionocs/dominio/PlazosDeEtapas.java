package ec.mdd.validacionocs.dominio;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Los días de cada etapa activa según una configuración del flujo (F12-H03, D-114): la que regía al
 * crear el distributivo, que reconstruye {@code EtapasAlCrearse}, o una recién cargada, para la
 * advertencia de RN-23. La copia del proceso ({@link EtapaDelProceso}) no guarda el plazo, y las
 * cargas son solo de inserción, así que la reconstrucción es exacta. PUBLICADO no tiene plazo.
 */
public final class PlazosDeEtapas {

    private final Map<TipoDeEtapa, Integer> dias;

    private PlazosDeEtapas(Map<TipoDeEtapa, Integer> dias) {
        this.dias = dias;
    }

    /**
     * @param etapas las etapas de una configuración ya validada
     * @return los días de las activas con plazo
     */
    public static PlazosDeEtapas de(Collection<Etapa> etapas) {
        Map<TipoDeEtapa, Integer> dias = new EnumMap<>(TipoDeEtapa.class);
        for (Etapa etapa : etapas) {
            Objects.requireNonNull(etapa, "etapa");
            if (etapa.activa() && etapa.tipo() != TipoDeEtapa.PUBLICADO) {
                etapa.plazoDias().ifPresent(plazo -> dias.put(etapa.tipo(), plazo));
            }
        }
        return new PlazosDeEtapas(dias);
    }

    /**
     * @param etapa una etapa
     * @return sus días; vacío en PUBLICADO y en una etapa inactiva
     */
    public OptionalInt de(TipoDeEtapa etapa) {
        Integer valor = dias.get(etapa);
        return valor == null ? OptionalInt.empty() : OptionalInt.of(valor);
    }

    /**
     * La suma de los plazos de las etapas activas sin PUBLICADO, es decir, hasta APROBADO: lo que
     * tardaría el flujo si cada etapa usa todo su plazo (RN-23, aclaración del 2026-11-20).
     *
     * @return los días
     */
    public int sumaHastaAprobado() {
        return dias.values().stream().mapToInt(Integer::intValue).sum();
    }
}
