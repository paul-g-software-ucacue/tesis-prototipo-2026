package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.Inconsistencia;
import ec.mdd.generacion.dominio.MotorReglas;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.ReglaDeAsignacion;
import ec.mdd.generacion.dominio.ReglasDelPeriodo;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.HorasExcedidas;
import ec.mdd.generacion.puertos.entrada.TopeDeClase;
import java.util.List;

/**
 * Lo que dos casos de uso comparten para evaluar una asignación antes de guardarla (RF-28). Corre
 * dentro de la transacción, con la versión bloqueada (D-83), así que no llama a otros procesos.
 *
 * @param docente el docente de la asignación
 * @param delDocente sus proyectos asignables
 * @param oferta las asignaturas-paralelo del periodo
 * @param reglas las cifras de la normativa
 */
record ReglasDeLaAsignacion(
        DocenteActivo docente,
        ProyectosYCargosDelDocente delDocente,
        List<AsignaturaParalelo> oferta,
        ReglasDelPeriodo reglas) {

    /**
     * Exige que la asignación se pueda guardar.
     *
     * @param antes la versión sin la asignación
     * @param despues la versión con la asignación
     * @return la carga del docente con la asignación
     * @throws HorasExcedidas si queda sobre su límite y sube
     * @throws TopeDeClase si queda sobre el tope de clase y sube
     */
    CargaHoraria exigir(VersionDistributivo antes, VersionDistributivo despues) {
        CargaHoraria sinElla = carga(antes);
        CargaHoraria conElla = carga(despues);
        MotorReglas.evaluar(
                        sinElla,
                        conElla,
                        docente.horasContratadas(),
                        MotorReglas.conProyecto(
                                delDocente.proyectos().stream()
                                        .map(ProyectoDelDocente::tipo)
                                        .toList()),
                        reglas)
                .ifPresent(ReglasDeLaAsignacion::rechazar);
        return conElla;
    }

    /** Si la carga no pasa del límite del docente. */
    boolean sinExceso(CargaHoraria carga) {
        return carga.exceso(reglas.limite(docente.horasContratadas())).signum() == 0;
    }

    private CargaHoraria carga(VersionDistributivo version) {
        return CargaHoraria.calcular(
                docente.identificacion(), version.propuestas(), version.actividades(), oferta);
    }

    private static void rechazar(Inconsistencia inconsistencia) {
        if (inconsistencia.regla() == ReglaDeAsignacion.RN_02) {
            throw new HorasExcedidas(inconsistencia.horasQueQuedan());
        }
        throw new TopeDeClase(inconsistencia.horasQueQuedan());
    }
}
