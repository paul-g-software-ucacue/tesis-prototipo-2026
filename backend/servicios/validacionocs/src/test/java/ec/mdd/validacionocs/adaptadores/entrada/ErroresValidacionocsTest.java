package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.CarreraRepetida;
import ec.mdd.validacionocs.dominio.ConsolidadoDesactualizado;
import ec.mdd.validacionocs.dominio.DevolucionDesactivada;
import ec.mdd.validacionocs.dominio.EtapaConOperacionPropia;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteAprobacion;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteDevolucion;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteEnvio;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteRevision;
import ec.mdd.validacionocs.dominio.InconsistenciasAbiertas;
import ec.mdd.validacionocs.dominio.ObservacionInvalida;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.dominio.ParcialNoEnviado;
import ec.mdd.validacionocs.dominio.ParcialYaEnviado;
import ec.mdd.validacionocs.dominio.ParcialesPendientes;
import ec.mdd.validacionocs.dominio.SinConsolidar;
import ec.mdd.validacionocs.dominio.SinInforme;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El catálogo de validacionocs coincide con el documentado (principio XIII). */
class ErroresValidacionocsTest {

    @Test
    void dadoElCatalogo_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresValidacionocs());
    }

    @Test
    void dadosLosFallos_cuandoSeBuscanSusEntradas_entoncesTienenSuCodigo() {
        assertThat(new ErroresValidacionocs().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(
                                ConfiguracionInvalida.class,
                                "VALIDACIONOCS_CONFIGURACION_INVALIDA",
                                400),
                        tuple(ArchivoIlegible.class, "VALIDACIONOCS_ARCHIVO_ILEGIBLE", 400),
                        tuple(
                                ArchivoDemasiadoGrande.class,
                                "VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE",
                                413),
                        tuple(
                                PeriodoSinPlanificacion.class,
                                "VALIDACIONOCS_PERIODO_SIN_PLANIFICACION",
                                409),
                        tuple(
                                PeriodoNoEncontrado.class,
                                "VALIDACIONOCS_PERIODO_NO_ENCONTRADO",
                                404),
                        tuple(SinDistributivo.class, "VALIDACIONOCS_SIN_DISTRIBUTIVO", 404),
                        tuple(EtapaNoAdmiteEnvio.class, "VALIDACIONOCS_ETAPA_NO_ADMITE_ENVIO", 409),
                        tuple(ParcialYaEnviado.class, "VALIDACIONOCS_PARCIAL_YA_ENVIADO", 409),
                        tuple(
                                EtapaConOperacionPropia.class,
                                "VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA",
                                409),
                        tuple(
                                ParcialNoEncontrado.class,
                                "VALIDACIONOCS_PARCIAL_NO_ENCONTRADO",
                                404),
                        tuple(PeriodoCerrado.class, "VALIDACIONOCS_PERIODO_CERRADO", 409),
                        tuple(
                                EtapaNoAdmiteRevision.class,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION",
                                409),
                        tuple(
                                EtapaNoAdmiteAprobacion.class,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION",
                                409),
                        tuple(ParcialesPendientes.class, "VALIDACIONOCS_PARCIALES_PENDIENTES", 409),
                        tuple(SinConsolidar.class, "VALIDACIONOCS_SIN_CONSOLIDAR", 409),
                        tuple(
                                ConsolidadoDesactualizado.class,
                                "VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO",
                                409),
                        tuple(SinInforme.class, "VALIDACIONOCS_SIN_INFORME", 409),
                        tuple(
                                InconsistenciasAbiertas.class,
                                "VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS",
                                409),
                        tuple(
                                EtapaNoAdmiteDevolucion.class,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION",
                                409),
                        tuple(
                                DevolucionDesactivada.class,
                                "VALIDACIONOCS_DEVOLUCION_DESACTIVADA",
                                409),
                        tuple(ParcialNoEnviado.class, "VALIDACIONOCS_PARCIAL_NO_ENVIADO", 409),
                        tuple(CarreraRepetida.class, "VALIDACIONOCS_CARRERA_REPETIDA", 400),
                        tuple(
                                ObservacionInvalida.class,
                                "VALIDACIONOCS_OBSERVACION_INVALIDA",
                                400));
    }

    @Test
    void dadoUnArchivoIlegibleConLinea_cuandoSeResponde_entoncesLaSenala() {
        EntradaCatalogo entrada =
                new ErroresValidacionocs()
                        .entradas().stream()
                                .filter(una -> una.excepcion() == ArchivoIlegible.class)
                                .findFirst()
                                .orElseThrow();

        assertThat(entrada.detalles().apply(new ArchivoIlegible(7)))
                .extracting(DetalleRespuesta::descripcion)
                .containsExactly("Revisa la línea 7.");
        assertThat(entrada.detalles().apply(new ArchivoIlegible())).isEmpty();
    }

    @Test
    void dadosVariosProblemas_cuandoSeResponde_entoncesCadaUnoEsUnDetalle() {
        EntradaCatalogo entrada =
                new ErroresValidacionocs()
                        .entradas().stream()
                                .filter(una -> una.excepcion() == ConfiguracionInvalida.class)
                                .findFirst()
                                .orElseThrow();

        assertThat(entrada.detalles().apply(new ConfiguracionInvalida(List.of("Uno.", "Dos."))))
                .extracting(DetalleRespuesta::campo, DetalleRespuesta::descripcion)
                .containsExactly(tuple("archivo", "Uno."), tuple("archivo", "Dos."));
    }

    @Test
    void dadasInconsistenciasAbiertas_cuandoSeResponde_entoncesDiceCuantasQuedan() {
        EntradaCatalogo entrada =
                new ErroresValidacionocs()
                        .entradas().stream()
                                .filter(una -> una.excepcion() == InconsistenciasAbiertas.class)
                                .findFirst()
                                .orElseThrow();

        assertThat(entrada.detalles().apply(new InconsistenciasAbiertas(2, 1)))
                .extracting(DetalleRespuesta::campo, DetalleRespuesta::descripcion)
                .containsExactly(
                        tuple("inconsistencias", "Quedan 3: 2 duras y 1 blanda sin justificar."));
        assertThat(entrada.detalles().apply(new InconsistenciasAbiertas(1, 0)))
                .extracting(DetalleRespuesta::descripcion)
                .containsExactly("Queda 1: 1 dura y 0 blandas sin justificar.");
        assertThat(entrada.detalles().apply(new InconsistenciasAbiertas(0, 2)))
                .extracting(DetalleRespuesta::descripcion)
                .containsExactly("Quedan 2: 0 duras y 2 blandas sin justificar.");
    }

    @Test
    void dadaUnaDevolucionRechazadaPorUnaCarrera_cuandoSeResponde_entoncesNombraLaCarrera() {
        ErroresValidacionocs catalogo = new ErroresValidacionocs();

        assertThat(detalles(catalogo, ObservacionInvalida.class, new ObservacionInvalida("DS")))
                .extracting(DetalleRespuesta::campo, DetalleRespuesta::descripcion)
                .containsExactly(
                        tuple("carrera", "Revisa la observación de DS: de 10 a 500 caracteres."));
        assertThat(detalles(catalogo, CarreraRepetida.class, new CarreraRepetida("DS")))
                .extracting(DetalleRespuesta::descripcion)
                .containsExactly("DS está más de una vez.");
        assertThat(detalles(catalogo, ParcialNoEnviado.class, new ParcialNoEnviado("DS")))
                .extracting(DetalleRespuesta::descripcion)
                .containsExactly("El parcial de DS no está enviado.");
    }

    private static List<DetalleRespuesta> detalles(
            ErroresValidacionocs catalogo, Class<?> tipo, RuntimeException error) {
        return catalogo.entradas().stream()
                .filter(una -> una.excepcion() == tipo)
                .findFirst()
                .orElseThrow()
                .detalles()
                .apply(error);
    }
}
