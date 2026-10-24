package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;

/**
 * Mantener la oferta de un periodo: ofertar, corregir, retirar y volver a ofertar
 * asignaturas-paralelo, con su registro (F03-H03, RF-15, principio VII). Solo mientras el periodo
 * está en Planificación.
 */
public interface MantenerOferta {

    /**
     * Oferta una asignatura en un paralelo del periodo.
     *
     * @param quien el usuario que lo hace
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    AsignaturaParalelo ofertar(AsignaturaParalelo nueva, String quien);

    /**
     * Corrige la jornada, la modalidad, la sede, los estudiantes o las horas.
     *
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws AsignaturaParaleloNoEncontrada si la asignatura no está en ese paralelo
     */
    AsignaturaParalelo corregir(
            String periodo,
            String asignatura,
            String paralelo,
            DatosDeAsignaturaParalelo datos,
            String quien);

    /**
     * Retira una asignatura-paralelo o la vuelve a ofertar.
     *
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws AsignaturaParaleloNoEncontrada si la asignatura no está en ese paralelo
     */
    AsignaturaParalelo cambiarEstado(
            String periodo,
            String asignatura,
            String paralelo,
            EstadoDeAsignaturaParalelo estado,
            String quien);
}
