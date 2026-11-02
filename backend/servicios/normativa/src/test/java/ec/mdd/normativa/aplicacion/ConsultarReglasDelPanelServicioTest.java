package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Componente;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ReglasDelPanelDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Todo lo del panel sale de la versión del periodo, y lo que falta no se inventa (principio VI).
 */
class ConsultarReglasDelPanelServicioTest {

    private static final String PERIODO = "2026-I";

    private final RepositorioVersiones versiones = mock(RepositorioVersiones.class);
    private final RepositorioActividades actividades = mock(RepositorioActividades.class);
    private final ConsultarReglasDelPanelServicio servicio =
            new ConsultarReglasDelPanelServicio(versiones, actividades);

    @Test
    void dadaUnaVersion_cuandoSeConsulta_entoncesTraeSusSeveridadesYSusCifras() {
        conVersion(version());

        ReglasDelPanelDeLaVersion reglas = servicio.reglas(PERIODO);

        assertThat(reglas.version()).isEqualTo(7);
        assertThat(reglas.severidades())
                .containsOnly(
                        Map.entry("RF-24", Severidad.DURA),
                        Map.entry("RN-02", Severidad.DURA),
                        Map.entry("RN-05", Severidad.BLANDA),
                        Map.entry("RN-10", Severidad.BLANDA),
                        Map.entry("RN-12", Severidad.DURA),
                        Map.entry("RN-17", Severidad.DURA),
                        Map.entry("RN-18", Severidad.DURA));
        assertThat(reglas.horasMaximasDocente()).isEqualByComparingTo("38");
        assertThat(reglas.metaClase()).isEqualByComparingTo("70");
        assertThat(reglas.metaOtrasDocencia()).isEqualByComparingTo("30");
        assertThat(reglas.margenPuntos()).isEqualByComparingTo("3");
        assertThat(reglas.gestoresMinimo()).isEqualTo(8);
        assertThat(reglas.gestoresMaximo()).isEqualTo(11);
        assertThat(reglas.actividadesObligatorias()).containsExactly("140-b", "140-g");
    }

    @Test
    void dadaUnaVersionConRn05Dura_cuandoSeConsulta_entoncesDevuelveEsaSeveridad() {
        VersionNormativa version = version();
        cifras(
                version,
                "RN-05",
                Severidad.DURA,
                Map.of(
                        "porcentaje_clase", new BigDecimal("70"),
                        "porcentaje_otras_docencia", new BigDecimal("30"),
                        "margen_puntos", new BigDecimal("3")));
        conVersion(version);

        assertThat(servicio.reglas(PERIODO).severidades()).containsEntry("RN-05", Severidad.DURA);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(versiones.porPeriodo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reglas(PERIODO)).isInstanceOf(PeriodoSinVersion.class);
    }

    @Test
    void dadaUnaVersionSinElMargen_cuandoSeConsulta_entoncesFallaSinInventarlo() {
        VersionNormativa version = version();
        cifras(
                version,
                "RN-05",
                Severidad.BLANDA,
                Map.of(
                        "porcentaje_clase", new BigDecimal("70"),
                        "porcentaje_otras_docencia", new BigDecimal("30")));
        conVersion(version);

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaVersionSinUnaSeveridad_cuandoSeConsulta_entoncesFalla() {
        VersionNormativa version = version();
        when(version.regla("RN-17")).thenReturn(Optional.empty());
        conVersion(version);

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaVersionSinElRango_cuandoSeConsulta_entoncesFalla() {
        VersionNormativa version = version();
        cifras(version, "RN-10", Severidad.BLANDA, Map.of("t2_minimo", new BigDecimal("8")));
        conVersion(version);

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaVersionSinActividadesObligatorias_cuandoSeConsulta_entoncesFalla() {
        conVersion(version());
        when(actividades.deLaVersion(7)).thenReturn(List.of(actividad("140-c", false)));

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    private void conVersion(VersionNormativa version) {
        when(versiones.porPeriodo(PERIODO)).thenReturn(Optional.of(version));
    }

    private VersionNormativa version() {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.numero()).thenReturn(7);
        cifras(version, "RF-24", Severidad.DURA, Map.of());
        cifras(
                version,
                "RN-02",
                Severidad.DURA,
                Map.of("horas_maximas_docente", new BigDecimal("38")));
        cifras(
                version,
                "RN-05",
                Severidad.BLANDA,
                Map.of(
                        "porcentaje_clase", new BigDecimal("70"),
                        "porcentaje_otras_docencia", new BigDecimal("30"),
                        "margen_puntos", new BigDecimal("3")));
        cifras(
                version,
                "RN-10",
                Severidad.BLANDA,
                Map.of(
                        "t1_minimo", new BigDecimal("5"),
                        "t2_minimo", new BigDecimal("8"),
                        "t2_maximo", new BigDecimal("11")));
        cifras(version, "RN-12", Severidad.DURA, Map.of());
        cifras(version, "RN-17", Severidad.DURA, Map.of());
        cifras(version, "RN-18", Severidad.DURA, Map.of());
        when(actividades.deLaVersion(7))
                .thenReturn(
                        List.of(
                                actividad("140-b", true),
                                actividad("140-c", false),
                                actividad("140-g", true)));
        return version;
    }

    private static void cifras(
            VersionNormativa version,
            String regla,
            Severidad severidad,
            Map<String, BigDecimal> parametros) {
        when(version.regla(regla)).thenReturn(Optional.of(new Regla(regla, severidad, parametros)));
    }

    private static ActividadDelCatalogo actividad(String codigo, boolean obligatoria) {
        return new ActividadDelCatalogo(
                codigo, "Actividad", Componente.OTRAS_DOCENCIA, List.of(), List.of(), obligatoria);
    }
}
