package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pide a Académico, Normativa y Docentes, en el proceso {@code datos}, lo que hace falta para la
 * afinidad, por sus rutas internas (ADR-023). Sus respuestas se leen con tipos propios de este
 * adaptador: Generación no comparte tipos con sus dueños (principio V).
 */
public final class DatosDesdeProcesoDatos implements DatosParaAfinidad {

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

    private URI ruta(String... segmentos) {
        return UriComponentsBuilder.fromUri(base)
                .pathSegment("interno")
                .pathSegment(segmentos)
                .build()
                .encode()
                .toUri();
    }

    private record Pesos(int version, ValoresDePesos pesos) {

        PesosDelPeriodo delPeriodo() {
            return new PesosDelPeriodo(version, pesos.afinidad());
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
