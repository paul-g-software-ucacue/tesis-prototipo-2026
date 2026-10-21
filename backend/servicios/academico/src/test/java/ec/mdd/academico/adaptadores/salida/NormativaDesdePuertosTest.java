package ec.mdd.academico.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * El adaptador traduce los puertos publicados de Normativa al lenguaje de Académico (research §4,
 * principio XIII), con dobles de esos puertos escritos a mano.
 */
class NormativaDesdePuertosTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    /** Las asignaciones en memoria, con las reglas de Normativa que el adaptador traduce. */
    private final Map<String, Integer> asignaciones = new HashMap<>();

    private final ConsultarNormativa consultar =
            new ConsultarNormativa() {
                @Override
                public List<VersionNormativa> versiones() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public VersionNormativa version(int numero) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public AsignacionDelPeriodo periodo(String periodo) {
                    Integer version = asignaciones.get(periodo);
                    if (version == null) {
                        throw new PeriodoSinVersion(periodo);
                    }
                    return new AsignacionDelPeriodo(periodo, version, AHORA);
                }
            };

    private final NormativaDesdePuertos normativa =
            new NormativaDesdePuertos(
                    consultar,
                    (periodo, version, quien) -> {
                        if (version == 9) {
                            throw new VersionNoEncontrada(version);
                        }
                        Integer actual = asignaciones.putIfAbsent(periodo, version);
                        if (actual != null && actual != version) {
                            throw new PeriodoConVersion(periodo, actual);
                        }
                        return new AsignacionDelPeriodo(periodo, version, AHORA);
                    });

    @Test
    void dadoUnPeriodoSinVersion_cuandoSePideSuVersion_entoncesVacia() {
        assertThat(normativa.versionDe("2026-2")).isEmpty();
    }

    @Test
    void dadoUnPeriodoAsociado_cuandoSePideSuVersion_entoncesLaDeNormativa() {
        normativa.asociar("2026-2", 2, "vicerrectorado.demo");

        assertThat(normativa.versionDe("2026-2")).hasValue(2);
    }

    @Test
    void dadoUnPeriodoConOtraVersion_cuandoSeAsocia_entoncesPeriodoConOtraVersion() {
        asignaciones.put("2026-2", 1);

        assertThatThrownBy(() -> normativa.asociar("2026-2", 2, "vicerrectorado.demo"))
                .isInstanceOf(PeriodoConOtraVersion.class)
                .hasMessageContaining("versión de normativa 1");
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeAsocia_entoncesVersionInexistente() {
        assertThatThrownBy(() -> normativa.asociar("2026-2", 9, "vicerrectorado.demo"))
                .isInstanceOf(VersionInexistente.class)
                .extracting("version")
                .isEqualTo(9);
    }
}
