package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pide a Académico, Normativa y Docentes, en el proceso {@code datos}, lo que hace falta para la
 * afinidad y la sugerencia, por sus rutas internas (ADR-023). Sus respuestas se leen con tipos
 * propios de este adaptador: Generación no comparte tipos con sus dueños (principio V).
 */
public final class DatosDesdeProcesoDatos implements DatosParaAfinidad, DatosParaSugerencia {

    private final ConsultasEntreProcesos consultas;
    private final URI base;

    /**
     * @param consultas el cliente entre procesos
     * @param base la URL del proceso {@code datos}
     */
    public DatosDesdeProcesoDatos(ConsultasEntreProcesos consultas, URI base) {
        this.consultas = consultas;
        this.base = base;
    }

    @Override
    public Optional<List<AsignaturaOfertada>> asignaturasOfertadas(String periodo) {
        return consultas
                .obtener(
                        ruta("academico", "periodos", periodo, "asignaturas-ofertadas"),
                        AsignaturaOfertada[].class)
                .map(List::of);
    }

    @Override
    public Optional<PesosDelPeriodo> pesos(String periodo) {
        return consultas
                .obtener(ruta("normativa", "periodos", periodo, "pesos-de-afinidad"), Pesos.class)
                .map(Pesos::delPeriodo);
    }

    @Override
    public Optional<FormacionDelDocente> formacion(String identificacion) {
        return consultas
                .obtener(ruta("docentes", identificacion, "formacion"), Formacion.class)
                .map(Formacion::delDocente);
    }

    @Override
    public Optional<AsignaturaParalelo> paralelo(
            String periodo, String asignatura, String paralelo) {
        return consultas
                .obtener(
                        ruta("academico", "periodos", periodo, "oferta", asignatura, paralelo),
                        Paralelo.class)
                .map(Paralelo::deLaOferta);
    }

    @Override
    public List<DocenteActivo> docentesActivos() {
        return consultas
                .obtener(ruta("docentes", "activos"), Activo[].class)
                .map(activos -> Arrays.stream(activos).map(Activo::docente).toList())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Docentes no publica sus docentes activos"));
    }

    private URI ruta(String... segmentos) {
        return UriComponentsBuilder.fromUri(base)
                .pathSegment("interno")
                .pathSegment(segmentos)
                .build()
                .encode()
                .toUri();
    }

    private record Paralelo(
            String asignatura,
            String nombre,
            String carrera,
            String paralelo,
            String campoAmplio,
            String campoEspecifico,
            boolean troncoComun,
            BigDecimal horasSemanales,
            String estado) {

        AsignaturaParalelo deLaOferta() {
            return new AsignaturaParalelo(
                    new AsignaturaOfertada(asignatura, nombre, campoAmplio, campoEspecifico),
                    paralelo,
                    carrera,
                    horasSemanales,
                    troncoComun,
                    "RETIRADA".equals(estado));
        }
    }

    private record Activo(
            String identificacion,
            String nombre,
            Dedicacion dedicacion,
            int horasContratadas,
            Formacion formacion) {

        DocenteActivo docente() {
            return new DocenteActivo(
                    identificacion, nombre, dedicacion, horasContratadas, formacion.delDocente());
        }
    }

    private record Pesos(int version, ValoresDePesos pesos, BigDecimal minimoAfinidad) {

        PesosDelPeriodo delPeriodo() {
            return new PesosDelPeriodo(version, pesos.afinidad(), minimoAfinidad);
        }
    }

    private record ValoresDePesos(
            BigDecimal cuartoNivelCampoEspecifico,
            BigDecimal cuartoNivelCampoAmplio,
            BigDecimal tercerNivelCampoEspecifico,
            BigDecimal tercerNivelCampoAmplio,
            BigDecimal experienciaRelacionada,
            BigDecimal cursoRelacionado,
            BigDecimal topeCursos) {

        PesosAfinidad afinidad() {
            return new PesosAfinidad(
                    cuartoNivelCampoEspecifico,
                    cuartoNivelCampoAmplio,
                    tercerNivelCampoEspecifico,
                    tercerNivelCampoAmplio,
                    experienciaRelacionada,
                    cursoRelacionado,
                    topeCursos);
        }
    }

    private record Formacion(
            TituloDeTercerNivel tercerNivel,
            List<ElementoDeFormacion> cuartoNivel,
            List<ElementoDeFormacion> experiencia,
            List<ElementoDeFormacion> cursos) {

        FormacionDelDocente delDocente() {
            return new FormacionDelDocente(tercerNivel, cuartoNivel, experiencia, cursos);
        }
    }
}
