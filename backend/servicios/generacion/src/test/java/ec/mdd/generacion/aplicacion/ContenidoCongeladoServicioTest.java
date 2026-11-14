package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.AvanceDelParcial;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.CarreraConsolidada;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.ContenidoDeLaVersion;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.entrada.VersionNoCongelada;
import ec.mdd.generacion.puertos.salida.DatosParaElDocumento;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El contenido de la versión congelada para el documento firmable (F13-H02,
 * contracts/generacion.md).
 */
class ContenidoCongeladoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final UUID ID = UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f");
    private static final Instant MOMENTO = Instant.parse("2026-11-01T15:00:00Z");
    private static final String HASH = "e".repeat(64);

    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final ConsultarConsolidado consolidado = mock(ConsultarConsolidado.class);
    private final DatosParaSugerencia oferta = mock(DatosParaSugerencia.class);
    private final DatosParaElDocumento datos = mock(DatosParaElDocumento.class);
    private final ParcialesDelDistributivo parciales = mock(ParcialesDelDistributivo.class);
    private final ContenidoCongeladoServicio servicio =
            new ContenidoCongeladoServicio(distributivos, consolidado, oferta, datos, parciales);

    @BeforeEach
    void conLaOfertaYLosNombres() {
        when(oferta.ofertaDelPeriodo(PERIODO))
                .thenReturn(
                        Optional.of(
                                List.of(
                                        paralelo("POO-204", "Programación", "A"),
                                        paralelo("BD-301", "Bases de datos", "A"))));
        when(datos.nombresDeCarreras()).thenReturn(Map.of("TDS", "DESARROLLO DE SOFTWARE"));
        when(parciales.hashDelDistributivo(PERIODO)).thenReturn(HASH);
        when(consolidado.consolidado(PERIODO))
                .thenReturn(
                        new ConsolidadoDelPeriodo(
                                PERIODO,
                                true,
                                Optional.of(ID),
                                2,
                                Optional.empty(),
                                false,
                                List.of(
                                        new CarreraConsolidada(
                                                "TDS",
                                                EstadoDelParcial.ENVIADO,
                                                new AvanceDelParcial(2, 2),
                                                horas("8", "4"))),
                                horas("8", "4"),
                                0,
                                0,
                                Optional.empty()));
    }

    @Test
    void dadaLaVersionCongelada_cuandoSePideElContenido_entoncesVersionHashCarrerasYTotales() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(EstadoDeVersion.CONGELADA)));

        ContenidoDeLaVersion contenido = servicio.contenido(PERIODO);

        assertThat(contenido.version()).isEqualTo(2);
        assertThat(contenido.hash()).isEqualTo(HASH);
        assertThat(contenido.carreras())
                .containsExactly(
                        new ContenidoDeLaVersion.Carrera(
                                "TDS",
                                "DESARROLLO DE SOFTWARE",
                                "ENVIADO",
                                2,
                                2,
                                new ContenidoDeLaVersion.Horas(
                                        new BigDecimal("8"),
                                        new BigDecimal("4"),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO)));
        assertThat(contenido.totales().clase()).isEqualByComparingTo("8");
    }

    @Test
    void dadaLaVersionCongelada_cuandoSePideElContenido_entoncesCadaDocenteConSusAsignaciones() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(EstadoDeVersion.CONGELADA)));

        ContenidoDeLaVersion contenido = servicio.contenido(PERIODO);

        // Por nombre: «ARIAS» antes que «ZAMBRANO».
        assertThat(contenido.docentes())
                .extracting(ContenidoDeLaVersion.Docente::nombre)
                .containsExactly("ARIAS ANA", "ZAMBRANO LUIS");
        ContenidoDeLaVersion.Docente zambrano = contenido.docentes().get(1);
        assertThat(zambrano.identificacion()).isEqualTo("0102030405");
        assertThat(zambrano.asignaturas())
                .containsExactly(
                        new ContenidoDeLaVersion.Asignatura(
                                "BD-301", "Bases de datos", "A", "TDS", new BigDecimal("4")),
                        new ContenidoDeLaVersion.Asignatura(
                                "POO-204", "Programación", "A", "TDS", new BigDecimal("4")));
        assertThat(zambrano.actividades())
                .containsExactly(
                        new ContenidoDeLaVersion.Actividad(
                                "Tutorías", "OTRAS_DOCENCIA", new BigDecimal("4")));
        assertThat(zambrano.horas().clase()).isEqualByComparingTo("8");
        assertThat(zambrano.horas().otrasDocencia()).isEqualByComparingTo("4");
        assertThat(contenido.docentes().get(0).asignaturas()).isEmpty();
    }

    @Test
    void dadaUnaCarreraSinNombreEnAcademico_cuandoSePideElContenido_entoncesUsaElCodigo() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(EstadoDeVersion.CONGELADA)));
        when(datos.nombresDeCarreras()).thenReturn(Map.of());

        assertThat(servicio.contenido(PERIODO).carreras().getFirst().nombre()).isEqualTo("TDS");
    }

    @Test
    void dadaLaVersionEditable_cuandoSePideElContenido_entoncesVersionNoCongelada() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(EstadoDeVersion.EDITABLE)));

        assertThatThrownBy(() -> servicio.contenido(PERIODO))
                .isInstanceOf(VersionNoCongelada.class);
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSePideElContenido_entoncesDistributivoNoEncontrado() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.contenido(PERIODO))
                .isInstanceOf(DistributivoNoEncontrado.class);
    }

    private static VersionDistributivo version(EstadoDeVersion estado) {
        return new VersionDistributivo(
                ID,
                2,
                estado,
                List.of(new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO)),
                List.of(propuesta("POO-204"), propuesta("BD-301")),
                List.of(
                        new ActividadNoLectiva(
                                "TDS",
                                "0102030405",
                                "ZAMBRANO LUIS",
                                "140-b",
                                "Tutorías",
                                Componente.OTRAS_DOCENCIA,
                                null,
                                null,
                                new BigDecimal("4"),
                                MOMENTO,
                                "coordinacion.demo"),
                        new ActividadNoLectiva(
                                "TDS",
                                "0909090909",
                                "ARIAS ANA",
                                "140-g",
                                "Planificación",
                                Componente.OTRAS_DOCENCIA,
                                null,
                                null,
                                new BigDecimal("2"),
                                MOMENTO,
                                "coordinacion.demo")));
    }

    private static Propuesta propuesta(String asignatura) {
        return new Propuesta(
                asignatura,
                "A",
                "TDS",
                "0102030405",
                "ZAMBRANO LUIS",
                new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó la materia tres años"),
                MOMENTO,
                "coordinacion.demo");
    }

    private static AsignaturaParalelo paralelo(String codigo, String nombre, String paralelo) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada(codigo, nombre, "06", "061"),
                paralelo,
                "TDS",
                new BigDecimal("4"),
                false,
                false,
                false);
    }

    private static CargaHoraria horas(String clase, String otras) {
        return new CargaHoraria(
                new BigDecimal(clase),
                new BigDecimal(otras),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
