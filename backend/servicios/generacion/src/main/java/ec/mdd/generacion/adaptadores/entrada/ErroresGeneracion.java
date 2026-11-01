package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.dominio.CargoNoVigente;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.ProyectoNoValido;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
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
                EntradaCatalogo.de(
                        DocenteNoEncontrado.class,
                        "GENERACION_DOCENTE_NO_ENCONTRADO",
                        404,
                        "Docente no encontrado",
                        "No hay un docente con esa identificación. Revísala y vuelve a"
                                + " intentarlo."));
    }
}
