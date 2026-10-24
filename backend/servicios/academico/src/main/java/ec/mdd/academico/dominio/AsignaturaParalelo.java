package ec.mdd.academico.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un elemento de la oferta: una asignatura en un paralelo del periodo, con su jornada y su
 * modalidad (glosario «Asignatura-paralelo», RF-15, carga 5). Las reglas que miran al periodo, a la
 * asignatura, a su carrera y a la sede están en {@link OfertaDelPeriodo}.
 *
 * @param periodo el código del periodo; no cambia
 * @param asignatura el código de la asignatura; no cambia
 * @param paralelo como {@code A_NOCTURNA}; no cambia
 * @param jornada una de las jornadas de la carrera
 * @param modalidad una del catálogo de la carga 2
 * @param sede la indicada, o {@code null} para la de la carrera
 * @param estudiantes los previstos, de 0 a 9999
 * @param horasIndicadas de 0,5 a 20, o {@code null} para las de la asignatura (RN-18)
 * @param estado ofertada o retirada
 */
public record AsignaturaParalelo(
        String periodo,
        String asignatura,
        String paralelo,
        Jornada jornada,
        Modalidad modalidad,
        String sede,
        int estudiantes,
        BigDecimal horasIndicadas,
        EstadoDeAsignaturaParalelo estado) {

    /** El máximo de estudiantes previstos, por la longitud 4 del diccionario. */
    public static final int ESTUDIANTES_MAXIMO = 9999;

    // Los formatos y los rangos del diccionario de la carga 05.
    private static final Pattern PARALELO = Pattern.compile("[0-9A-Z_-]{1,20}");
    private static final BigDecimal MINIMO_DE_HORAS = new BigDecimal("0.5");
    private static final BigDecimal MAXIMO_DE_HORAS = new BigDecimal("20");

    /** Valida sus reglas y quita los espacios de los extremos del paralelo y de la sede. */
    public AsignaturaParalelo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(asignatura, "asignatura");
        paralelo = paralelo == null ? null : paralelo.strip();
        if (paralelo == null || !PARALELO.matcher(paralelo).matches()) {
            throw new DatoInvalido(
                    "paralelo",
                    "El paralelo solo admite mayúsculas, números, guion y guion bajo, hasta 20"
                            + " caracteres");
        }
        Objects.requireNonNull(jornada, "jornada");
        Objects.requireNonNull(modalidad, "modalidad");
        sede = sede == null || sede.isBlank() ? null : sede.strip();
        if (estudiantes < 0 || estudiantes > ESTUDIANTES_MAXIMO) {
            throw new DatoInvalido(
                    "estudiantes", "Los estudiantes previstos van de 0 a " + ESTUDIANTES_MAXIMO);
        }
        if (horasIndicadas != null
                && (horasIndicadas.compareTo(MINIMO_DE_HORAS) < 0
                        || horasIndicadas.compareTo(MAXIMO_DE_HORAS) > 0
                        || horasIndicadas.stripTrailingZeros().scale() > 2)) {
            throw new DatoInvalido(
                    "horasSemanales",
                    "Las horas semanales van de 0,5 a 20, con dos decimales como máximo");
        }
        Objects.requireNonNull(estado, "estado");
    }

    /** Una asignatura-paralelo recién ofertada. */
    public static AsignaturaParalelo nueva(
            String periodo,
            String asignatura,
            String paralelo,
            Jornada jornada,
            Modalidad modalidad,
            String sede,
            int estudiantes,
            BigDecimal horasIndicadas) {
        return new AsignaturaParalelo(
                periodo,
                asignatura,
                paralelo,
                jornada,
                modalidad,
                sede,
                estudiantes,
                horasIndicadas,
                EstadoDeAsignaturaParalelo.OFERTADA);
    }

    /**
     * La asignatura-paralelo con sus datos corregidos; el periodo, la asignatura, el paralelo y el
     * estado no cambian.
     *
     * @return la corregida y lo que cambió; sin cambios, la lista viene vacía
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public Correccion corregida(
            Jornada jornada,
            Modalidad modalidad,
            String sede,
            int estudiantes,
            BigDecimal horasIndicadas) {
        AsignaturaParalelo corregida =
                new AsignaturaParalelo(
                        periodo,
                        asignatura,
                        paralelo,
                        jornada,
                        modalidad,
                        sede,
                        estudiantes,
                        horasIndicadas,
                        estado);
        List<CambioDeValor> cambios = new ArrayList<>();
        Cambios.agregar(cambios, "jornada", this.jornada, corregida.jornada);
        Cambios.agregar(cambios, "modalidad", this.modalidad, corregida.modalidad);
        Cambios.agregar(cambios, "sede", this.sede, corregida.sede);
        Cambios.agregar(cambios, "estudiantes", this.estudiantes, corregida.estudiantes);
        boolean mismasHoras =
                this.horasIndicadas == null
                        ? corregida.horasIndicadas == null
                        : corregida.horasIndicadas != null
                                && this.horasIndicadas.compareTo(corregida.horasIndicadas) == 0;
        if (!mismasHoras) {
            Cambios.agregar(
                    cambios, "horasSemanales", this.horasIndicadas, corregida.horasIndicadas);
        }
        return new Correccion(corregida, cambios);
    }

    /** La asignatura-paralelo ofertada o retirada; los demás datos no cambian. */
    public AsignaturaParalelo conEstado(EstadoDeAsignaturaParalelo estado) {
        return new AsignaturaParalelo(
                periodo,
                asignatura,
                paralelo,
                jornada,
                modalidad,
                sede,
                estudiantes,
                horasIndicadas,
                estado);
    }

    /**
     * Una asignatura-paralelo corregida y lo que cambió, para el registro.
     *
     * @param asignaturaParalelo la corregida
     * @param cambios cada dato que cambió, con su valor anterior y el nuevo
     */
    public record Correccion(AsignaturaParalelo asignaturaParalelo, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
