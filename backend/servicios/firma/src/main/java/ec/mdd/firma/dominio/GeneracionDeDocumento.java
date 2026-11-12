package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * La generación del documento firmable de una versión congelada (F13-H02, D-101): la solicitud que
 * entregó Aprobación (su cadena y el rol de la etapa, MD-05) y el último resultado.
 *
 * <p>Sus reglas:
 *
 * <ul>
 *   <li>EN_CURSO pasa a GENERADA o a FALLIDA; FALLIDA vuelve a EN_CURSO con un reintento;
 *   <li>GENERADA no cambia: una versión tiene un solo documento (FR-005);
 *   <li>solo reintenta el rol de la etapa que llegó con la solicitud (FR-012);
 *   <li>el motivo de un fallo tiene hasta 200 caracteres.
 * </ul>
 */
public final class GeneracionDeDocumento {

    /** El largo máximo del motivo de un fallo, el de la columna. */
    public static final int MOTIVO_MAXIMO = 200;

    private final String periodo;
    private final int version;
    private final String hashContenido;
    private final List<PasoDeFirma> cadena;
    private final String rolDeLaEtapa;
    private EstadoDeGeneracion estado;
    private String motivo;
    private String pedidaPor;
    private Instant actualizadaEn;

    private GeneracionDeDocumento(
            String periodo,
            int version,
            String hashContenido,
            List<PasoDeFirma> cadena,
            String rolDeLaEtapa,
            EstadoDeGeneracion estado,
            String motivo,
            String pedidaPor,
            Instant actualizadaEn) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.version = version;
        this.hashContenido = Objects.requireNonNull(hashContenido, "hashContenido");
        this.cadena = List.copyOf(cadena);
        this.rolDeLaEtapa = Objects.requireNonNull(rolDeLaEtapa, "rolDeLaEtapa");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.motivo = motivo;
        this.pedidaPor = Objects.requireNonNull(pedidaPor, "pedidaPor");
        this.actualizadaEn = Objects.requireNonNull(actualizadaEn, "actualizadaEn");
        if ((estado == EstadoDeGeneracion.FALLIDA) != (motivo != null)) {
            throw new IllegalArgumentException("Solo una generación fallida tiene motivo");
        }
    }

    /**
     * La generación que empieza con la solicitud de Aprobación.
     *
     * @param periodo el periodo
     * @param version la versión congelada
     * @param hashContenido su hash de contenido (D-100)
     * @param cadena los pasos de la cadena de firmas
     * @param rolDeLaEtapa el rol que puede reintentar
     * @param pedidaPor {@code sistema}
     * @param ahora cuándo
     * @return la generación EN_CURSO
     */
    public static GeneracionDeDocumento nueva(
            String periodo,
            int version,
            String hashContenido,
            List<PasoDeFirma> cadena,
            String rolDeLaEtapa,
            String pedidaPor,
            Instant ahora) {
        return new GeneracionDeDocumento(
                periodo,
                version,
                hashContenido,
                cadena,
                rolDeLaEtapa,
                EstadoDeGeneracion.EN_CURSO,
                null,
                pedidaPor,
                ahora);
    }

    /** Una generación tal como está guardada. */
    public static GeneracionDeDocumento de(
            String periodo,
            int version,
            String hashContenido,
            List<PasoDeFirma> cadena,
            String rolDeLaEtapa,
            EstadoDeGeneracion estado,
            Optional<String> motivo,
            String pedidaPor,
            Instant actualizadaEn) {
        return new GeneracionDeDocumento(
                periodo,
                version,
                hashContenido,
                cadena,
                rolDeLaEtapa,
                estado,
                motivo.orElse(null),
                pedidaPor,
                actualizadaEn);
    }

    /**
     * Empieza a generar. Con el documento ya generado no hace nada.
     *
     * @param quien {@code sistema} o quien pide el reintento
     * @param ahora cuándo
     * @return verdadero si hay que generar; falso si ya está GENERADA
     */
    public boolean iniciar(String quien, Instant ahora) {
        if (estado == EstadoDeGeneracion.GENERADA) {
            return false;
        }
        estado = EstadoDeGeneracion.EN_CURSO;
        motivo = null;
        pedidaPor = Objects.requireNonNull(quien, "quien");
        actualizadaEn = ahora;
        return true;
    }

    /**
     * La generación falló: queda FALLIDA con el motivo, cortado a {@value #MOTIVO_MAXIMO}
     * caracteres.
     */
    public void fallar(String motivo, Instant ahora) {
        String texto = Objects.requireNonNull(motivo, "motivo").strip();
        this.motivo =
                texto.length() <= MOTIVO_MAXIMO
                        ? texto
                        : texto.substring(0, MOTIVO_MAXIMO - 1) + "…";
        estado = EstadoDeGeneracion.FALLIDA;
        actualizadaEn = ahora;
    }

    /** El documento quedó registrado. */
    public void completar(Instant ahora) {
        estado = EstadoDeGeneracion.GENERADA;
        motivo = null;
        actualizadaEn = ahora;
    }

    /**
     * Si alguien con esos roles puede reintentar: el rol de la etapa que llegó con la solicitud.
     */
    public boolean puedeReintentar(Set<String> roles) {
        return roles.contains(rolDeLaEtapa);
    }

    /** El periodo. */
    public String periodo() {
        return periodo;
    }

    /** La versión congelada. */
    public int version() {
        return version;
    }

    /** El hash de contenido de la versión. */
    public String hashContenido() {
        return hashContenido;
    }

    /** La cadena de firmas, en el orden en que llegó. */
    public List<PasoDeFirma> cadena() {
        return cadena;
    }

    /** El rol de la etapa CONGELADO_EN_FIRMA. */
    public String rolDeLaEtapa() {
        return rolDeLaEtapa;
    }

    /** El estado. */
    public EstadoDeGeneracion estado() {
        return estado;
    }

    /** El motivo del fallo; vacío si no falló. */
    public Optional<String> motivo() {
        return Optional.ofNullable(motivo);
    }

    /** Quién la pidió la última vez. */
    public String pedidaPor() {
        return pedidaPor;
    }

    /** Cuándo cambió por última vez. */
    public Instant actualizadaEn() {
        return actualizadaEn;
    }
}
