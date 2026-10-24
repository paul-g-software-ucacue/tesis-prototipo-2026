package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.OfertaDelPeriodo;

/** Consultar la oferta de un periodo, con sus horas a cubrir (F03-H03, RF-15). */
public interface ConsultarOferta {

    /**
     * La oferta del periodo, con sus asignaturas-paralelo y los datos maestros para sus horas, su
     * sede y los datos de su asignatura.
     *
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    OfertaDelPeriodo oferta(String periodo);
}
