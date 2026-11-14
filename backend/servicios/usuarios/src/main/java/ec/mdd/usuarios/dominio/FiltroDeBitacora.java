package ec.mdd.usuarios.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Los criterios de una consulta de la bitácora (F17-H02, FR-002): fechas, usuario, tipo de cambio
 * (servicio y acción), identificador del objeto y periodo. Todos son opcionales y se cumplen a la
 * vez (FR-003). No se guarda.
 *
 * <p>Las fechas son días completos en la hora de Ecuador continental ({@link #ZONA}): {@link
 * #desde()} es el inicio del primer día y {@link #hasta()}, el inicio del día siguiente al último,
 * excluido (research §3). Las reglas de cada campo son las del contrato (FR-010, principio XIV); si
 * una no se cumple, {@link #de} lanza {@link FiltroDeBitacoraInvalido} con su campo.
 */
public final class FiltroDeBitacora {

    /** La hora de la institución, sin horario de verano (UTC−5). */
    public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    /** Los servicios que registran en la bitácora (D-90). */
    public static final Set<String> SERVICIOS =
            Set.of(
                    "generacion",
                    "academico",
                    "docentes",
                    "normativa",
                    "roles",
                    "validacionocs",
                    "firma");

    private static final int USUARIO_MAXIMO = 64;
    private static final Pattern ACCION = Pattern.compile("^[A-Z][A-Z_]{2,39}$");
    private static final Pattern IDENTIFICADOR = Pattern.compile("^[0-9A-Za-z._/-]{1,64}$");
    private static final Pattern PERIODO = Pattern.compile("^[A-Za-z0-9._-]{1,20}$");

    private final Instant desde;
    private final Instant hasta;
    private final String usuario;
    private final String servicio;
    private final String accion;
    private final String identificador;
    private final String periodo;

    private FiltroDeBitacora(
            Instant desde,
            Instant hasta,
            String usuario,
            String servicio,
            String accion,
            String identificador,
            String periodo) {
        this.desde = desde;
        this.hasta = hasta;
        this.usuario = usuario;
        this.servicio = servicio;
        this.accion = accion;
        this.identificador = identificador;
        this.periodo = periodo;
    }

    /**
     * Valida y construye el filtro. Un valor {@code null} es un filtro que no se aplica.
     *
     * @param desde el primer día, inclusivo
     * @param hasta el último día, inclusivo; no anterior a {@code desde}
     * @param usuario el nombre de usuario exacto, de 1 a 64 caracteres
     * @param servicio uno de {@link #SERVICIOS}
     * @param accion {@code ^[A-Z][A-Z_]{2,39}$}, solo con {@code servicio}
     * @param identificador {@code ^[0-9A-Za-z._/-]{1,64}$}
     * @param periodo {@code ^[A-Za-z0-9._-]{1,20}$}
     * @return el filtro válido
     * @throws FiltroDeBitacoraInvalido si un campo no cumple su regla
     */
    public static FiltroDeBitacora de(
            LocalDate desde,
            LocalDate hasta,
            String usuario,
            String servicio,
            String accion,
            String identificador,
            String periodo) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new FiltroDeBitacoraInvalido(
                    "hasta", "La fecha hasta no puede ser anterior a la fecha desde.");
        }
        if (usuario != null && (usuario.isEmpty() || usuario.length() > USUARIO_MAXIMO)) {
            throw new FiltroDeBitacoraInvalido(
                    "usuario", "El usuario debe tener de 1 a 64 caracteres.");
        }
        if (servicio != null && !SERVICIOS.contains(servicio)) {
            throw new FiltroDeBitacoraInvalido(
                    "servicio", "Elige uno de los servicios de la lista.");
        }
        if (accion != null) {
            if (servicio == null) {
                throw new FiltroDeBitacoraInvalido(
                        "accion", "Elige primero el servicio de la acción.");
            }
            exigir(ACCION, accion, "accion", "La acción va en mayúsculas y guiones bajos.");
        }
        if (identificador != null) {
            exigir(
                    IDENTIFICADOR,
                    identificador,
                    "identificador",
                    "Usa de 1 a 64 letras sin tilde, dígitos, punto, guion, guion bajo o barra.");
        }
        if (periodo != null) {
            exigir(
                    PERIODO,
                    periodo,
                    "periodo",
                    "Usa de 1 a 20 letras sin tilde, dígitos, punto, guion o guion bajo.");
        }
        return new FiltroDeBitacora(
                desde == null ? null : desde.atStartOfDay(ZONA).toInstant(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant(),
                usuario,
                servicio,
                accion,
                identificador,
                periodo);
    }

    /** El inicio del primer día, inclusivo; vacío si no hay límite inferior. */
    public Optional<Instant> desde() {
        return Optional.ofNullable(desde);
    }

    /** El inicio del día siguiente al último, excluido; vacío si no hay límite superior. */
    public Optional<Instant> hasta() {
        return Optional.ofNullable(hasta);
    }

    /** El nombre de usuario, exacto. */
    public Optional<String> usuario() {
        return Optional.ofNullable(usuario);
    }

    /** El servicio de origen. */
    public Optional<String> servicio() {
        return Optional.ofNullable(servicio);
    }

    /** La acción, siempre con su servicio. */
    public Optional<String> accion() {
        return Optional.ofNullable(accion);
    }

    /** Un valor que debe estar en el objeto de la entrada, a cualquier profundidad. */
    public Optional<String> identificador() {
        return Optional.ofNullable(identificador);
    }

    /** El código del periodo. */
    public Optional<String> periodo() {
        return Optional.ofNullable(periodo);
    }

    private static void exigir(Pattern regla, String valor, String campo, String motivo) {
        if (!regla.matcher(valor).matches()) {
            throw new FiltroDeBitacoraInvalido(campo, motivo);
        }
    }
}
