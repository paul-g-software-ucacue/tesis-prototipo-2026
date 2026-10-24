package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.dominio.PeriodoSinVersion;
import ec.mdd.academico.dominio.SedeMatrizExistente;
import ec.mdd.academico.dominio.TransicionNoPermitida;
import ec.mdd.academico.dominio.UnidadConDependientes;
import ec.mdd.academico.puertos.entrada.AsignaturaRepetida;
import ec.mdd.academico.puertos.entrada.CarreraRepetida;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.MallaRepetida;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.SiglasRepetidas;
import ec.mdd.academico.puertos.entrada.UnidadRepetida;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Académico coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresAcademicoTest {

    @Test
    void dadoElCatalogoDeAcademico_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresAcademico());
    }

    @Test
    void
            dadoLasReglasDeLosPeriodosLaEstructuraYLasCarreras_cuandoSeBuscanSusEntradas_entoncesSonLos409() {
        assertThat(new ErroresAcademico().entradas())
                .extracting(EntradaCatalogo::excepcion, entrada -> entrada.definicion().codigo())
                .containsExactlyInAnyOrder(
                        tuple(CodigoRepetido.class, "ACADEMICO_CODIGO_REPETIDO"),
                        tuple(PeriodoCerrado.class, "ACADEMICO_PERIODO_CERRADO"),
                        tuple(TransicionNoPermitida.class, "ACADEMICO_TRANSICION_NO_PERMITIDA"),
                        tuple(PeriodoSinVersion.class, "ACADEMICO_PERIODO_SIN_VERSION"),
                        tuple(PeriodoConOtraVersion.class, "ACADEMICO_PERIODO_CON_VERSION"),
                        tuple(UnidadRepetida.class, "ACADEMICO_UNIDAD_REPETIDA"),
                        tuple(SedeMatrizExistente.class, "ACADEMICO_SEDE_MATRIZ_EXISTENTE"),
                        tuple(UnidadConDependientes.class, "ACADEMICO_UNIDAD_CON_DEPENDIENTES"),
                        tuple(CarreraRepetida.class, "ACADEMICO_CARRERA_REPETIDA"),
                        tuple(SiglasRepetidas.class, "ACADEMICO_SIGLAS_REPETIDAS"),
                        tuple(MallaRepetida.class, "ACADEMICO_MALLA_REPETIDA"),
                        tuple(AsignaturaRepetida.class, "ACADEMICO_ASIGNATURA_REPETIDA"));
        assertThat(new ErroresAcademico().entradas())
                .allMatch(entrada -> entrada.definicion().estado() == 409);
    }
}
