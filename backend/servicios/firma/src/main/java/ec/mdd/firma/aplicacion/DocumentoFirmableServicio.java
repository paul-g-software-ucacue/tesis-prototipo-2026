package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.Firmante;
import ec.mdd.firma.dominio.FirmanteSinNombre;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.puertos.entrada.PrepararDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.SolicitudDeDocumento;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.GeneradorDePdf;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * El documento firmable de la versión congelada (F13-H02, D-101). Aprobación entrega la cadena al
 * entrar en firma (MD-05); este servicio genera el PDF/A, lo guarda por su hash y lo registra una
 * sola vez.
 *
 * <p>Generar es en tres pasos, para no tener una transacción abierta mientras se llama a otros
 * procesos (D-83):
 *
 * <ol>
 *   <li>con la generación bloqueada, se marca EN_CURSO, o no se hace nada si ya está GENERADA;
 *   <li>fuera de una transacción: el contenido de Generación, los nombres de los firmantes, el PDF
 *       y el archivo, nombrado por su SHA-256;
 *   <li>con la generación bloqueada otra vez, el documento, la entrada de la bitácora y GENERADA.
 *       Si otra generación llegó antes, esta no registra nada (SC-005).
 * </ol>
 *
 * <p>Un fallo se registra con su motivo (FALLIDA) y no se relanza al preparar: así el reenvío de
 * publicaciones no repite sin fin una generación que falla por un dato (research §4).
 */
public final class DocumentoFirmableServicio implements PrepararDocumentoFirmable {

    /** Quien genera cuando nadie lo pidió: la generación automática al entrar en firma. */
    public static final String SISTEMA = "sistema";

    private static final Logger LOG = LoggerFactory.getLogger(DocumentoFirmableServicio.class);

    private final RepositorioDocumentos documentos;
    private final ContenidoDelDistributivo contenido;
    private final FirmantesDelPeriodo firmantes;
    private final GeneradorDePdf generador;
    private final AlmacenDeDocumentos almacen;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * @param documentos las generaciones y los documentos
     * @param contenido el contenido de la versión congelada, de Generación
     * @param firmantes los nombres de los firmantes, de Datos y de Roles
     * @param generador el PDF/A
     * @param almacen los archivos, fuera de la base
     * @param registro la bitácora
     * @param transaccion la transacción de Firma
     * @param reloj el momento
     */
    public DocumentoFirmableServicio(
            RepositorioDocumentos documentos,
            ContenidoDelDistributivo contenido,
            FirmantesDelPeriodo firmantes,
            GeneradorDePdf generador,
            AlmacenDeDocumentos almacen,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.documentos = documentos;
        this.contenido = contenido;
        this.firmantes = firmantes;
        this.generador = generador;
        this.almacen = almacen;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void preparar(SolicitudDeDocumento solicitud) {
        List<PasoDeFirma> cadena =
                solicitud.cadena().stream()
                        .map(paso -> new PasoDeFirma(paso.orden(), paso.etiqueta(), paso.rol()))
                        .toList();
        Optional<GeneracionDeDocumento> empezada =
                transaccion.ejecutar(
                        () -> {
                            Optional<GeneracionDeDocumento> guardada =
                                    documentos.bloquear(solicitud.periodo(), solicitud.version());
                            GeneracionDeDocumento generacion;
                            if (guardada.isEmpty()) {
                                generacion =
                                        GeneracionDeDocumento.nueva(
                                                solicitud.periodo(),
                                                solicitud.version(),
                                                solicitud.hash(),
                                                cadena,
                                                solicitud.rolDeLaEtapa(),
                                                SISTEMA,
                                                reloj.instant());
                            } else {
                                generacion = guardada.get();
                                if (!generacion.iniciar(SISTEMA, reloj.instant())) {
                                    return Optional.empty();
                                }
                            }
                            documentos.guardar(generacion);
                            return Optional.of(generacion);
                        });
        empezada.ifPresent(generacion -> generar(generacion, SISTEMA));
    }

    /**
     * Genera y registra el documento de una generación EN_CURSO, ya confirmada.
     *
     * @return el motivo, si falló
     */
    Optional<String> generar(GeneracionDeDocumento generacion, String quien) {
        byte[] pdf;
        String sha256;
        String archivo;
        try {
            pdf = generador.generar(contenidoConFirmantes(generacion));
            sha256 = sha256(pdf);
            archivo = almacen.guardar(pdf, sha256);
        } catch (RuntimeException fallo) {
            String motivo = motivoDe(fallo);
            LOG.warn(
                    "No se generó el documento firmable de {} versión {}: {}",
                    generacion.periodo(),
                    generacion.version(),
                    motivo,
                    fallo);
            transaccion.ejecutar(
                    () -> {
                        documentos
                                .bloquear(generacion.periodo(), generacion.version())
                                .filter(una -> una.estado() != EstadoDeGeneracion.GENERADA)
                                .ifPresent(
                                        una -> {
                                            una.fallar(motivo, reloj.instant());
                                            documentos.guardar(una);
                                        });
                        return motivo;
                    });
            return Optional.of(motivo);
        }
        transaccion.ejecutar(
                () -> {
                    GeneracionDeDocumento bloqueada =
                            documentos
                                    .bloquear(generacion.periodo(), generacion.version())
                                    .orElseThrow();
                    if (bloqueada.estado() == EstadoDeGeneracion.GENERADA) {
                        return bloqueada;
                    }
                    Instant ahora = reloj.instant();
                    DocumentoFirmable documento =
                            new DocumentoFirmable(
                                    UUID.randomUUID(),
                                    generacion.periodo(),
                                    generacion.version(),
                                    DocumentoFirmable.CONSOLIDADO,
                                    generacion.hashContenido(),
                                    sha256,
                                    archivo,
                                    pdf.length,
                                    quien,
                                    ahora);
                    documentos.registrar(documento);
                    bloqueada.completar(ahora);
                    documentos.guardar(bloqueada);
                    registro.registrar(cambio(documento));
                    return bloqueada;
                });
        return Optional.empty();
    }

    // El contenido de Generación con los firmantes de la cadena. Si la versión o el hash no son los
    // que Aprobación congeló, no se genera.
    private ContenidoDelDocumento contenidoConFirmantes(GeneracionDeDocumento generacion) {
        ContenidoDelDocumento leido = contenido.contenido(generacion.periodo());
        if (leido.version() != generacion.version()
                || !leido.hashContenido().equals(generacion.hashContenido())) {
            throw new ContenidoDeOtraVersion(generacion.version());
        }
        Map<String, String> nombres =
                firmantes.nombres(
                        generacion.periodo(),
                        generacion.cadena().stream()
                                .map(PasoDeFirma::rol)
                                .collect(Collectors.toSet()));
        List<Firmante> bloque = Firmante.de(generacion.cadena(), nombres);
        return new ContenidoDelDocumento(
                leido.periodo(),
                leido.version(),
                leido.hashContenido(),
                leido.carreras(),
                leido.totales(),
                leido.docentes(),
                bloque);
    }

    // El motivo que ve la pantalla: el de las reglas, tal cual; el de lo inesperado, sin detalles
    // internos (principio IX), que quedan en el registro del proceso.
    private static String motivoDe(RuntimeException fallo) {
        if (fallo instanceof FirmanteSinNombre || fallo instanceof ContenidoDeOtraVersion) {
            return fallo.getMessage();
        }
        return "No se pudo generar el PDF. Vuelve a intentarlo; si sigue fallando, revisa el"
                + " registro del proceso.";
    }

    private static CambioDeFirma cambio(DocumentoFirmable documento) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", documento.periodo());
        detalle.put("version", documento.version());
        detalle.put("hashContenido", documento.hashContenido());
        detalle.put("sha256", documento.sha256Original());
        return new CambioDeFirma(
                documento.generadoEn(),
                documento.generadoPor(),
                CambioDeFirma.DOCUMENTO_FIRMABLE_GENERADO,
                detalle,
                Map.of("estado", "GENERADO"));
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("La JVM no tiene SHA-256", error);
        }
    }

    /** El contenido que dio Generación no es el de la versión que se congeló. */
    static final class ContenidoDeOtraVersion extends RuntimeException {

        private static final long serialVersionUID = 1L;

        ContenidoDeOtraVersion(int version) {
            super(
                    "El contenido de Generación no es el de la versión "
                            + version
                            + " que se congeló.");
        }
    }
}
