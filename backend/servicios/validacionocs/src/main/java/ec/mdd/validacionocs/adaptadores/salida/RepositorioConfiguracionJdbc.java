package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.AdvertenciaDeRemision;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.CodigoDeAdvertencia;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cada carga es una fila nueva; el número se cuenta al leer, y nunca se actualiza (D-89). Desde
 * F12-H03 guarda la advertencia de RN-23 con la que se cargó, en {@code advertencia} (jsonb, nula
 * si no hubo), para mostrarla al consultar la carga (FR-014).
 */
public final class RepositorioConfiguracionJdbc implements RepositorioConfiguracion {

    private static final String INSERTAR =
            """
            insert into validacionocs.configuracion_flujo
                (id, periodo, contenido, cargada_por, cargada_en, advertencia)
            values (:id, :periodo, :contenido, :por, :en, cast(:advertencia as jsonb))
            """;

    private static final String VIGENTE =
            """
            select contenido, cargada_por, cargada_en, advertencia::text as advertencia,
                   (select count(*) from validacionocs.configuracion_flujo
                     where periodo = :periodo) as numero
              from validacionocs.configuracion_flujo
             where periodo = :periodo
             order by cargada_en desc, id desc
             limit 1
            """;

    private static final String VIGENTE_EN =
            """
            select contenido, cargada_por, cargada_en, advertencia::text as advertencia,
                   (select count(*) from validacionocs.configuracion_flujo
                     where periodo = :periodo and cargada_en <= :instante) as numero
              from validacionocs.configuracion_flujo
             where periodo = :periodo and cargada_en <= :instante
             order by cargada_en desc, id desc
             limit 1
            """;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioConfiguracionJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(
            String periodo,
            String contenido,
            String por,
            Instant en,
            Optional<AdvertenciaDeRemision> advertencia) {
        jdbc.sql(INSERTAR)
                .param("id", UUID.randomUUID())
                .param("periodo", periodo)
                .param("contenido", contenido)
                .param("por", por)
                .param("en", Timestamp.from(en))
                .param(
                        "advertencia",
                        advertencia
                                .map(AdvertenciaGuardada::de)
                                .map(JSON::writeValueAsString)
                                .orElse(null))
                .update();
    }

    @Override
    public Optional<CargaGuardada> vigente(String periodo) {
        return jdbc.sql(VIGENTE)
                .param("periodo", periodo)
                .query(
                        (fila, indice) ->
                                new CargaGuardada(
                                        fila.getInt("numero"),
                                        fila.getString("contenido"),
                                        fila.getString("cargada_por"),
                                        fila.getTimestamp("cargada_en").toInstant(),
                                        advertencia(fila.getString("advertencia"))))
                .optional();
    }

    @Override
    public Optional<CargaGuardada> vigenteEn(String periodo, Instant instante) {
        return jdbc.sql(VIGENTE_EN)
                .param("periodo", periodo)
                .param("instante", java.sql.Timestamp.from(instante))
                .query(
                        (fila, indice) ->
                                new CargaGuardada(
                                        fila.getInt("numero"),
                                        fila.getString("contenido"),
                                        fila.getString("cargada_por"),
                                        fila.getTimestamp("cargada_en").toInstant(),
                                        advertencia(fila.getString("advertencia"))))
                .optional();
    }

    private static Optional<AdvertenciaDeRemision> advertencia(String texto) {
        return Optional.ofNullable(texto)
                .map(json -> JSON.readValue(json, AdvertenciaGuardada.class).advertencia());
    }

    /** La advertencia tal como va en el jsonb, con las fechas en ISO 8601. */
    record AdvertenciaGuardada(
            String codigo,
            String ultimoPlazo,
            String inicioDelPeriodo,
            Integer diasAntesDelInicio,
            String fechaDeRemision,
            Integer diasDeMargen) {

        static AdvertenciaGuardada de(AdvertenciaDeRemision una) {
            return new AdvertenciaGuardada(
                    una.codigo().name(),
                    una.ultimoPlazo().toString(),
                    una.inicioDelPeriodo().toString(),
                    una.diasAntesDelInicio().isPresent()
                            ? una.diasAntesDelInicio().getAsInt()
                            : null,
                    una.fechaDeRemision().map(LocalDate::toString).orElse(null),
                    una.diasDeMargen().isPresent() ? una.diasDeMargen().getAsInt() : null);
        }

        AdvertenciaDeRemision advertencia() {
            return new AdvertenciaDeRemision(
                    CodigoDeAdvertencia.valueOf(codigo),
                    LocalDate.parse(ultimoPlazo),
                    LocalDate.parse(inicioDelPeriodo),
                    diasAntesDelInicio == null
                            ? OptionalInt.empty()
                            : OptionalInt.of(diasAntesDelInicio),
                    Optional.ofNullable(fechaDeRemision).map(LocalDate::parse),
                    diasDeMargen == null ? OptionalInt.empty() : OptionalInt.of(diasDeMargen));
        }
    }
}
