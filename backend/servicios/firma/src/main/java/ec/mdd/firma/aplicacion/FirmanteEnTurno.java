package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.ArchivoDelDocumento;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.FirmasDelDocumento.PasoEnTurno;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quién está en turno y el archivo vigente verificado, compartido por la lista de pendientes, la
 * descarga (F14-H01), la subida (F14-H02) y el rechazo en firma (F14-H04): la regla es una sola
 * (D-102). Un documento rechazado no tiene a nadie en turno (D-105).
 *
 * <p>Está en turno el firmante esperado del primer paso sin firma: el Responsable designado, por su
 * usuario, o el docente de un cargo, por la identificación de la cuenta. El rol del token no
 * cuenta. La identificación se pide a Usuarios solo si el paso la necesita, una sola vez por
 * pedido, y fuera de transacciones (D-83).
 *
 * <p>El archivo vigente se entrega solo si su SHA-256 es el registrado (FR-009 de F14-H01): uno que
 * falta o no coincide es {@link ArchivoNoDisponible}.
 */
final class FirmanteEnTurno {

    private static final Logger LOG = LoggerFactory.getLogger(FirmanteEnTurno.class);

    private final RepositorioDocumentos documentos;
    private final AlmacenDeDocumentos almacen;
    private final IdentificacionDeUsuarios identificaciones;

    FirmanteEnTurno(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones) {
        this.documentos = documentos;
        this.almacen = almacen;
        this.identificaciones = identificaciones;
    }

    /**
     * El documento y su paso en turno, si quien pide es su firmante esperado.
     *
     * @throws SinTurnoDeFirma si no lo es, o si el documento no existe o no está generado: el 403
     *     no dice cuál es
     */
    DocumentoEnTurno documentoEnTurno(UUID documento, Solicitante quien) {
        FirmasDelDocumento firmas = documentos.firmas(documento).orElseThrow(SinTurnoDeFirma::new);
        PasoEnTurno turno =
                firmas.pasoEnTurno()
                        .filter(uno -> reconoce(uno, quien, identificacion(quien.usuario())))
                        .orElseThrow(SinTurnoDeFirma::new);
        return new DocumentoEnTurno(firmas, turno);
    }

    /** La identificación de la cuenta, pedida a Usuarios la primera vez que hace falta. */
    Supplier<Optional<String>> identificacion(String usuario) {
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

    /** Si quien pide es el firmante esperado del paso; la identificación solo si el paso la usa. */
    static boolean reconoce(
            PasoEnTurno turno, Solicitante quien, Supplier<Optional<String>> identificacion) {
        return turno.firmante()
                .reconoce(
                        quien.usuario(),
                        turno.firmante().porIdentificacion()
                                ? identificacion.get()
                                : Optional.empty());
    }

    /**
     * El contenido del archivo, solo si su SHA-256 es el registrado.
     *
     * @throws ArchivoNoDisponible si falta o no coincide
     */
    byte[] leerVerificado(ArchivoDelDocumento vigente) {
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

    /** El SHA-256 en hexadecimal, en minúsculas. */
    static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("La JVM no tiene SHA-256", error);
        }
    }

    /**
     * Un documento con el paso en turno de quien pidió.
     *
     * @param firmas el documento, su cadena, sus firmantes y sus firmas
     * @param turno el paso en turno
     */
    record DocumentoEnTurno(FirmasDelDocumento firmas, PasoEnTurno turno) {}
}
