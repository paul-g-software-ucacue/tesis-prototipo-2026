package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.EtapaConOperacionPropia;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteEnvio;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.dominio.ParcialYaEnviado;
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
                        tuple(PeriodoCerrado.class, "VALIDACIONOCS_PERIODO_CERRADO", 409));
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
}
