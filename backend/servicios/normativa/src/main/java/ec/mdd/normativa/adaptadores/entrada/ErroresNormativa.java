package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Normativa en el catálogo (ADR-016): las reglas de las versiones de normativa y de
 * su asociación a periodos (F04-H02). Sus textos son los de {@code contratos/comun/errores.md}; una
 * prueba lo comprueba.
 */
@Component
class ErroresNormativa implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        VersionEnUso.class,
                        "NORMATIVA_VERSION_EN_USO",
                        409,
                        "Versión en uso",
                        "La versión rige al menos un periodo y ya no se puede cambiar. Crea una"
                                + " versión nueva a partir de ella."),
                EntradaCatalogo.de(
                        CodigoRepetido.class,
                        "NORMATIVA_CODIGO_REPETIDO",
                        409,
                        "Código repetido",
                        "Ya existe una versión de normativa con ese código. Elige otro."),
                EntradaCatalogo.de(
                        PeriodoConVersion.class,
                        "NORMATIVA_PERIODO_CON_VERSION",
                        409,
                        "Periodo con versión",
                        "El periodo ya tiene una versión de normativa."));
    }
}
