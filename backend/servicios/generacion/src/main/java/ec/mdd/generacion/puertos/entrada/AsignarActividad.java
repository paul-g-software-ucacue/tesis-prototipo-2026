package ec.mdd.generacion.puertos.entrada;

/** Asignar y quitar actividades no lectivas (RF-26, RN-22, RN-25). */
public interface AsignarActividad {

    /**
     * Guarda la actividad, o reemplaza sus horas si el parcial ya la tiene.
     *
     * @param solicitud lo que pide quien asigna
     * @return la actividad, con el aviso de las horas (D-78)
     * @throws OtraCarrera si el parcial es de otra carrera
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     * @throws ActividadFueraDelCatalogo si la actividad no está en el catálogo
     * @throws DocenteNoActivo si el docente no está activo
     * @throws ec.mdd.generacion.dominio.ParcialNoEditable si el parcial ya no admite cambios
     * @throws ec.mdd.generacion.dominio.ProyectoNoValido si el proyecto no sirve
     * @throws ec.mdd.generacion.dominio.CargoNoVigente si el docente no tiene el cargo
     * @throws ec.mdd.generacion.dominio.HorasNoValidas si las horas no son válidas
     */
    ActividadGuardada asignar(SolicitudDeActividad solicitud);

    /**
     * Quita una actividad del parcial, también si el docente ya no está activo.
     *
     * @param quitada lo que pide quien la quita
     * @throws OtraCarrera si el parcial es de otra carrera
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws ActividadNoEncontrada si no la tiene
     * @throws ec.mdd.generacion.dominio.ParcialNoEditable si el parcial ya no admite cambios
     */
    void quitar(ActividadQuitada quitada);
}
