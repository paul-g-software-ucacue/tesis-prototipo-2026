package ec.mdd.docentes.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * El cargo de gestión que ocupa un docente (glosario «Cargo», carga 8). Su clave es el docente, el
 * cargo y la fecha de designación, y no cambia. Las reglas que miran a Académico (que la unidad y
 * la carrera existan y correspondan al cargo) están en {@link Referencias}; las del docente, en
 * {@link #verificarDocente(Docente)}.
 *
 * @param docente la identificación del docente
 * @param tipo el cargo
 * @param fechaDesignacion el inicio de funciones
 * @param unidad la unidad que dirige; nula solo para el coordinador de carrera
 * @param carrera la carrera que coordina; solo para el coordinador de carrera
 * @param fechaFin el fin de funciones, si ya se conoce
 * @param documento la resolución o la acción de personal de la designación
 * @param estado vigente o finalizado
 */
public record Cargo(
        String docente,
        TipoDeCargo tipo,
        LocalDate fechaDesignacion,
        String unidad,
        String carrera,
        LocalDate fechaFin,
        String documento,
        EstadoDeCargo estado) {

    /** El largo máximo del documento de designación. */
    public static final int DOCUMENTO_MAXIMO = 60;

    /** Los años que dura, como máximo, una designación (POL 5.4). */
    public static final int ANIOS_MAXIMOS = 2;

    private static final Pattern DOCENTE = Pattern.compile("[0-9A-Z]{1,13}");
    private static final Pattern UNIDAD = Pattern.compile("[A-Z0-9_-]{1,20}");
    private static final Pattern CARRERA = Pattern.compile("[0-9A-Z-]{1,20}");

    /** Valida cada campo según la carga 8 y quita los espacios de los extremos del documento. */
    public Cargo {
        if (docente == null || !DOCENTE.matcher(docente).matches()) {
            throw new DatoInvalido("docente", "Elige un docente registrado");
        }
        if (tipo == null) {
            throw new DatoInvalido("tipo", "Elige el cargo");
        }
        if (fechaDesignacion == null) {
            throw new DatoInvalido("fechaDesignacion", "Indica la fecha de designación");
        }
        unidad = vacioANulo(unidad);
        carrera = vacioANulo(carrera);
        if (unidad != null && !UNIDAD.matcher(unidad).matches()) {
            throw new DatoInvalido("unidad", "Elige una unidad de la estructura orgánica");
        }
        if (tipo.coordinaCarrera()) {
            if (carrera == null || !CARRERA.matcher(carrera).matches()) {
                throw new DatoInvalido("carrera", "Elige la carrera que coordina");
            }
        } else {
            if (unidad == null) {
                throw new DatoInvalido("unidad", "Elige la unidad que dirige");
            }
            if (carrera != null) {
                throw new DatoInvalido("carrera", "Solo el coordinador de carrera tiene carrera");
            }
        }
        if (fechaFin != null && !fechaFin.isAfter(fechaDesignacion)) {
            throw new DatoInvalido(
                    "fechaFin", "El fin de funciones tiene que ser posterior a la designación");
        }
        if (fechaFin != null && fechaFin.isAfter(fechaDesignacion.plusYears(ANIOS_MAXIMOS))) {
            throw new DatoInvalido(
                    "fechaFin",
                    "Una designación dura hasta "
                            + ANIOS_MAXIMOS
                            + " años: el fin de funciones va hasta "
                            + fechaDesignacion.plusYears(ANIOS_MAXIMOS)
                            + " (POL 5.4)");
        }
        documento = documento == null ? "" : documento.strip();
        if (documento.isEmpty()) {
            throw new DatoInvalido("documento", "Indica la resolución o la acción de personal");
        }
        if (documento.length() > DOCUMENTO_MAXIMO) {
            throw new DatoInvalido(
                    "documento", "No puede tener más de " + DOCUMENTO_MAXIMO + " caracteres");
        }
        estado = estado == null ? EstadoDeCargo.VIGENTE : estado;
    }

    /**
     * Comprueba que el docente pueda recibir el cargo: activo y de tiempo completo (POL 5.4).
     *
     * @throws DocenteNoHabilitado si no lo es
     */
    public void verificarDocente(Docente candidato) {
        if (!habilitado(candidato)) {
            throw new DocenteNoHabilitado();
        }
    }

    /**
     * Lo que hay que revisar del cargo hoy. Un coordinador o un gestor sin título de cuarto nivel
     * (RN-20); y, si está vigente, la fecha de fin vencida y un docente que ya no está activo a
     * tiempo completo.
     */
    public List<AdvertenciaDeCargo> advertencias(Docente suDocente, LocalDate hoy) {
        List<AdvertenciaDeCargo> advertencias = new ArrayList<>();
        if (tipo.exigeCuartoNivel() && suDocente.datos().ficha().cuartoNivel().isEmpty()) {
            advertencias.add(AdvertenciaDeCargo.CUARTO_NIVEL);
        }
        if (estado == EstadoDeCargo.VIGENTE) {
            if (fechaFin != null && fechaFin.isBefore(hoy)) {
                advertencias.add(AdvertenciaDeCargo.FECHA_FIN_VENCIDA);
            }
            if (!habilitado(suDocente)) {
                advertencias.add(AdvertenciaDeCargo.DOCENTE_NO_HABILITADO);
            }
        }
        return List.copyOf(advertencias);
    }

    /** Si es un coordinador de carrera vigente: solo puede haber uno por carrera (RN-12). */
    public boolean esCoordinadorVigente() {
        return tipo.coordinaCarrera() && estado == EstadoDeCargo.VIGENTE;
    }

    /** Si tiene la misma clave: docente, cargo y fecha de designación. */
    public boolean mismaClave(Cargo otro) {
        return docente.equals(otro.docente)
                && tipo == otro.tipo
                && fechaDesignacion.equals(otro.fechaDesignacion);
    }

    /**
     * El cargo con sus datos corregidos; la clave no cambia.
     *
     * @param nuevo el cargo con la misma clave y los datos nuevos
     * @return el cargo corregido y lo que cambió; sin cambios, la lista viene vacía
     */
    public Correccion corregido(Cargo nuevo) {
        if (!mismaClave(nuevo)) {
            throw new IllegalArgumentException("La clave de un cargo no cambia");
        }
        List<CambioDeValor> cambios = new ArrayList<>();
        Cambios.agregar(cambios, "unidad", unidad, nuevo.unidad);
        Cambios.agregar(cambios, "carrera", carrera, nuevo.carrera);
        Cambios.agregar(cambios, "fechaFin", fechaFin, nuevo.fechaFin);
        Cambios.agregar(cambios, "documento", documento, nuevo.documento);
        Cambios.agregar(cambios, "estado", estado, nuevo.estado);
        return new Correccion(nuevo, cambios);
    }

    private static boolean habilitado(Docente candidato) {
        Objects.requireNonNull(candidato, "docente");
        return candidato.estado() == EstadoLaboral.ACTIVO
                && candidato.datos().dedicacion() == Dedicacion.TC;
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    /**
     * Un cargo corregido con lo que cambió.
     *
     * @param cargo el cargo corregido
     * @param cambios cada campo con su valor anterior y el nuevo
     */
    public record Correccion(Cargo cargo, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
