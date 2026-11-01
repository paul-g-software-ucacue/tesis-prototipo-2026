package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.puertos.entrada.ActividadPosible;
import ec.mdd.generacion.puertos.entrada.ConsultarActividadesPosibles;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import java.util.List;

/** El catálogo frente a un docente: la regla está en el dominio, no en la pantalla. */
public final class ConsultarActividadesPosiblesServicio implements ConsultarActividadesPosibles {

    private final DatosParaSugerencia datos;
    private final DatosParaActividades actividades;

    /** Recibe los puertos de salida. */
    public ConsultarActividadesPosiblesServicio(
            DatosParaSugerencia datos, DatosParaActividades actividades) {
        this.datos = datos;
        this.actividades = actividades;
    }

    @Override
    public List<ActividadPosible> posibles(String periodo, String docente) {
        if (datos.docentesActivos().stream()
                .noneMatch(activo -> activo.identificacion().equals(docente))) {
            throw new DocenteNoActivo();
        }
        List<ActividadDelCatalogo> catalogo =
                actividades.catalogo(periodo).orElseThrow(PeriodoSinAbrir::new);
        ProyectosYCargosDelDocente delDocente =
                actividades.proyectosYCargos(docente, periodo).orElseThrow(DocenteNoActivo::new);
        return catalogo.stream()
                .map(
                        una ->
                                new ActividadPosible(
                                        una,
                                        una.proyectosPosibles(delDocente),
                                        una.motivoParaNoAsignar(delDocente)))
                .toList();
    }
}
