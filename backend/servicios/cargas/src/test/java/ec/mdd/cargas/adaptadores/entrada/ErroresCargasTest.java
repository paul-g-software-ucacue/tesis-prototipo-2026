package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.CargaConErrores;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDesactualizada;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Cargas coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresCargasTest {

    @Test
    void dadoElCatalogoDeCargas_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresCargas());
    }

    @Test
    void dadosLosRechazosDeUnArchivo_cuandoSeBuscanSusEntradas_entoncesSonLos10Codigos() {
        assertThat(new ErroresCargas().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(FormatoNoAdmitido.class, "CARGAS_FORMATO_NO_ADMITIDO", 400),
                        tuple(CodificacionNoAdmitida.class, "CARGAS_CODIFICACION_NO_ADMITIDA", 400),
                        tuple(VariasHojas.class, "CARGAS_VARIAS_HOJAS", 400),
                        tuple(ArchivoIlegible.class, "CARGAS_ARCHIVO_ILEGIBLE", 400),
                        tuple(EncabezadosDistintos.class, "CARGAS_ENCABEZADOS_DISTINTOS", 400),
                        tuple(ArchivoSinRegistros.class, "CARGAS_ARCHIVO_SIN_REGISTROS", 400),
                        tuple(ArchivoDemasiadoGrande.class, "CARGAS_ARCHIVO_DEMASIADO_GRANDE", 413),
                        tuple(CargaConErrores.class, "CARGAS_CARGA_CON_ERRORES", 409),
                        tuple(
                                VistaPreviaDesactualizada.class,
                                "CARGAS_VISTA_PREVIA_DESACTUALIZADA",
                                409),
                        tuple(RechazoDelLote.class, "CARGAS_CARGA_RECHAZADA", 409));
    }

    @Test
    void dadoUnRechazo_cuandoSeTraduce_entoncesLaFilaYElMotivoSinLaTraza() {
        ErroresCargas errores = new ErroresCargas();

        assertThat(
                        errores.detalles(
                                new RechazoDelLote(
                                        5, "Esta clave ya está en la fila 2: no la repitas.")))
                .containsExactly(
                        new DetalleRespuesta(
                                "fila 5", "Esta clave ya está en la fila 2: no la repitas."));
        assertThat(
                        errores.detalles(
                                new RechazoDelLote(7, new IllegalStateException("traza interna"))))
                .containsExactly(new DetalleRespuesta("fila 7", ErroresCargas.RECHAZO_SIN_CODIGO));
    }
}
