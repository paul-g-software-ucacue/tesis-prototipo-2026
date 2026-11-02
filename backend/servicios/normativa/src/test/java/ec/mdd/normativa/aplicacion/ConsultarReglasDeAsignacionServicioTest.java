package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ReglasDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Las cifras de RN-02 y RN-13 salen de la versión que rige el periodo (principio VI). */
class ConsultarReglasDeAsignacionServicioTest {

    private static final String PERIODO = "2025-1";
    private static final String CON_PROYECTO = "DOCENTE_INVESTIGACION_VINCULACION";

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final ConsultarReglasDeAsignacionServicio servicio =
            new ConsultarReglasDeAsignacionServicio(repositorio);

    @Test
    void dadaLaPolitica_cuandoSeConsulta_entoncesDevuelveLosDosTopes() {
        con(40, CON_PROYECTO, 16);

        ReglasDeLaVersion reglas = servicio.reglas(PERIODO);

        assertThat(reglas.version()).isEqualTo(3);
        assertThat(reglas.horasMaximasDocente()).isEqualByComparingTo("40");
        assertThat(reglas.horasClaseConProyecto()).isEqualByComparingTo("16");
    }

    @Test
    void dadaOtraVersion_cuandoSeConsulta_entoncesDevuelveSusCifras() {
        con(38, CON_PROYECTO, 14);

        ReglasDeLaVersion reglas = servicio.reglas(PERIODO);

        assertThat(reglas.horasMaximasDocente()).isEqualByComparingTo("38");
        assertThat(reglas.horasClaseConProyecto()).isEqualByComparingTo("14");
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(repositorio.porPeriodo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reglas(PERIODO)).isInstanceOf(PeriodoSinVersion.class);
    }

    @Test
    void dadaUnaVersionSinElTopeDeHoras_cuandoSeConsulta_entoncesFallaSinInventarlo() {
        VersionNormativa version = version(40, CON_PROYECTO, 16);
        when(version.regla("RN-02")).thenReturn(Optional.empty());
        when(repositorio.porPeriodo(PERIODO)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaVersionSinLaFila_cuandoSeConsulta_entoncesFallaSinInventarla() {
        con(40, "OTRO_PERFIL", 16);

        assertThatThrownBy(() -> servicio.reglas(PERIODO))
                .isInstanceOf(IllegalStateException.class);
    }

    private void con(int horas, String perfil, int clase) {
        VersionNormativa version = version(horas, perfil, clase);
        when(repositorio.porPeriodo(PERIODO)).thenReturn(Optional.of(version));
    }

    private static VersionNormativa version(int horas, String perfil, int clase) {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.numero()).thenReturn(3);
        when(version.regla("RN-02"))
                .thenReturn(
                        Optional.of(
                                new Regla(
                                        "RN-02",
                                        Severidad.DURA,
                                        Map.of(
                                                "horas_maximas_docente",
                                                BigDecimal.valueOf(horas)))));
        when(version.tabla())
                .thenReturn(
                        new TablaCargaHoraria(
                                java.util.List.of(
                                        new FilaCargaHoraria(
                                                null,
                                                perfil,
                                                40,
                                                clase,
                                                0,
                                                clase,
                                                null,
                                                null,
                                                24,
                                                Severidad.DURA,
                                                "POL 6",
                                                null))));
        return version;
    }
}
