package ec.mdd.firma.aplicacion;

import ec.mdd.firma.aplicacion.FirmanteEnTurno.DocumentoEnTurno;
import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.ComprobacionDeDocumento;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.Resultado;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas.HallazgosDeLaFirma;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * La subida del PDF firmado y su validación básica (F14-H02, RF-39, D-23, D-103).
 *
 * <ol>
 *   <li><b>El turno</b>, primero: a quien no es el firmante en turno, y por un documento que no
 *       existe o no está generado, {@code SinTurnoDeFirma}, sin mirar el archivo ni dejar rastro.
 *       Así el 403 no dice nada del documento (FR-002).
 *   <li><b>El archivo</b>: vacío o de más de 10 MB se rechaza sin resultado.
 *   <li><b>La validación</b>, fuera de transacciones, como la consulta a Usuarios (D-83): el mismo
 *       documento lo decide el dominio contra el vigente verificado por su SHA-256; si pasa, el
 *       validador revisa la firma nueva.
 *   <li><b>El registro</b>, en una transacción con su cambio para la bitácora. Si se aceptó, el
 *       archivo se guarda antes por su SHA-256 y el repositorio comprueba, con la generación
 *       bloqueada, que nadie firmó el paso entretanto; si alguien lo hizo, la subida se registra
 *       como rechazada por no ser la versión vigente (FR-014). Si la transacción falla después de
 *       guardar el archivo, queda un archivo sin registro, que no es vigente ni se entrega
 *       (research §4).
 * </ol>
 */
public final class SubirFirmaServicio implements SubirFirma {

    private final FirmanteEnTurno enTurno;
    private final RepositorioDocumentos documentos;
    private final AlmacenDeDocumentos almacen;
    private final ValidadorDeFirmas validador;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /**
     * @param documentos los documentos con sus firmas, y el registro de las validaciones
     * @param almacen los archivos
     * @param identificaciones la identificación de las cuentas, de Usuarios
     * @param validador la revisión de la firma nueva
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción del registro
     * @param reloj el momento de la subida
     */
    public SubirFirmaServicio(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones,
            ValidadorDeFirmas validador,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.enTurno = new FirmanteEnTurno(documentos, almacen, identificaciones);
        this.documentos = documentos;
        this.almacen = almacen;
        this.validador = validador;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ResultadoValidacion subir(UUID documento, byte[] archivo, Solicitante quien) {
        DocumentoEnTurno turno = enTurno.documentoEnTurno(documento, quien);
        if (archivo.length == 0) {
            throw new ArchivoVacio();
        }
        if (archivo.length > ArchivoDemasiadoGrande.LIMITE_EN_BYTES) {
            throw new ArchivoDemasiadoGrande(archivo.length);
        }
        FirmasDelDocumento firmas = turno.firmas();
        byte[] vigente = enTurno.leerVerificado(firmas.archivoVigente());
        ResultadoValidacion resultado = validar(documento, turno, vigente, archivo, quien);
        if (resultado.aceptada()) {
            almacen.guardar(archivo, resultado.sha256Subido());
        }
        ResultadoValidacion registrado =
                transaccion.ejecutar(
                        () -> {
                            ResultadoValidacion aRegistrar = resultado;
                            if (!documentos.registrarValidacion(aRegistrar, firmas)) {
                                aRegistrar = resultado.ganadaPorOtraSubida();
                                documentos.registrarValidacion(aRegistrar, firmas);
                            }
                            registro.registrar(cambio(aRegistrar, firmas.documento()));
                            return aRegistrar;
                        });
        if (!registrado.aceptada()) {
            throw ArchivoFirmadoRechazado.de(registrado);
        }
        return registrado;
    }

    private ResultadoValidacion validar(
            UUID documento,
            DocumentoEnTurno turno,
            byte[] vigente,
            byte[] archivo,
            Solicitante quien) {
        ResultadoDeComprobacion mismo = ComprobacionDeDocumento.mismoDocumento(vigente, archivo);
        List<ResultadoDeComprobacion> evaluadas = new ArrayList<>(List.of(mismo));
        Optional<CertificadoDeLaFirma> certificado = Optional.empty();
        if (mismo.resultado() == Resultado.APROBADA) {
            HallazgosDeLaFirma hallazgos = validador.validar(vigente, archivo);
            evaluadas.addAll(hallazgos.evaluadas());
            certificado = hallazgos.certificado();
        }
        return new ResultadoValidacion(
                UUID.randomUUID(),
                documento,
                turno.turno().paso().orden(),
                quien.usuario(),
                reloj.instant(),
                FirmanteEnTurno.sha256(archivo),
                archivo.length,
                ResultadoDeComprobacion.desde(evaluadas.toArray(ResultadoDeComprobacion[]::new)),
                certificado);
    }

    // El cambio para la bitácora: el documento, el paso, el archivo y el resultado de cada
    // comprobación (FR-017, research §9).
    private static CambioDeFirma cambio(
            ResultadoValidacion resultado, DocumentoFirmable documento) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", documento.periodo());
        detalle.put("version", documento.version());
        detalle.put("orden", resultado.orden());
        detalle.put("sha256Subido", resultado.sha256Subido());
        Map<String, String> comprobaciones = new LinkedHashMap<>();
        resultado
                .comprobaciones()
                .forEach(
                        una ->
                                comprobaciones.put(
                                        una.comprobacion().name(), una.resultado().name()));
        detalle.put("comprobaciones", comprobaciones);
        resultado
                .certificado()
                .ifPresent(
                        certificado -> {
                            detalle.put("certificado", certificado.nombre());
                            detalle.put("serie", certificado.serie());
                        });
        return resultado.aceptada()
                ? new CambioDeFirma(
                        resultado.subidoEn(),
                        resultado.subidoPor(),
                        CambioDeFirma.FIRMA_ACEPTADA,
                        detalle,
                        Map.of("sha256Vigente", resultado.sha256Subido()))
                : new CambioDeFirma(
                        resultado.subidoEn(),
                        resultado.subidoPor(),
                        CambioDeFirma.FIRMA_RECHAZADA,
                        detalle,
                        Map.of("rechazo", resultado.primerRechazo().orElseThrow().name()));
    }
}
