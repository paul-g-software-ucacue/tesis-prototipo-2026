package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * El proceso de aprobación de un distributivo (modelo de dominio §5, RF-34, F12-H02): la etapa
 * actual, el envío del parcial de cada carrera en BORRADOR_CARRERA, la copia de la configuración
 * con la que empezó el distributivo (FR-003) y el historial de transiciones.
 *
 * <p>Sus reglas:
 *
 * <ul>
 *   <li>solo el rol de la etapa actual, según la copia de la configuración, mueve el distributivo
 *       ({@link RolDeOtraEtapa}); un coordinador con alcance propio, solo su carrera ({@link
 *       ParcialDeOtraCarrera}, D-64);
 *   <li>los parciales se envían en BORRADOR_CARRERA ({@link EtapaNoAdmiteEnvio}), una sola vez
 *       ({@link ParcialYaEnviado}); con el último, el distributivo pasa a la siguiente etapa
 *       activa;
 *   <li>el avance genérico vale solo para las etapas opcionales sin historia propia, hoy
 *       BORRADOR_UNIDADES; las demás se avanzan con su propia operación ({@link
 *       EtapaConOperacionPropia});
 *   <li>un intento rechazado no cambia nada ni deja transición (FR-013).
 * </ul>
 */
public final class ProcesoAprobacion {

    /** Las etapas que se avanzan con el avance genérico: opcionales y sin historia propia. */
    public static final Set<TipoDeEtapa> CON_AVANCE_GENERICO =
            Set.of(TipoDeEtapa.BORRADOR_UNIDADES);

    private final String periodo;
    private final List<EtapaDelProceso> etapas;
    private final Instant distributivoCreadoEn;
    private final Map<String, EstadoDeEnvio> parciales;
    private final List<Transicion> transiciones;
    private TipoDeEtapa estado;

    private ProcesoAprobacion(
            String periodo,
            List<EtapaDelProceso> etapas,
            TipoDeEtapa estado,
            Instant distributivoCreadoEn,
            Map<String, EstadoDeEnvio> parciales,
            List<Transicion> transiciones) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.etapas = List.copyOf(etapas);
        this.estado = Objects.requireNonNull(estado, "estado");
        this.distributivoCreadoEn = Objects.requireNonNull(distributivoCreadoEn, "creadoEn");
        this.parciales = new TreeMap<>(parciales);
        this.transiciones = new ArrayList<>(transiciones);
        if (this.etapas.size() != TipoDeEtapa.values().length) {
            throw new IllegalArgumentException("La copia de la configuración lleva las 9 etapas");
        }
        if (!etapa(estado).activa()) {
            throw new IllegalArgumentException("El distributivo solo está en una etapa activa");
        }
    }

    /**
     * El proceso de un distributivo recién creado: en BORRADOR_CARRERA, con todos sus parciales
     * pendientes.
     *
     * @param periodo el periodo del distributivo
     * @param etapas la copia de la configuración que regía al crearse, las 9 en orden
     * @param distributivoCreadoEn cuándo se creó el distributivo
     * @param carreras las carreras con parcial en la versión vigente
     * @return el proceso
     */
    public static ProcesoAprobacion nuevo(
            String periodo,
            List<EtapaDelProceso> etapas,
            Instant distributivoCreadoEn,
            Collection<String> carreras) {
        Map<String, EstadoDeEnvio> parciales = new TreeMap<>();
        carreras.forEach(carrera -> parciales.put(carrera, EstadoDeEnvio.PENDIENTE));
        return new ProcesoAprobacion(
                periodo,
                etapas,
                TipoDeEtapa.BORRADOR_CARRERA,
                distributivoCreadoEn,
                parciales,
                List.of());
    }

    /** Un proceso tal como está guardado. */
    public static ProcesoAprobacion de(
            String periodo,
            List<EtapaDelProceso> etapas,
            TipoDeEtapa estado,
            Instant distributivoCreadoEn,
            Map<String, EstadoDeEnvio> parciales,
            List<Transicion> transiciones) {
        return new ProcesoAprobacion(
                periodo, etapas, estado, distributivoCreadoEn, parciales, transiciones);
    }

    /** El periodo del distributivo que acompaña. */
    public String periodo() {
        return periodo;
    }

    /** La copia de la configuración, las 9 etapas en orden. */
    public List<EtapaDelProceso> etapas() {
        return etapas;
    }

    /** La etapa actual. */
    public TipoDeEtapa estado() {
        return estado;
    }

    /** El nombre de la etapa actual en la copia de la configuración. */
    public String nombreDeLaEtapa() {
        return etapa(estado).nombre();
    }

    /** Cuándo se creó el distributivo que acompaña. */
    public Instant distributivoCreadoEn() {
        return distributivoCreadoEn;
    }

    /** El envío de cada parcial, por carrera. */
    public Map<String, EstadoDeEnvio> parciales() {
        return Collections.unmodifiableMap(parciales);
    }

    /** Las transiciones, en orden. */
    public List<Transicion> transiciones() {
        return List.copyOf(transiciones);
    }

    /**
     * La primera etapa activa después de {@code etapa}. Siempre existe antes de PUBLICADO, que es
     * del esqueleto fijo.
     */
    public TipoDeEtapa siguienteActiva(TipoDeEtapa etapa) {
        return etapas.stream()
                .filter(una -> una.tipo().ordinal() > etapa.ordinal() && una.activa())
                .map(EtapaDelProceso::tipo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PUBLICADO es la última etapa"));
    }

    /**
     * Suma como pendientes los parciales que Generación agregó después (una carrera con su primera
     * propuesta, research §6).
     */
    public void sumarParciales(Collection<String> carreras) {
        carreras.forEach(carrera -> parciales.putIfAbsent(carrera, EstadoDeEnvio.PENDIENTE));
    }

    /**
     * Envía el parcial de una carrera. Si era el último, el distributivo pasa también a la
     * siguiente etapa activa.
     *
     * @param carrera la carrera del parcial
     * @param quien quien lo envía
     * @param version la versión vigente del distributivo
     * @param hashDelParcial el hash del contenido del parcial
     * @param hashDelDistributivo el hash de todos los parciales, para el paso de etapa
     * @param ahora cuándo
     * @return las transiciones nuevas: el envío y, si era el último, el paso de etapa
     */
    public List<Transicion> enviarParcial(
            String carrera,
            Solicitante quien,
            int version,
            String hashDelParcial,
            String hashDelDistributivo,
            Instant ahora) {
        validarEnvio(carrera, quien);
        List<Transicion> nuevas = new ArrayList<>();
        nuevas.add(
                new Transicion(
                        TipoDeTransicion.ENVIO_PARCIAL,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.of(carrera),
                        version,
                        hashDelParcial,
                        quien.usuario(),
                        ahora));
        parciales.put(carrera, EstadoDeEnvio.ENVIADO);
        if (parciales.values().stream().allMatch(EstadoDeEnvio.ENVIADO::equals)) {
            nuevas.add(paso(quien, version, hashDelDistributivo, ahora));
        }
        transiciones.addAll(nuevas);
        return List.copyOf(nuevas);
    }

    /**
     * Comprueba que {@code quien} puede enviar el parcial de {@code carrera}, sin cambiar nada. El
     * caso de uso lo llama antes de pedirle a Generación que marque el parcial, para que un intento
     * rechazado no lo toque (FR-013).
     *
     * @throws RolDeOtraEtapa si no tiene el rol de BORRADOR_CARRERA
     * @throws ParcialDeOtraCarrera si su carrera es otra
     * @throws EtapaNoAdmiteEnvio si el distributivo ya no está en BORRADOR_CARRERA
     * @throws ParcialYaEnviado si el parcial ya se envió
     * @throws ParcialNoEncontrado si la carrera no tiene parcial en el distributivo
     */
    public void validarEnvio(String carrera, Solicitante quien) {
        exigirRol(quien, TipoDeEtapa.BORRADOR_CARRERA);
        if (quien.carreraPropia().filter(propia -> !propia.equals(carrera)).isPresent()) {
            throw new ParcialDeOtraCarrera();
        }
        if (estado != TipoDeEtapa.BORRADOR_CARRERA) {
            throw new EtapaNoAdmiteEnvio();
        }
        EstadoDeEnvio actual = parciales.get(carrera);
        if (actual == null) {
            throw new ParcialNoEncontrado();
        }
        if (actual == EstadoDeEnvio.ENVIADO) {
            throw new ParcialYaEnviado();
        }
    }

    /**
     * El avance genérico: pasa a la siguiente etapa activa, solo en una etapa opcional sin historia
     * propia y con su rol.
     *
     * @param quien quien avanza
     * @param version la versión vigente del distributivo
     * @param hashDelDistributivo el hash de todos los parciales
     * @param ahora cuándo
     * @return la transición nueva
     */
    public List<Transicion> avanzar(
            Solicitante quien, int version, String hashDelDistributivo, Instant ahora) {
        if (!CON_AVANCE_GENERICO.contains(estado)) {
            throw new EtapaConOperacionPropia();
        }
        exigirRol(quien, estado);
        Transicion paso = paso(quien, version, hashDelDistributivo, ahora);
        transiciones.add(paso);
        return List.of(paso);
    }

    /** Lo que {@code quien} puede hacer ahora (FR-014). */
    public List<AccionDelProceso> acciones(Solicitante quien) {
        if (estado == TipoDeEtapa.BORRADOR_CARRERA
                && tieneRol(quien, TipoDeEtapa.BORRADOR_CARRERA)
                && quien.carreraPropia()
                        .map(carrera -> parciales.get(carrera) == EstadoDeEnvio.PENDIENTE)
                        .orElse(false)) {
            return List.of(AccionDelProceso.ENVIAR_PARCIAL);
        }
        if (CON_AVANCE_GENERICO.contains(estado) && tieneRol(quien, estado)) {
            return List.of(AccionDelProceso.AVANZAR);
        }
        return List.of();
    }

    private Transicion paso(Solicitante quien, int version, String hash, Instant ahora) {
        TipoDeEtapa desde = estado;
        estado = siguienteActiva(desde);
        return new Transicion(
                TipoDeTransicion.PASO_DE_ETAPA,
                desde,
                estado,
                Optional.empty(),
                version,
                hash,
                quien.usuario(),
                ahora);
    }

    private void exigirRol(Solicitante quien, TipoDeEtapa etapa) {
        if (!tieneRol(quien, etapa)) {
            throw new RolDeOtraEtapa();
        }
    }

    private boolean tieneRol(Solicitante quien, TipoDeEtapa etapa) {
        return quien.roles().contains(etapa(etapa).rol());
    }

    private EtapaDelProceso etapa(TipoDeEtapa tipo) {
        return etapas.stream()
                .filter(una -> una.tipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la etapa " + tipo));
    }
}
