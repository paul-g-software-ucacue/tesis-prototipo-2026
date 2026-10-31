package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
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
                        DocenteNoEncontrado.class,
                        "GENERACION_DOCENTE_NO_ENCONTRADO",
                        404,
                        "Docente no encontrado",
                        "No hay un docente con esa identificación. Revísala y vuelve a"
                                + " intentarlo."));
    }
}
