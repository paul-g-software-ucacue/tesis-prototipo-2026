package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ec.mdd.docentes.puertos.entrada.CorreoRepetido;
import ec.mdd.docentes.puertos.entrada.DocenteRepetido;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import org.junit.jupiter.api.Test;

/** Los errores de Docentes coinciden con contratos/comun/errores.md (ADR-016). */
class ErroresDocentesTest {

    @Test
    void dadoElCatalogoDeDocentes_cuandoSeComparaConElDocumentado_entoncesCoincide() {
        VerificadorDeCatalogo.verificar(new ErroresDocentes());
    }

    @Test
    void dadasLaIdentificacionYElCorreoRepetidos_cuandoSeBuscanSusEntradas_entoncesSonLos409() {
        assertThat(new ErroresDocentes().entradas())
                .extracting(EntradaCatalogo::excepcion, entrada -> entrada.definicion().codigo())
                .containsExactlyInAnyOrder(
                        tuple(DocenteRepetido.class, "DOCENTES_DOCENTE_REPETIDO"),
                        tuple(CorreoRepetido.class, "DOCENTES_CORREO_REPETIDO"));
        assertThat(new ErroresDocentes().entradas())
                .allMatch(entrada -> entrada.definicion().estado() == 409);
    }
}
