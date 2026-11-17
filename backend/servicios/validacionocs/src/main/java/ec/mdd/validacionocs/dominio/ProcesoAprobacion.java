package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
 *       ({@link ParcialYaEnviado}), salvo que se devuelvan; con el último, el distributivo pasa a
 *       la siguiente etapa activa;
 *   <li>desde EN_CONSOLIDACION, solo su rol devuelve parciales, cada uno con su observación, si la
 *       etapa admite devolución; el distributivo vuelve a BORRADOR_CARRERA (F12-H05, D-98);
 *   <li>el avance genérico vale solo para las etapas opcionales sin historia propia, hoy
 *       BORRADOR_UNIDADES; las demás se avanzan con su propia operación ({@link
 *       EtapaConOperacionPropia});
 *   <li>desde EN_CONSOLIDACION, solo su rol envía a revisión, con las condiciones de D-97 en su
 *       orden (F10-H03);
 *   <li>desde EN_REVISION_VRA, solo su rol aprueba el contenido y el distributivo pasa a
 *       CONGELADO_EN_FIRMA; todo paso hacia esa etapa congela antes la versión (F13-H01, D-100);
 *   <li>un rechazo en firma devuelve el distributivo de CONGELADO_EN_FIRMA a la etapa activa
 *       anterior, y ningún paso vuelve a congelar la versión hasta que haya una nueva ({@link
 *       RequiereVersionNueva}, F14-H04, D-105);
 *   <li>un intento rechazado no cambia nada ni deja transición (FR-013).
 * </ul>
 */
public final class ProcesoAprobacion {

    /** Quién registra las transiciones que no pide una persona, como la cadena completa. */
    public static final String SISTEMA = "sistema";

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
     * A quién toca una transición (F16-H01, D-108): el rol responsable de la etapa a la que llega
     * el distributivo, según la copia de la configuración (principio VI).
     *
     * <ul>
     *   <li>en el envío de un parcial, que no cambia de etapa, el de la siguiente etapa activa, que
     *       es quien lo recibe;
     *   <li>en una devolución, los coordinadores de cada carrera devuelta, uno por carrera;
     *   <li>en los demás, el de la etapa {@code hacia}.
     * </ul>
     *
     * <p>Notificaciones resuelve el rol a usuarios y quita al autor.
     *
     * @param transicion una transición de este proceso
     * @return un destino por mensaje
     */
    public List<DestinoDeNotificacion> destinoDe(Transicion transicion) {
        return switch (transicion.tipo()) {
            case ENVIO_PARCIAL ->
                    List.of(
                            new DestinoDeNotificacion(
                                    etapa(siguienteActiva(TipoDeEtapa.BORRADOR_CARRERA)).rol(),
                                    Optional.empty()));
            case DEVOLUCION ->
                    transicion.observaciones().keySet().stream()
                            .map(
                                    carrera ->
                                            new DestinoDeNotificacion(
                                                    etapa(transicion.hacia()).rol(),
                                                    Optional.of(carrera)))
                            .toList();
            case PASO_DE_ETAPA, RECHAZO_EN_FIRMA, VERSION_NUEVA ->
                    List.of(
                            new DestinoDeNotificacion(
                                    etapa(transicion.hacia()).rol(), Optional.empty()));
        };
    }

    /**
     * La etapa en la copia de la configuración, con su nombre y su rol.
     *
     * @param tipo cuál
     * @return la etapa
     */
    public EtapaDelProceso etapaDe(TipoDeEtapa tipo) {
        return etapa(tipo);
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
        validarAvance(quien);
        Transicion paso = paso(quien, version, hashDelDistributivo, ahora);
        transiciones.add(paso);
        return List.of(paso);
    }

    /** La zona de la institución: las fechas de las resoluciones son días de aquí (D-107). */
    public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    /**
     * La Secretaría general académica registra la resolución del OCS y el distributivo pasa de
     * EN_OCS a APROBADO (F15-H01, RF-37, D-107): registrar es aprobar, y después no se corrige. La
     * transición lleva la versión y el hash de la congelación, la que se firmó, y la resolución.
     *
     * <p>La fecha va desde el día en que el distributivo llegó a EN_OCS hasta el de {@code ahora},
     * los dos en {@link #ZONA}: la resolución aprueba la versión ya firmada, y se comparan días, no
     * instantes, para que una resolución de hoy no se rechace por la hora.
     *
     * @param quien quien registra
     * @param resolucion el número, la fecha y el PDF, ya comprobados
     * @param ahora cuándo se registra
     * @return la transición nueva
     * @throws EtapaNoAdmiteResolucion si el distributivo no está en EN_OCS
     * @throws RolDeOtraEtapa si no tiene el rol de EN_OCS en la copia de la configuración
     * @throws ResolucionFueraDeRango si la fecha es futura o anterior a la llegada a EN_OCS
     */
    public List<Transicion> registrarResolucion(
            Solicitante quien, ResolucionOcs resolucion, Instant ahora) {
        validarResolucion(quien, resolucion, ahora);
        Transicion congelada =
                congelacion().orElseThrow(() -> new IllegalStateException("En EN_OCS sin firma"));
        TipoDeEtapa desde = estado;
        estado = siguienteActiva(desde);
        Transicion paso =
                new Transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        desde,
                        estado,
                        Optional.empty(),
                        congelada.version(),
                        congelada.hash(),
                        quien.usuario(),
                        ahora,
                        Map.of(),
                        Optional.empty(),
                        Optional.of(resolucion));
        transiciones.add(paso);
        return List.of(paso);
    }

    /**
     * Comprueba, sin cambiar nada, que {@code quien} puede registrar esa resolución ahora: las
     * mismas reglas de {@link #registrarResolucion}. El caso de uso lo llama antes de guardar el
     * PDF, para que un rechazo no deje ni el archivo (F15-H01).
     *
     * @param quien quien registra
     * @param resolucion el número, la fecha y el PDF
     * @param ahora cuándo se registra
     * @throws EtapaNoAdmiteResolucion si el distributivo no está en EN_OCS
     * @throws RolDeOtraEtapa si no tiene el rol de EN_OCS
     * @throws ResolucionFueraDeRango si la fecha es futura o anterior a la llegada a EN_OCS
     */
    public void validarResolucion(Solicitante quien, ResolucionOcs resolucion, Instant ahora) {
        Objects.requireNonNull(resolucion, "resolucion");
        if (estado != TipoDeEtapa.EN_OCS) {
            throw new EtapaNoAdmiteResolucion();
        }
        exigirRol(quien, estado);
        LocalDate llegada =
                transiciones.stream()
                        .filter(una -> una.hacia() == TipoDeEtapa.EN_OCS)
                        .reduce((primera, ultima) -> ultima)
                        .map(una -> LocalDate.ofInstant(una.momento(), ZONA))
                        .orElseThrow(() -> new IllegalStateException("En EN_OCS sin llegar"));
        LocalDate hoy = LocalDate.ofInstant(ahora, ZONA);
        if (resolucion.fecha().isBefore(llegada) || resolucion.fecha().isAfter(hoy)) {
            throw new ResolucionFueraDeRango();
        }
    }

    /** La transición que aprobó el distributivo con la resolución del OCS, si ya se registró. */
    public Optional<Transicion> resolucion() {
        return transiciones.stream()
                .filter(una -> una.resolucion().isPresent())
                .reduce((primera, ultima) -> ultima);
    }

    /**
     * Comprueba que {@code quien} puede usar el avance genérico, sin cambiar nada. El caso de uso
     * lo llama antes de pedirle a Generación que congele, si el paso llega a CONGELADO_EN_FIRMA
     * (F13-H01).
     *
     * @param quien quien avanza
     * @throws RequiereResolucion si el distributivo está en EN_OCS (F15-H01)
     * @throws EtapaConOperacionPropia si la etapa no es de las del avance genérico
     * @throws RolDeOtraEtapa si no tiene el rol de la etapa actual
     * @throws RequiereVersionNueva si el avance congelaría la versión de un rechazo en firma
     */
    public void validarAvance(Solicitante quien) {
        // F15-H01: a APROBADO solo se pasa registrando la resolución del OCS (RF-37).
        if (estado == TipoDeEtapa.EN_OCS) {
            throw new RequiereResolucion();
        }
        if (!CON_AVANCE_GENERICO.contains(estado)) {
            throw new EtapaConOperacionPropia();
        }
        exigirRol(quien, estado);
        if (congelaAlAvanzar()) {
            exigirSinRechazoPendiente();
        }
    }

    /**
     * Si enviar el parcial de {@code carrera} completa los parciales y el distributivo pasa de
     * etapa: los demás ya están enviados (F13-H01). No cambia nada.
     *
     * @param carrera la carrera del parcial que se envía
     * @return verdadero si es el último
     */
    public boolean esElUltimoParcial(String carrera) {
        return parciales.entrySet().stream()
                .allMatch(
                        uno ->
                                uno.getKey().equals(carrera)
                                        || uno.getValue() == EstadoDeEnvio.ENVIADO);
    }

    /**
     * Comprueba que {@code quien} puede enviar el distributivo a revisión con esas condiciones, sin
     * cambiar nada. El caso de uso lo llama antes de pedirle a Generación que congele, cuando
     * EN_REVISION_VRA está desactivada, para que un envío que no cabe no congele (F13-H01).
     *
     * @param quien quien envía
     * @param condiciones lo que Generación dice del consolidado
     * @throws RolDeOtraEtapa si no tiene el rol de EN_CONSOLIDACION
     * @throws EtapaNoAdmiteRevision si el distributivo no está en EN_CONSOLIDACION
     * @throws ParcialesPendientes si algún parcial no está enviado
     * @throws SinConsolidar si la versión vigente no se consolidó
     * @throws ConsolidadoDesactualizado si algo cambió después de consolidar
     * @throws SinInforme si no tiene informe sustentado
     * @throws InconsistenciasAbiertas si quedan duras o blandas sin justificar (MD-07)
     */
    public void validarRevision(Solicitante quien, CondicionesDeRevision condiciones) {
        validarRevision(quien);
        // El orden de D-97: de lo que se arregla antes a lo que se arregla después.
        if (!condiciones.consolidado()) {
            throw new SinConsolidar();
        }
        if (condiciones.desactualizado()) {
            throw new ConsolidadoDesactualizado();
        }
        if (condiciones.informe().isEmpty()) {
            throw new SinInforme();
        }
        if (condiciones.duras() + condiciones.blandas() > 0) {
            throw new InconsistenciasAbiertas(
                    Math.toIntExact(condiciones.duras()), Math.toIntExact(condiciones.blandas()));
        }
    }

    /**
     * Comprueba que {@code quien} puede enviar el distributivo a revisión, sin cambiar nada
     * (F10-H03, D-97). El caso de uso lo llama antes de pedirle a Generación las condiciones del
     * consolidado, para que un intento que no cabe no la llame (FR-012).
     *
     * @param quien quien envía
     * @throws RolDeOtraEtapa si no tiene el rol de EN_CONSOLIDACION en la copia de la configuración
     * @throws EtapaNoAdmiteRevision si el distributivo no está en EN_CONSOLIDACION
     * @throws ParcialesPendientes si algún parcial no está enviado
     * @throws RequiereVersionNueva si el envío congelaría la versión de un rechazo en firma
     */
    public void validarRevision(Solicitante quien) {
        exigirRol(quien, TipoDeEtapa.EN_CONSOLIDACION);
        if (estado != TipoDeEtapa.EN_CONSOLIDACION) {
            throw new EtapaNoAdmiteRevision();
        }
        // Pendiente o devuelto: su carrera todavía no lo envió (F12-H05).
        if (parciales.values().stream().anyMatch(uno -> uno != EstadoDeEnvio.ENVIADO)) {
            throw new ParcialesPendientes();
        }
        if (congelaAlAvanzar()) {
            exigirSinRechazoPendiente();
        }
    }

    /**
     * Envía el distributivo a revisión: pasa de EN_CONSOLIDACION a la siguiente etapa activa (por
     * defecto, EN_REVISION_VRA). No notifica a nadie: eso es de F16-H01 (D-97).
     *
     * @param quien quien envía
     * @param condiciones lo que Generación dice del consolidado, calculado al enviar
     * @param version la versión vigente del distributivo
     * @param hashDelDistributivo el hash de todos los parciales
     * @param ahora cuándo
     * @return la transición nueva, un paso de etapa
     * @throws RolDeOtraEtapa si no tiene el rol de EN_CONSOLIDACION
     * @throws EtapaNoAdmiteRevision si el distributivo no está en EN_CONSOLIDACION
     * @throws ParcialesPendientes si algún parcial no está enviado
     * @throws SinConsolidar si la versión vigente no se consolidó
     * @throws ConsolidadoDesactualizado si algo cambió después de consolidar
     * @throws SinInforme si no tiene informe sustentado
     * @throws InconsistenciasAbiertas si quedan duras o blandas sin justificar (MD-07)
     */
    public List<Transicion> enviarARevision(
            Solicitante quien,
            CondicionesDeRevision condiciones,
            int version,
            String hashDelDistributivo,
            Instant ahora) {
        validarRevision(quien, condiciones);
        Transicion paso = paso(quien, version, hashDelDistributivo, ahora);
        transiciones.add(paso);
        return List.of(paso);
    }

    /**
     * Comprueba que {@code quien} puede devolver esos parciales, sin cambiar nada (F12-H05, D-98).
     * El caso de uso lo llama antes de pedirle a Generación que los marque, para que un intento
     * rechazado no la toque (FR-013). Las comprobaciones van en el orden de research §10.
     *
     * @param quien quien devuelve
     * @param devoluciones las carreras, cada una con su observación
     * @throws RolDeOtraEtapa si no tiene el rol de EN_CONSOLIDACION en la copia de la configuración
     * @throws EtapaNoAdmiteDevolucion si el distributivo no está en EN_CONSOLIDACION
     * @throws DevolucionDesactivada si EN_CONSOLIDACION no admite devolución en la copia
     * @throws CarreraRepetida si una carrera está dos veces
     * @throws ParcialNoEncontrado si una carrera no tiene parcial en el distributivo
     * @throws ParcialNoEnviado si el parcial de una carrera no está enviado
     */
    public void validarDevolucion(Solicitante quien, List<DevolucionDeParcial> devoluciones) {
        exigirRol(quien, TipoDeEtapa.EN_CONSOLIDACION);
        if (estado != TipoDeEtapa.EN_CONSOLIDACION) {
            throw new EtapaNoAdmiteDevolucion();
        }
        if (!etapa(TipoDeEtapa.EN_CONSOLIDACION).admiteDevolucion()) {
            throw new DevolucionDesactivada();
        }
        Set<String> vistas = new HashSet<>();
        for (DevolucionDeParcial devolucion : devoluciones) {
            if (!vistas.add(devolucion.carrera())) {
                throw new CarreraRepetida(devolucion.carrera());
            }
        }
        for (DevolucionDeParcial devolucion : devoluciones) {
            EstadoDeEnvio actual = parciales.get(devolucion.carrera());
            if (actual == null) {
                throw new ParcialNoEncontrado();
            }
            if (actual != EstadoDeEnvio.ENVIADO) {
                throw new ParcialNoEnviado(devolucion.carrera());
            }
        }
    }

    /**
     * Devuelve los parciales de una o varias carreras, cada una con su observación, todos o ninguno
     * (FR-005). Quedan DEVUELTO y se corrigen y reenvían en BORRADOR_CARRERA, adonde vuelve el
     * distributivo; los demás siguen enviados. No crea una versión nueva: no hay firmas que anular
     * (RF-59). No notifica a nadie: eso es de F16-H01 (D-98).
     *
     * @param quien quien devuelve
     * @param devoluciones las carreras, cada una con su observación; al menos una
     * @param version la versión vigente del distributivo
     * @param hashDelDistributivo el hash de todos los parciales
     * @param ahora cuándo
     * @return la transición nueva, una devolución con las observaciones
     * @throws RolDeOtraEtapa si no tiene el rol de EN_CONSOLIDACION
     * @throws EtapaNoAdmiteDevolucion si el distributivo no está en EN_CONSOLIDACION
     * @throws DevolucionDesactivada si EN_CONSOLIDACION no admite devolución
     * @throws CarreraRepetida si una carrera está dos veces
     * @throws ParcialNoEncontrado si una carrera no tiene parcial
     * @throws ParcialNoEnviado si el parcial de una carrera no está enviado
     */
    public List<Transicion> devolver(
            Solicitante quien,
            List<DevolucionDeParcial> devoluciones,
            int version,
            String hashDelDistributivo,
            Instant ahora) {
        if (devoluciones.isEmpty()) {
            throw new IllegalArgumentException("Una devolución lleva al menos una carrera");
        }
        validarDevolucion(quien, devoluciones);
        Map<String, Observacion> observaciones = new TreeMap<>();
        devoluciones.forEach(
                devolucion -> {
                    parciales.put(devolucion.carrera(), EstadoDeEnvio.DEVUELTO);
                    observaciones.put(devolucion.carrera(), devolucion.observacion());
                });
        TipoDeEtapa desde = estado;
        estado = TipoDeEtapa.BORRADOR_CARRERA;
        Transicion devolucion =
                new Transicion(
                        TipoDeTransicion.DEVOLUCION,
                        desde,
                        estado,
                        Optional.empty(),
                        version,
                        hashDelDistributivo,
                        quien.usuario(),
                        ahora,
                        observaciones);
        transiciones.add(devolucion);
        return List.of(devolucion);
    }

    /**
     * Comprueba que {@code quien} puede aprobar el contenido, sin cambiar nada (F13-H01, D-100). El
     * caso de uso lo llama antes de pedirle a Generación que congele la versión, para que un
     * intento que no cabe no la toque (FR-009).
     *
     * @param quien quien aprueba
     * @throws RolDeOtraEtapa si no tiene el rol de EN_REVISION_VRA en la copia de la configuración
     * @throws EtapaNoAdmiteAprobacion si el distributivo no está en EN_REVISION_VRA
     * @throws RequiereVersionNueva si volvió por un rechazo en firma y no hay versión nueva
     */
    public void validarAprobacion(Solicitante quien) {
        exigirRol(quien, TipoDeEtapa.EN_REVISION_VRA);
        if (estado != TipoDeEtapa.EN_REVISION_VRA) {
            throw new EtapaNoAdmiteAprobacion();
        }
        exigirSinRechazoPendiente();
    }

    /**
     * Aprueba el contenido: pasa de EN_REVISION_VRA a la siguiente etapa activa,
     * CONGELADO_EN_FIRMA, que es del esqueleto fijo. La versión ya la congeló Generación por orden
     * de Aprobación (MD-04); la transición lleva su número y su hash, el mismo del envío a revisión
     * porque nada cambió durante la revisión (D-99). Sin comentario y sin notificar: eso es de
     * F16-H01.
     *
     * @param quien quien aprueba
     * @param version la versión congelada
     * @param hashDelDistributivo el hash de la versión congelada
     * @param ahora cuándo
     * @return la transición nueva, un paso de etapa
     * @throws RolDeOtraEtapa si no tiene el rol de EN_REVISION_VRA
     * @throws EtapaNoAdmiteAprobacion si el distributivo no está en EN_REVISION_VRA
     */
    public List<Transicion> aprobarContenido(
            Solicitante quien, int version, String hashDelDistributivo, Instant ahora) {
        validarAprobacion(quien);
        Transicion paso = paso(quien, version, hashDelDistributivo, ahora);
        transiciones.add(paso);
        return List.of(paso);
    }

    /**
     * Si el próximo paso de etapa llega a CONGELADO_EN_FIRMA: entonces la versión se congela antes
     * (D-100, research §3). Es la regla de la etapa de llegada, así que vale también para el envío
     * a revisión cuando EN_REVISION_VRA está desactivada.
     */
    public boolean congelaAlAvanzar() {
        return estado.ordinal() < TipoDeEtapa.CONGELADO_EN_FIRMA.ordinal()
                && siguienteActiva(estado) == TipoDeEtapa.CONGELADO_EN_FIRMA;
    }

    /**
     * La congelación de la versión: el último paso de etapa hacia CONGELADO_EN_FIRMA, con quién,
     * cuándo, la versión y el hash (F13-H01). Vacía si el distributivo todavía no llegó.
     */
    public Optional<Transicion> congelacion() {
        return transiciones.stream()
                .filter(una -> una.tipo() == TipoDeTransicion.PASO_DE_ETAPA)
                .filter(una -> una.hacia() == TipoDeEtapa.CONGELADO_EN_FIRMA)
                .reduce((primera, ultima) -> ultima);
    }

    /**
     * La cadena de firmas de la versión congelada quedó completa: el distributivo sale de
     * CONGELADO_EN_FIRMA hacia la siguiente etapa activa, EN_OCS del esqueleto fijo (F14-H03,
     * D-104). Lo pide Firma con un aviso que puede llegar más de una vez, así que es idempotente:
     * si ya salió de firma con esa versión, o el distributivo está en otra etapa, o la versión no
     * es la congelada, no hace nada. Mover el flujo no es de Firma (MD-05): lo decide este
     * agregado.
     *
     * @param version la versión que se firmó
     * @param ahora cuándo se completó
     * @return la transición nueva, por {@value #SISTEMA}, o vacío si no corresponde
     */
    public List<Transicion> completarFirmas(int version, Instant ahora) {
        Optional<Transicion> congelada = congelacion();
        if (estado != TipoDeEtapa.CONGELADO_EN_FIRMA
                || congelada.filter(una -> una.version() == version).isEmpty()) {
            return List.of();
        }
        Transicion paso =
                paso(
                        new Solicitante(SISTEMA, Set.of(), Optional.empty()),
                        version,
                        congelada.get().hash(),
                        ahora);
        transiciones.add(paso);
        return List.of(paso);
    }

    /**
     * Si un rechazo en firma de {@code version} cambiaría algo (F12-H04): el distributivo está en
     * CONGELADO_EN_FIRMA con esa versión congelada. Aprobación lo pregunta antes de ordenar a
     * Generación la versión nueva, sin bloquear el proceso; {@link #rechazarEnFirma} lo vuelve a
     * comprobar con el proceso bloqueado.
     *
     * @param version la versión del aviso
     */
    public boolean admiteRechazoEnFirma(int version) {
        return estado == TipoDeEtapa.CONGELADO_EN_FIRMA
                && congelacion().filter(una -> una.version() == version).isPresent();
    }

    /**
     * El firmante en turno rechazó en firma la versión congelada: el distributivo vuelve de
     * CONGELADO_EN_FIRMA a la etapa activa anterior, EN_REVISION_VRA con la configuración por
     * defecto (F14-H04, D-105). Es la regla de {@link #siguienteActiva} al revés: si la copia de la
     * configuración desactiva la revisión, no vuelve a una etapa inactiva. Lo pide Firma con un
     * aviso que puede llegar más de una vez, así que es idempotente, como {@link #completarFirmas}:
     * fuera de CONGELADO_EN_FIRMA o con otra versión que la congelada, no hace nada.
     *
     * @param version la versión rechazada
     * @param rechazo quién rechazó, en qué paso y por qué
     * @param ahora cuándo se registra
     * @return la transición nueva, por {@value #SISTEMA}, o vacío si no corresponde
     */
    public List<Transicion> rechazarEnFirma(
            int version, RechazoDelFirmante rechazo, Instant ahora) {
        Objects.requireNonNull(rechazo, "rechazo");
        if (!admiteRechazoEnFirma(version)) {
            return List.of();
        }
        Optional<Transicion> congelada = congelacion();
        TipoDeEtapa desde = estado;
        estado = anteriorActiva(desde);
        Transicion vuelta =
                new Transicion(
                        TipoDeTransicion.RECHAZO_EN_FIRMA,
                        desde,
                        estado,
                        Optional.empty(),
                        version,
                        congelada.get().hash(),
                        SISTEMA,
                        ahora,
                        Map.of(),
                        Optional.of(rechazo));
        transiciones.add(vuelta);
        return List.of(vuelta);
    }

    /**
     * El rechazo en firma por el que volvió el distributivo, mientras sea la última transición
     * (F14-H04). La versión nueva de F12-H04 agrega otra transición y lo deja atrás.
     */
    public Optional<Transicion> rechazoPendiente() {
        return transiciones.isEmpty()
                ? Optional.empty()
                : Optional.of(transiciones.getLast())
                        .filter(una -> una.tipo() == TipoDeTransicion.RECHAZO_EN_FIRMA);
    }

    /**
     * Generación anuló la versión rechazada en firma y creó la siguiente (F12-H04, D-106): queda la
     * transición {@code VERSION_NUEVA}, de la etapa a la que volvió el distributivo a la misma, por
     * {@value #SISTEMA}. Con ella el rechazo deja de estar pendiente y se puede aprobar otra vez,
     * sin cambiar la regla de D-105. Solo corresponde justo después del rechazo de {@code anulada}:
     * un reintento, un rechazo de otra versión o ninguno no hacen nada.
     *
     * @param anulada la versión rechazada en firma
     * @param nueva la versión que creó Generación
     * @param hash el hash del distributivo de la versión nueva
     * @param ahora cuándo se registra
     * @return la transición nueva, o vacío si no corresponde
     */
    public List<Transicion> registrarVersionNueva(
            int anulada, int nueva, String hash, Instant ahora) {
        if (rechazoPendiente().filter(una -> una.version() == anulada).isEmpty()) {
            return List.of();
        }
        Transicion version =
                new Transicion(
                        TipoDeTransicion.VERSION_NUEVA,
                        estado,
                        estado,
                        Optional.empty(),
                        nueva,
                        hash,
                        SISTEMA,
                        ahora);
        transiciones.add(version);
        return List.of(version);
    }

    /**
     * El rechazo en firma de la versión vigente (F12-H04): el último, si no hubo una congelación
     * después. A diferencia de {@link #rechazoPendiente}, sigue presente con la versión nueva, para
     * que el Vicerrector vea por qué se anuló la anterior, hasta que la apruebe.
     */
    public Optional<Transicion> rechazoVigente() {
        for (int i = transiciones.size() - 1; i >= 0; i--) {
            Transicion una = transiciones.get(i);
            if (una.tipo() == TipoDeTransicion.RECHAZO_EN_FIRMA) {
                return Optional.of(una);
            }
            if (una.tipo() == TipoDeTransicion.PASO_DE_ETAPA
                    && una.hacia() == TipoDeEtapa.CONGELADO_EN_FIRMA) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * La última etapa activa antes de {@code etapa}. Siempre existe después de BORRADOR_CARRERA,
     * que es del esqueleto fijo.
     */
    public TipoDeEtapa anteriorActiva(TipoDeEtapa etapa) {
        return etapas.stream()
                .filter(una -> una.tipo().ordinal() < etapa.ordinal() && una.activa())
                .map(EtapaDelProceso::tipo)
                .reduce((primera, ultima) -> ultima)
                .orElseThrow(() -> new IllegalStateException("BORRADOR_CARRERA es la primera"));
    }

    // F14-H04, D-105: congelar otra vez la versión de un rechazo en firma pediría a Firma el
    // documento rechazado (D-101); hace falta la versión nueva de F12-H04.
    private void exigirSinRechazoPendiente() {
        if (rechazoPendiente().isPresent()) {
            throw new RequiereVersionNueva();
        }
    }

    /** Lo que {@code quien} puede hacer ahora (FR-014). */
    public List<AccionDelProceso> acciones(Solicitante quien) {
        if (estado == TipoDeEtapa.BORRADOR_CARRERA
                && tieneRol(quien, TipoDeEtapa.BORRADOR_CARRERA)
                && quien.carreraPropia()
                        .map(
                                carrera ->
                                        parciales.containsKey(carrera)
                                                && parciales.get(carrera) != EstadoDeEnvio.ENVIADO)
                        .orElse(false)) {
            return List.of(AccionDelProceso.ENVIAR_PARCIAL);
        }
        if (CON_AVANCE_GENERICO.contains(estado) && tieneRol(quien, estado)) {
            return List.of(AccionDelProceso.AVANZAR);
        }
        // Aunque falten condiciones: la pantalla dice cuáles (F10-H03, FR-013). Devolver, si la
        // copia de la configuración lo admite (F12-H05, FR-016).
        if (estado == TipoDeEtapa.EN_CONSOLIDACION
                && tieneRol(quien, TipoDeEtapa.EN_CONSOLIDACION)) {
            return etapa(TipoDeEtapa.EN_CONSOLIDACION).admiteDevolucion()
                    ? List.of(
                            AccionDelProceso.ENVIAR_A_REVISION, AccionDelProceso.DEVOLVER_PARCIALES)
                    : List.of(AccionDelProceso.ENVIAR_A_REVISION);
        }
        // F15-H01: la Secretaría registra la resolución del OCS.
        if (estado == TipoDeEtapa.EN_OCS && tieneRol(quien, TipoDeEtapa.EN_OCS)) {
            return List.of(AccionDelProceso.REGISTRAR_RESOLUCION);
        }
        // Con un rechazo en firma pendiente no se aprueba hasta la versión nueva (F14-H04).
        if (estado == TipoDeEtapa.EN_REVISION_VRA
                && tieneRol(quien, TipoDeEtapa.EN_REVISION_VRA)
                && rechazoPendiente().isEmpty()) {
            return List.of(AccionDelProceso.APROBAR_CONTENIDO);
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
