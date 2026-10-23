package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.dominio.PeriodoSinVersion;
import ec.mdd.academico.dominio.SedeMatrizExistente;
import ec.mdd.academico.dominio.TransicionNoPermitida;
import ec.mdd.academico.dominio.UnidadConDependientes;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.UnidadRepetida;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Académico en el catálogo (ADR-016): las reglas de los periodos académicos
 * (F02-H01) y de la estructura orgánica (F03-H01). Sus textos son los de {@code
 * contratos/comun/errores.md}; una prueba lo comprueba.
 */
@Component
class ErroresAcademico implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        CodigoRepetido.class,
                        "ACADEMICO_CODIGO_REPETIDO",
                        409,
                        "Código repetido",
                        "Ya existe un periodo con ese código. Elige otro."),
                EntradaCatalogo.de(
                        PeriodoCerrado.class,
                        "ACADEMICO_PERIODO_CERRADO",
                        409,
                        "Periodo cerrado",
                        "El periodo está cerrado y ya no admite cambios."),
                EntradaCatalogo.de(
                        TransicionNoPermitida.class,
                        "ACADEMICO_TRANSICION_NO_PERMITIDA",
                        409,
                        "Cambio de estado no permitido",
                        "El periodo solo pasa al estado siguiente, sin saltos ni retrocesos."),
                EntradaCatalogo.de(
                        PeriodoSinVersion.class,
                        "ACADEMICO_PERIODO_SIN_VERSION",
                        409,
                        "Periodo sin versión",
                        "El periodo necesita su versión de normativa para salir de Planificación."
                                + " Ábrelo antes."),
                EntradaCatalogo.de(
                        PeriodoConOtraVersion.class,
                        "ACADEMICO_PERIODO_CON_VERSION",
                        409,
                        "Periodo con versión",
                        "El periodo ya tiene otra versión de normativa, y no se puede cambiar."),
                EntradaCatalogo.de(
                        UnidadRepetida.class,
                        "ACADEMICO_UNIDAD_REPETIDA",
                        409,
                        "Unidad repetida",
                        "Ya existe una unidad con ese código. Elige otro."),
                EntradaCatalogo.de(
                        SedeMatrizExistente.class,
                        "ACADEMICO_SEDE_MATRIZ_EXISTENTE",
                        409,
                        "Sede matriz existente",
                        "La institución ya tiene su sede matriz. Registra las demás como sede,"
                                + " extensión o campus."),
                EntradaCatalogo.de(
                        UnidadConDependientes.class,
                        "ACADEMICO_UNIDAD_CON_DEPENDIENTES",
                        409,
                        "Unidad con dependientes",
                        "La unidad tiene unidades activas que dependen de ella. Inactívalas"
                                + " antes."));
    }
}
