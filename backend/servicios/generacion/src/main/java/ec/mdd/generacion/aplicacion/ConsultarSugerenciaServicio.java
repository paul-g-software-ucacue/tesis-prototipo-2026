package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.HorasDelPeriodo;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SugerenciaDelParalelo;
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
    private final RepositorioDistributivos distributivos;

    /** Recibe los datos de los otros servicios y los distributivos. */
    public ConsultarSugerenciaServicio(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            RepositorioDistributivos distributivos) {
        this.datos = datos;
        this.afinidad = afinidad;
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
                                                datos.ofertaDelPeriodo(periodo).orElse(List.of()),
                                                uno))
                        .orElse(Map.of());
        List<DocenteActivo> docentes = datos.docentesActivos();
        Sugerencia sugerencia = Sugerencia.armar(periodo, uno, pesos, docentes, horas);
        return new SugerenciaDelParalelo(
                sugerencia,
                vigente.isPresent(),
                vigente.flatMap(version -> version.propuestaDe(asignatura, paralelo))
                        .map(propuesta -> vista(propuesta, sugerencia)));
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
