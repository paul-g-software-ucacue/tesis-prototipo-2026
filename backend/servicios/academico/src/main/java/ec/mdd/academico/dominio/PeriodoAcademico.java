package ec.mdd.academico.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un periodo académico: el lapso para el que se arma un distributivo (glosario §7.3, RF-13).
 * Académico es de soporte (principio II): el periodo es un valor inmutable con sus reglas, sin
 * agregado ni eventos (research §1).
 *
 * <p>Su versión de normativa no es parte de él: la guarda Normativa (asignación normativa), y la
 * aplicación se la pide (research §2).
 *
 * @param codigo la clave que comparte con Normativa; no cambia nunca
 * @param nombre en blanco no vale; hasta {@value #NOMBRE_MAXIMO} caracteres
 * @param inicio el primer día
 * @param fin el último día, posterior al inicio
 * @param estado en qué etapa está
 * @throws DatoInvalido si un dato no cumple su regla (FR-006)
 */
public record PeriodoAcademico(
        String codigo, String nombre, LocalDate inicio, LocalDate fin, EstadoPeriodo estado) {

    /** El largo máximo del nombre. */
    public static final int NOMBRE_MAXIMO = 120;

    // El mismo formato que acepta Normativa para el código del periodo (F04-H02).
    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9._-]{1,20}");

    /** Valida los datos y quita los espacios de los extremos del nombre. */
    public PeriodoAcademico {
        Objects.requireNonNull(estado, "estado");
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código solo admite letras, números, punto, guion y guion bajo, hasta 20"
                            + " caracteres");
        }
        nombre = nombre == null ? "" : nombre.strip();
        if (nombre.isEmpty()) {
            throw new DatoInvalido("nombre", "El nombre no puede estar en blanco");
        }
        if (nombre.length() > NOMBRE_MAXIMO) {
            throw new DatoInvalido(
                    "nombre", "El nombre no puede tener más de " + NOMBRE_MAXIMO + " caracteres");
        }
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new DatoInvalido(
                    "fin",
                    "La fecha de fin ("
                            + fin
                            + ") debe ser posterior a la de inicio ("
                            + inicio
                            + ")");
        }
    }

    /** Un periodo recién registrado, en Planificación (FR-001). */
    public static PeriodoAcademico nuevo(
            String codigo, String nombre, LocalDate inicio, LocalDate fin) {
        return new PeriodoAcademico(codigo, nombre, inicio, fin, EstadoPeriodo.PLANIFICACION);
    }

    /**
     * El periodo con su nombre y sus fechas corregidos; el código y el estado no cambian.
     *
     * @return el periodo corregido y lo que cambió; sin cambios, la lista viene vacía
     * @throws PeriodoCerrado si está cerrado
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public Correccion corregido(String nombre, LocalDate inicio, LocalDate fin) {
        if (estado == EstadoPeriodo.CERRADO) {
            throw new PeriodoCerrado(codigo);
        }
        PeriodoAcademico corregido = new PeriodoAcademico(codigo, nombre, inicio, fin, estado);
        List<CambioDeValor> cambios = new ArrayList<>();
        agregar(cambios, "nombre", this.nombre, corregido.nombre);
        agregar(cambios, "inicio", this.inicio.toString(), corregido.inicio.toString());
        agregar(cambios, "fin", this.fin.toString(), corregido.fin.toString());
        return new Correccion(corregido, cambios);
    }

    /**
     * El periodo en el estado de destino, que tiene que ser el siguiente: no se salta ni se
     * retrocede (RF-13, FR-004). Salir de Planificación exige la versión de normativa.
     *
     * @param destino el estado pedido; si es el actual, devuelve el mismo periodo
     * @param tieneVersion si el periodo ya tiene versión de normativa (la guarda Normativa)
     * @throws PeriodoCerrado si está cerrado
     * @throws TransicionNoPermitida si el destino no es el siguiente
     * @throws PeriodoSinVersion si sale de Planificación sin versión
     */
    public PeriodoAcademico conEstado(EstadoPeriodo destino, boolean tieneVersion) {
        Objects.requireNonNull(destino, "destino");
        if (destino == estado) {
            return this;
        }
        if (estado == EstadoPeriodo.CERRADO) {
            throw new PeriodoCerrado(codigo);
        }
        if (estado.siguiente().filter(destino::equals).isEmpty()) {
            throw new TransicionNoPermitida(codigo, estado, destino);
        }
        if (estado == EstadoPeriodo.PLANIFICACION && !tieneVersion) {
            throw new PeriodoSinVersion(codigo);
        }
        return new PeriodoAcademico(codigo, nombre, inicio, fin, destino);
    }

    private static void agregar(
            List<CambioDeValor> cambios, String campo, String anterior, String nuevo) {
        if (!anterior.equals(nuevo)) {
            cambios.add(new CambioDeValor(campo, anterior, nuevo));
        }
    }

    /**
     * Un periodo corregido y lo que cambió, para el registro.
     *
     * @param periodo el periodo corregido
     * @param cambios cada dato que cambió, con su valor anterior y el nuevo
     */
    public record Correccion(PeriodoAcademico periodo, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
