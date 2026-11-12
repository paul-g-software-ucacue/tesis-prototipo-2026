package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.PasoFirma;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La cadena de firmas del consolidado de la copia de la configuración (F13-H02, MD-05). */
class EtapasAlCrearseTest {

    private static final String PERIODO = "2026-I";
    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");

    private final RepositorioConfiguracion configuraciones = mock(RepositorioConfiguracion.class);
    private final EtapasAlCrearse etapas =
            new EtapasAlCrearse(configuraciones, new LectorYaml(), new PlantillaEnRecursos());

    @Test
    void dadaLaConfiguracionPorDefecto_cuandoSePideLaCadena_entoncesSonLosTresFirmantesEnOrden() {
        when(configuraciones.vigenteEn(PERIODO, CREADO)).thenReturn(Optional.empty());

        assertThat(etapas.cadenaDelConsolidado(PERIODO, CREADO))
                .extracting(PasoFirma::orden, PasoFirma::etiqueta, PasoFirma::rol)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                        org.assertj.core.groups.Tuple.tuple(2, "Revisado", "VICERRECTOR"),
                        org.assertj.core.groups.Tuple.tuple(3, "Aprobado", "RECTOR"));
    }

    @Test
    void dadaUnaCargaAnteriorALaCreacion_cuandoSePideLaCadena_entoncesEsLaDeEsaCarga() {
        String otra =
                new PlantillaEnRecursos()
                        .texto()
                        .replaceFirst("etiqueta: Revisado", "etiqueta: Visto bueno");
        when(configuraciones.vigenteEn(PERIODO, CREADO))
                .thenReturn(Optional.of(new CargaGuardada(1, otra, "admin.demo", CREADO)));

        assertThat(etapas.cadenaDelConsolidado(PERIODO, CREADO))
                .extracting(PasoFirma::etiqueta)
                .containsExactly("Elaborado", "Visto bueno", "Aprobado");
    }
}
