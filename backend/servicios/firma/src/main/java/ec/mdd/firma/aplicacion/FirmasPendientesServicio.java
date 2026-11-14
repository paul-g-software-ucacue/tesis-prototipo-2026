package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.ArchivoDelDocumento;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes;
import ec.mdd.firma.puertos.entrada.DescargarArchivoVigente;
import ec.mdd.firma.puertos.entrada.FirmaPendiente;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Las firmas pendientes de cada usuario y la descarga del archivo vigente (F14-H01, D-102).
 *
 * <p>Está en turno el firmante esperado del primer paso sin firma, fijado al generar el documento;
 * la regla y la identificación, pedida una sola vez por pedido, están en {@link FirmanteEnTurno}.
 * El paso en turno y el archivo vigente siguen a las firmas registradas (F14-H02).
 *
 * <p>Antes de entregar el archivo se recalcula su SHA-256: un archivo que falta o que no es el
 * registrado no se entrega (FR-009). A quien no está en turno, y por un documento que no existe o
 * no está generado, el mismo {@link SinTurnoDeFirma}: el 403 no dice cuál es (FR-010).
 */
public final class FirmasPendientesServicio
        implements ConsultarFirmasPendientes, DescargarArchivoVigente {

    private static final Logger LOG = LoggerFactory.getLogger(FirmasPendientesServicio.class);

    private final RepositorioDocumentos documentos;
    private final FirmanteEnTurno enTurno;

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
        this.enTurno = new FirmanteEnTurno(documentos, almacen, identificaciones);
    }

    @Override
    public List<FirmaPendiente> pendientes(Solicitante quien) {
        Supplier<Optional<String>> identificacion = enTurno.identificacion(quien.usuario());
        List<FirmaPendiente> pendientes = new ArrayList<>();
        for (FirmasDelDocumento firmas : documentos.generados()) {
            firmas.pasoEnTurno()
                    .filter(turno -> FirmanteEnTurno.reconoce(turno, quien, identificacion))
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
        FirmasDelDocumento firmas = enTurno.documentoEnTurno(documento, quien).firmas();
        ArchivoDelDocumento vigente = firmas.archivoVigente();
        byte[] contenido = enTurno.leerVerificado(vigente);
        return new ArchivoVigente(
                "distributivo-"
                        + firmas.documento().periodo()
                        + "-v"
                        + firmas.documento().version()
                        + ".pdf",
                vigente.sha256(),
                contenido);
    }
}
