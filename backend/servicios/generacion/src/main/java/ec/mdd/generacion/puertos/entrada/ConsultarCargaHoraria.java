package ec.mdd.generacion.puertos.entrada;

/** La carga horaria de un docente (RF-27, D-82). Es solo lectura. */
public interface ConsultarCargaHoraria {

    /**
     * Los totales de un docente activo en todas las carreras y unidades.
     *
     * @param periodo el código del periodo
     * @param docente su número de identificación
     * @return sus totales, sus horas contratadas y la meta
     * @throws DocenteNoActivo si no está activo
     * @throws SinDistributivo si el periodo no tiene distributivo, o no existe
     * @throws PeriodoSinAbrir si el periodo no tiene versión de normativa
     */
    CargaDelDocente carga(String periodo, String docente);
}
