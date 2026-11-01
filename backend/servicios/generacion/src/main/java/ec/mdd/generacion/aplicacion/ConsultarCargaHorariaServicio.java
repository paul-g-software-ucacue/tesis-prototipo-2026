package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.CargaDelDocente;
import ec.mdd.generacion.puertos.entrada.ConsultarCargaHoraria;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.List;

/**
 * La carga horaria de un docente activo, en todas las carreras (RF-27, D-01). No filtra por carrera
 * (D-82) y no guarda nada: se puede consultar también con el periodo cerrado.
 */
public final class ConsultarCargaHorariaServicio implements ConsultarCargaHoraria {

    private final DatosParaSugerencia datos;
    private final DatosParaActividades normativa;
    private final RepositorioDistributivos distributivos;

    /** Recibe los puertos de salida. */
    public ConsultarCargaHorariaServicio(
            DatosParaSugerencia datos,
            DatosParaActividades normativa,
            RepositorioDistributivos distributivos) {
        this.datos = datos;
        this.normativa = normativa;
        this.distributivos = distributivos;
    }

    @Override
    public CargaDelDocente carga(String periodo, String docente) {
        DocenteActivo activo =
                datos.docentesActivos().stream()
                        .filter(uno -> uno.identificacion().equals(docente))
                        .findFirst()
                        .orElseThrow(DocenteNoActivo::new);
        VersionDistributivo version =
                distributivos.vigenteDe(periodo).orElseThrow(SinDistributivo::new);
        return new CargaDelDocente(
                docente,
                activo.nombre(),
                activo.horasContratadas(),
                CargaHoraria.calcular(
                        docente,
                        version.propuestas(),
                        version.actividades(),
                        datos.ofertaDelPeriodo(periodo).orElse(List.of())),
                normativa.meta(periodo).orElseThrow(PeriodoSinAbrir::new));
    }
}
