package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.ArchivoDelDocumento;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.FirmasDelDocumento.PasoEnTurno;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes;
import ec.mdd.firma.puertos.entrada.DescargarArchivoVigente;
import ec.mdd.firma.puertos.entrada.FirmaPendiente;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Las firmas pendientes de cada usuario y la descarga del archivo vigente (F14-H01, D-102).
 *
 * <p>Está en turno el firmante esperado del primer paso sin firma, fijado al generar el documento:
 * el Responsable designado, por su usuario, o el docente de un cargo, por la identificación de la
 * cuenta. El rol del token no cuenta. La identificación se pide a Usuarios solo si algún paso en
 * turno la necesita, y una sola vez por pedido; fuera de transacciones (D-83).
 *
 * <p>Antes de entregar el archivo se recalcula su SHA-256: un archivo que falta o que no es el
 * registrado no se entrega (FR-009). A quien no está en turno, y por un documento que no existe o
 * no está generado, el mismo {@link SinTurnoDeFirma}: el 403 no dice cuál es (FR-010).
 */
public final class FirmasPendientesServicio
        implements ConsultarFirmasPendientes, DescargarArchivoVigente {

    private static final Logger LOG = LoggerFactory.getLogger(FirmasPendientesServicio.class);

    private final RepositorioDocumentos documentos;
    private final AlmacenDeDocumentos almacen;
    private final IdentificacionDeUsuarios identificaciones;

    /**
     * @param documentos los documentos generados con su cadena y sus firmantes
     * @param almacen los archivos
     * @param identificaciones la identificación de las cuentas, de Usuarios
     */
    public FirmasPendientesServicio(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones) {
        this.documentos = documentos;
        this.almacen = almacen;
        this.identificaciones = identificaciones;
    }

    @Override
    public List<FirmaPendiente> pendientes(Solicitante quien) {
        Supplier<Optional<String>> identificacion = unaVez(quien.usuario());
        List<FirmaPendiente> pendientes = new ArrayList<>();
        for (FirmasDelDocumento firmas : documentos.generados()) {
            firmas.pasoEnTurno()
                    .filter(turno -> reconoce(turno, quien, identificacion))
                    .ifPresent(
                            turno ->
                                    pendientes.add(
                                            new FirmaPendiente(
                                                    firmas.documento().id(),
                                                    firmas.documento().periodo(),
                                                    firmas.documento().version(),
                                                    turno.paso().orden(),
                                                    turno.rotulo(),
                                                    firmas.documento().generadoEn())));
        }
        return List.copyOf(pendientes);
    }

    @Override
    public ArchivoVigente archivo(UUID documento, Solicitante quien) {
        FirmasDelDocumento firmas = documentos.firmas(documento).orElseThrow(SinTurnoDeFirma::new);
        Supplier<Optional<String>> identificacion = unaVez(quien.usuario());
        firmas.pasoEnTurno()
                .filter(turno -> reconoce(turno, quien, identificacion))
                .orElseThrow(SinTurnoDeFirma::new);
        ArchivoDelDocumento vigente = firmas.archivoVigente();
        byte[] contenido = leer(vigente);
        return new ArchivoVigente(
                "distributivo-"
                        + firmas.documento().periodo()
                        + "-v"
                        + firmas.documento().version()
                        + ".pdf",
                vigente.sha256(),
                contenido);
    }

    private static boolean reconoce(
            PasoEnTurno turno, Solicitante quien, Supplier<Optional<String>> identificacion) {
        return turno.firmante()
                .reconoce(
                        quien.usuario(),
                        turno.firmante().porIdentificacion()
                                ? identificacion.get()
                                : Optional.empty());
    }

    // La identificación de la cuenta, pedida a Usuarios la primera vez que hace falta.
    private Supplier<Optional<String>> unaVez(String usuario) {
        return new Supplier<>() {
            private Optional<String> leida;

            @Override
            public Optional<String> get() {
                if (leida == null) {
                    leida = identificaciones.de(usuario);
                }
                return leida;
            }
        };
    }

    // El archivo, solo si su SHA-256 es el registrado.
    private byte[] leer(ArchivoDelDocumento vigente) {
        byte[] contenido;
        try {
            contenido = almacen.leer(vigente.nombre());
        } catch (UncheckedIOException | IllegalArgumentException error) {
            LOG.error("No se pudo leer el archivo vigente {}", vigente.nombre(), error);
            throw new ArchivoNoDisponible("No se pudo leer " + vigente.nombre());
        }
        if (!sha256(contenido).equals(vigente.sha256())) {
            LOG.error("El archivo vigente {} no tiene su SHA-256 registrado", vigente.nombre());
            throw new ArchivoNoDisponible(vigente.nombre() + " no coincide con su SHA-256");
        }
        return contenido;
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("La JVM no tiene SHA-256", error);
        }
    }
}
