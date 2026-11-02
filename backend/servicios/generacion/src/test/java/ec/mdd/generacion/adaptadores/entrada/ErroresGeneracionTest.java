package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.generacion.dominio.CargoNoVigente;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.ProyectoNoValido;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.HorasExcedidas;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.TopeDeClase;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Generación coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresGeneracionTest {

    @Test
    void dadoElCatalogoDeGeneracion_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresGeneracion());
    }

    @Test
    void dadosLosFallosDeGeneracion_cuandoSeBuscanSusEntradas_entoncesTienenSuCodigo() {
        assertThat(new ErroresGeneracion().entradas())
                .extracting(
                        EntradaCatalogo::excepcion,
                        entrada -> entrada.definicion().codigo(),
                        entrada -> entrada.definicion().estado())
                .containsExactlyInAnyOrder(
                        tuple(PeriodoNoEncontrado.class, "GENERACION_PERIODO_NO_ENCONTRADO", 404),
                        tuple(PeriodoSinAbrir.class, "GENERACION_PERIODO_SIN_ABRIR", 409),
                        tuple(ParaleloNoEncontrado.class, "GENERACION_PARALELO_NO_ENCONTRADO", 404),
                        tuple(ParaleloRetirado.class, "GENERACION_PARALELO_RETIRADO", 409),
                        tuple(PeriodoNoListo.class, "GENERACION_PERIODO_NO_LISTO", 409),
                        tuple(DistributivoRepetido.class, "GENERACION_DISTRIBUTIVO_REPETIDO", 409),
                        tuple(
                                DistributivoNoEncontrado.class,
                                "GENERACION_DISTRIBUTIVO_NO_ENCONTRADO",
                                404),
                        tuple(SinDistributivo.class, "GENERACION_SIN_DISTRIBUTIVO", 409),
                        tuple(PeriodoCerrado.class, "GENERACION_PERIODO_CERRADO", 409),
                        tuple(DocenteNoActivo.class, "GENERACION_DOCENTE_NO_ACTIVO", 409),
                        tuple(ParcialNoEditable.class, "GENERACION_PARCIAL_NO_EDITABLE", 409),
                        tuple(
                                PropuestaNoEncontrada.class,
                                "GENERACION_PROPUESTA_NO_ENCONTRADA",
                                404),
                        tuple(
                                ActividadFueraDelCatalogo.class,
                                "GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO",
                                404),
                        tuple(ProyectoNoValido.class, "GENERACION_PROYECTO_NO_VALIDO", 409),
                        tuple(CargoNoVigente.class, "GENERACION_CARGO_NO_VIGENTE", 409),
                        tuple(
                                ActividadNoEncontrada.class,
                                "GENERACION_ACTIVIDAD_NO_ENCONTRADA",
                                404),
                        tuple(HorasExcedidas.class, "GENERACION_HORAS_EXCEDIDAS", 409),
                        tuple(TopeDeClase.class, "GENERACION_TOPE_DE_CLASE", 409),
                        tuple(DocenteNoEncontrado.class, "GENERACION_DOCENTE_NO_ENCONTRADO", 404));
    }
}
