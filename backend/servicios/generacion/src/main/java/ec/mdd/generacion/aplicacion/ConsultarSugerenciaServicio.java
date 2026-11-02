package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.HorasDelPeriodo;
import ec.mdd.generacion.dominio.Inconsistencia;
import ec.mdd.generacion.dominio.MotorReglas;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.ReglaDeAsignacion;
import ec.mdd.generacion.dominio.ReglasDelPeriodo;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SugerenciaDelParalelo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Arma la sugerencia en cada consulta, así que siempre refleja las fichas y las propuestas de ese
 * momento; nada se guarda (FR-022). Pide los datos uno a uno y el primero que falla corta: nunca
 * hay una lista parcial.
 */
public final class ConsultarSugerenciaServicio implements ConsultarSugerencia {

    private final DatosParaSugerencia datos;
    private final DatosParaAfinidad afinidad;
    private final DatosParaActividades actividades;
    private final RepositorioDistributivos distributivos;

    /** Recibe los datos de los otros servicios y los distributivos. */
    public ConsultarSugerenciaServicio(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos) {
        this.datos = datos;
        this.afinidad = afinidad;
        this.actividades = actividades;
        this.distributivos = distributivos;
    }

    @Override
    public SugerenciaDelParalelo sugerencia(
            String periodo, String asignatura, String paralelo, Optional<String> carrera) {
        AsignaturaParalelo uno =
                datos.paralelo(periodo, asignatura, paralelo)
                        .orElseThrow(ParaleloNoEncontrado::new);
        if (carrera.isPresent() && !carrera.get().equals(uno.carrera())) {
            throw new OtraCarrera();
        }
        if (uno.retirada()) {
            throw new ParaleloRetirado();
        }
        PesosDelPeriodo pesos = afinidad.pesos(periodo).orElseThrow(PeriodoSinAbrir::new);
        Optional<VersionDistributivo> vigente = distributivos.vigenteDe(periodo);
        Map<String, HorasDelDocente> horas =
                vigente.map(
                                version ->
                                        HorasDelPeriodo.calcular(
                                                version.propuestas(),
                                                version.actividades(),
                                                datos.ofertaDelPeriodo(periodo).orElse(List.of()),
                                                uno))
                        .orElse(Map.of());
        List<DocenteActivo> docentes = datos.docentesActivos();
        ReglasDelPeriodo reglas = actividades.reglas(periodo).orElseThrow(PeriodoSinAbrir::new);
        Map<String, List<String>> conProyecto = actividades.docentesConProyecto(periodo);
        List<AsignaturaParalelo> oferta =
                vigente.isPresent() ? datos.ofertaDelPeriodo(periodo).orElse(List.of()) : List.of();
        Sugerencia sugerencia =
                Sugerencia.armar(
                        periodo,
                        uno,
                        pesos,
                        docentes,
                        horas,
                        reglas,
                        docente ->
                                motivo(
                                        docente,
                                        uno,
                                        vigente,
                                        oferta,
                                        conProyecto.getOrDefault(
                                                docente.identificacion(), List.of()),
                                        reglas));
        return new SugerenciaDelParalelo(
                sugerencia,
                vigente.isPresent(),
                vigente.flatMap(version -> version.propuestaDe(asignatura, paralelo))
                        .map(propuesta -> vista(propuesta, sugerencia)));
    }

    // El mismo motor que al guardar (SC-003). Si ya está propuesto aquí, repetir no le suma horas.
    private static Optional<ReglaDeAsignacion> motivo(
            DocenteActivo docente,
            AsignaturaParalelo paralelo,
            Optional<VersionDistributivo> vigente,
            List<AsignaturaParalelo> oferta,
            List<String> tiposDeProyecto,
            ReglasDelPeriodo reglas) {
        String id = docente.identificacion();
        CargaHoraria antes =
                CargaHoraria.calcular(
                        id,
                        vigente.map(VersionDistributivo::propuestas).orElse(List.of()),
                        vigente.map(VersionDistributivo::actividades).orElse(List.of()),
                        oferta);
        boolean yaPropuesto =
                vigente.flatMap(
                                version ->
                                        version.propuestaDe(
                                                paralelo.asignatura().codigo(),
                                                paralelo.paralelo()))
                        .filter(propuesta -> propuesta.docente().equals(id))
                        .isPresent();
        return MotorReglas.evaluar(
                        antes,
                        yaPropuesto ? antes : antes.conMasClase(paralelo.horasSemanales()),
                        docente.horasContratadas(),
                        MotorReglas.conProyecto(tiposDeProyecto),
                        reglas)
                .map(Inconsistencia::regla);
    }

    private static PropuestaVista vista(Propuesta propuesta, Sugerencia sugerencia) {
        boolean suficientes =
                Stream.concat(sugerencia.candidatos().stream(), sugerencia.bajoElMinimo().stream())
                        .filter(uno -> uno.identificacion().equals(propuesta.docente()))
                        .findFirst()
                        .map(uno -> uno.tieneHorasPara(sugerencia.paralelo().horasSemanales()))
                        .orElse(true);
        return new PropuestaVista(propuesta, false, suficientes);
    }
}
