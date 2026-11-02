package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.dominio.CargoNoVigente;
import ec.mdd.generacion.dominio.InformeDemasiadoGrande;
import ec.mdd.generacion.dominio.InformeNoEsPdf;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.ProyectoNoValido;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.AfinidadInsuficiente;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.HorasExcedidas;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoJustificable;
import ec.mdd.generacion.puertos.entrada.InformeIlegible;
import ec.mdd.generacion.puertos.entrada.InformeNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.SinConsolidar;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.TopeDeClase;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de negocio de Generación, con los códigos de {@code contratos/comun/errores.md}
 * (principio XIII, D-72). Los fallos entre procesos los traduce {@code ErroresEntreProcesos}.
 */
@Component
class ErroresGeneracion implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        PeriodoNoEncontrado.class,
                        "GENERACION_PERIODO_NO_ENCONTRADO",
                        404,
                        "Periodo no encontrado",
                        "El periodo no está en los ciclos académicos. Elige otro."),
                EntradaCatalogo.de(
                        PeriodoSinAbrir.class,
                        "GENERACION_PERIODO_SIN_ABRIR",
                        409,
                        "Periodo sin abrir",
                        "El periodo todavía no tiene su versión de normativa. Ábrelo en «Periodos»"
                                + " para calcular la afinidad."),
                EntradaCatalogo.de(
                        ParaleloNoEncontrado.class,
                        "GENERACION_PARALELO_NO_ENCONTRADO",
                        404,
                        "Asignatura-paralelo no encontrada",
                        "Esa asignatura-paralelo no está en la oferta del periodo. Revisa la"
                                + " oferta y elige otra."),
                EntradaCatalogo.de(
                        ParaleloRetirado.class,
                        "GENERACION_PARALELO_RETIRADO",
                        409,
                        "Asignatura-paralelo retirada",
                        "Esta asignatura-paralelo se retiró de la oferta y no recibe sugerencias."
                                + " Vuelve a ofertarla si hace falta."),
                EntradaCatalogo.de(
                        PeriodoNoListo.class,
                        "GENERACION_PERIODO_NO_LISTO",
                        409,
                        "Periodo sin preparar",
                        "El periodo todavía no está listo para su distributivo. Revisa lo que falta"
                                + " en «Preparación del periodo»."),
                EntradaCatalogo.de(
                        DistributivoRepetido.class,
                        "GENERACION_DISTRIBUTIVO_REPETIDO",
                        409,
                        "Distributivo ya creado",
                        "Este periodo ya tiene su distributivo."),
                EntradaCatalogo.de(
                        DistributivoNoEncontrado.class,
                        "GENERACION_DISTRIBUTIVO_NO_ENCONTRADO",
                        404,
                        "Distributivo no encontrado",
                        "El periodo todavía no tiene distributivo."),
                EntradaCatalogo.de(
                        SinDistributivo.class,
                        "GENERACION_SIN_DISTRIBUTIVO",
                        409,
                        "Sin distributivo",
                        "El periodo todavía no tiene distributivo. Pide al Vicerrector académico"
                                + " que lo cree."),
                EntradaCatalogo.de(
                        PeriodoCerrado.class,
                        "GENERACION_PERIODO_CERRADO",
                        409,
                        "Periodo cerrado",
                        "El periodo está cerrado y ya no admite propuestas."),
                EntradaCatalogo.de(
                        DocenteNoActivo.class,
                        "GENERACION_DOCENTE_NO_ACTIVO",
                        409,
                        "Docente no activo",
                        "Ese docente no está activo. Elige otro."),
                EntradaCatalogo.de(
                        ParcialNoEditable.class,
                        "GENERACION_PARCIAL_NO_EDITABLE",
                        409,
                        "Parcial cerrado",
                        "Este parcial ya no admite cambios."),
                EntradaCatalogo.de(
                        PropuestaNoEncontrada.class,
                        "GENERACION_PROPUESTA_NO_ENCONTRADA",
                        404,
                        "Sin propuesta",
                        "Esta asignatura-paralelo no tiene una propuesta."),
                EntradaCatalogo.de(
                        ActividadFueraDelCatalogo.class,
                        "GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO",
                        404,
                        "Actividad fuera del catálogo",
                        "Esa actividad no está en el catálogo de la normativa del periodo. Elige"
                                + " otra."),
                EntradaCatalogo.de(
                        ProyectoNoValido.class,
                        "GENERACION_PROYECTO_NO_VALIDO",
                        409,
                        "Proyecto no válido",
                        "Esa actividad necesita un proyecto aprobado del docente, del tipo que"
                                + " corresponde y asignable en el periodo. Revisa el proyecto"
                                + " elegido."),
                EntradaCatalogo.de(
                        CargoNoVigente.class,
                        "GENERACION_CARGO_NO_VIGENTE",
                        409,
                        "Cargo no vigente",
                        "El docente no tiene vigente el cargo que exige esa actividad. Elige otra"
                                + " o revisa sus cargos."),
                EntradaCatalogo.de(
                        ActividadNoEncontrada.class,
                        "GENERACION_ACTIVIDAD_NO_ENCONTRADA",
                        404,
                        "Sin actividad",
                        "Ese docente no tiene esa actividad en este parcial."),
                EntradaCatalogo.conDetalles(
                        HorasExcedidas.class,
                        new DefinicionDeCodigo(
                                "GENERACION_HORAS_EXCEDIDAS",
                                409,
                                "Horas laborales excedidas",
                                "Esta asignación pasa las horas del docente, sumando todas sus"
                                        + " carreras. Elige a otro docente o baja sus horas."),
                        error ->
                                List.of(
                                        new DetalleRespuesta(
                                                "horas",
                                                "Le quedan "
                                                        + formato(error.horasQueQuedan())
                                                        + " h."))),
                EntradaCatalogo.conDetalles(
                        TopeDeClase.class,
                        new DefinicionDeCodigo(
                                "GENERACION_TOPE_DE_CLASE",
                                409,
                                "Tope de horas de clase",
                                "Este docente tiene un proyecto de investigación o vinculación, y"
                                        + " su tope de clase no alcanza. Elige a otro docente."),
                        error ->
                                List.of(
                                        new DetalleRespuesta(
                                                "horas",
                                                "Le quedan "
                                                        + formato(error.horasQueQuedan())
                                                        + " h de clase."))),
                EntradaCatalogo.de(
                        SinConsolidar.class,
                        "GENERACION_SIN_CONSOLIDAR",
                        409,
                        "Sin consolidar",
                        "Primero consolida el periodo, y después adjunta el informe."),
                EntradaCatalogo.de(
                        InformeNoEsPdf.class,
                        "GENERACION_INFORME_NO_ES_PDF",
                        400,
                        "El archivo no es un PDF",
                        "Adjunta el informe en PDF. Si lo tienes en otro formato, expórtalo a PDF."),
                EntradaCatalogo.de(
                        InformeDemasiadoGrande.class,
                        "GENERACION_INFORME_DEMASIADO_GRANDE",
                        413,
                        "Informe demasiado grande",
                        "El informe pasa de 10 MB. Reduce su tamaño y vuelve a adjuntarlo."),
                EntradaCatalogo.de(
                        InformeNoEncontrado.class,
                        "GENERACION_INFORME_NO_ENCONTRADO",
                        404,
                        "Sin informe",
                        "Este periodo todavía no tiene informe sustentado."),
                EntradaCatalogo.de(
                        InformeIlegible.class,
                        "GENERACION_INFORME_ILEGIBLE",
                        500,
                        "Informe no disponible",
                        "No se pudo leer el informe guardado. Vuelve a adjuntarlo o avisa al administrador."),
                EntradaCatalogo.de(
                        InconsistenciaNoEncontrada.class,
                        "GENERACION_INCONSISTENCIA_NO_ENCONTRADA",
                        409,
                        "Inconsistencia resuelta",
                        "Esta inconsistencia ya no existe, quizá porque alguien la corrigió."
                                + " Actualiza el panel."),
                EntradaCatalogo.de(
                        InconsistenciaNoJustificable.class,
                        "GENERACION_INCONSISTENCIA_DURA",
                        409,
                        "No se puede justificar",
                        "La normativa del periodo no admite excepciones para esta regla. Corrígela"
                                + " en su pantalla."),
                EntradaCatalogo.de(
                        AfinidadInsuficiente.class,
                        "GENERACION_AFINIDAD_INSUFICIENTE",
                        409,
                        "Afinidad insuficiente",
                        "Este docente está bajo el mínimo de afinidad, y la normativa del periodo"
                                + " no admite excepciones. Elige a otro docente."),
                EntradaCatalogo.de(
                        DocenteNoEncontrado.class,
                        "GENERACION_DOCENTE_NO_ENCONTRADO",
                        404,
                        "Docente no encontrado",
                        "No hay un docente con esa identificación. Revísala y vuelve a"
                                + " intentarlo."));
    }

    private static String formato(BigDecimal horas) {
        return horas.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
