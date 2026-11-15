package ec.mdd.firma.aplicacion;

import ec.mdd.firma.aplicacion.FirmanteEnTurno.DocumentoEnTurno;
import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.ComprobacionDeDocumento;
import ec.mdd.firma.dominio.ComprobacionDelFirmante;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.PasoSinNombre;
import ec.mdd.firma.dominio.Resultado;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.AvisoDeCadenaCompleta;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas.HallazgosDeLaFirma;
import java.time.Clock;
import java.time.Instant;
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
 *   <li><b>El nombre impreso del paso</b>: sin él no se puede comprobar al firmante (F14-H03); un
 *       documento anterior da {@code PasoSinNombre}, sin resultado.
 *   <li><b>El archivo</b>: vacío o de más de 10 MB se rechaza sin resultado.
 *   <li><b>La validación</b>, fuera de transacciones, como la consulta a Usuarios (D-83): el mismo
 *       documento lo decide el dominio contra el vigente verificado por su SHA-256; si pasa, el
 *       validador revisa la firma nueva; si es íntegra y de confianza, el dominio comprueba la
 *       vigencia al subir, el orden y el firmante (F14-H03, D-104).
 *   <li><b>El registro</b>, en una transacción con su cambio para la bitácora. Si se aceptó, el
 *       archivo se guarda antes por su SHA-256 y el repositorio comprueba, con la generación
 *       bloqueada, que nadie firmó el paso ni rechazó el documento entretanto (F14-H04); si pasó,
 *       la subida se registra como rechazada por no ser la versión vigente (FR-014). Si la
 *       transacción falla después de guardar el archivo, queda un archivo sin registro, que no es
 *       vigente ni se entrega (research §4). Con la firma del último paso, en la misma transacción,
 *       avisa que la cadena está completa; Aprobación decide qué pasa con el distributivo (F14-H03,
 *       MD-05).
 * </ol>
 */
public final class SubirFirmaServicio implements SubirFirma {

    private final FirmanteEnTurno enTurno;
    private final RepositorioDocumentos documentos;
    private final AlmacenDeDocumentos almacen;
    private final ValidadorDeFirmas validador;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final AvisoDeCadenaCompleta aviso;
    private final Clock reloj;

    /**
     * @param documentos los documentos con sus firmas, y el registro de las validaciones
     * @param almacen los archivos
     * @param identificaciones la identificación de las cuentas, de Usuarios
     * @param validador la revisión de la firma nueva
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción del registro
     * @param aviso el aviso de cadena completa, para Aprobación (F14-H03)
     * @param reloj el momento de la subida
     */
    public SubirFirmaServicio(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones,
            ValidadorDeFirmas validador,
            RegistroDeCambios registro,
            Transaccion transaccion,
            AvisoDeCadenaCompleta aviso,
            Clock reloj) {
        this.enTurno = new FirmanteEnTurno(documentos, almacen, identificaciones);
        this.documentos = documentos;
        this.almacen = almacen;
        this.validador = validador;
        this.registro = registro;
        this.transaccion = transaccion;
        this.aviso = aviso;
        this.reloj = reloj;
    }

    @Override
    public ResultadoValidacion subir(UUID documento, byte[] archivo, Solicitante quien) {
        DocumentoEnTurno turno = enTurno.documentoEnTurno(documento, quien);
        if (turno.turno().firmante().nombre().isEmpty()) {
            throw new PasoSinNombre();
        }
        if (archivo.length == 0) {
            throw new ArchivoVacio();
        }
        if (archivo.length > ArchivoDemasiadoGrande.LIMITE_EN_BYTES) {
            throw new ArchivoDemasiadoGrande(archivo.length);
        }
        FirmasDelDocumento firmas = turno.firmas();
        byte[] vigente = enTurno.leerVerificado(firmas.archivoVigente());
        ResultadoValidacion resultado =
                validar(documento, turno, vigente, archivo, quien, reloj.instant());
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
                            if (aRegistrar.aceptada()
                                    && esElUltimoPaso(firmas, aRegistrar.orden())) {
                                aviso.avisar(
                                        new CadenaDeFirmasCompleta(
                                                firmas.documento().periodo(),
                                                firmas.documento().version(),
                                                aRegistrar.subidoEn()));
                            }
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
            Solicitante quien,
            Instant ahora) {
        ResultadoDeComprobacion mismo = ComprobacionDeDocumento.mismoDocumento(vigente, archivo);
        List<ResultadoDeComprobacion> evaluadas = new ArrayList<>(List.of(mismo));
        Optional<CertificadoDeLaFirma> certificado = Optional.empty();
        if (mismo.resultado() == Resultado.APROBADA) {
            HallazgosDeLaFirma hallazgos = validador.validar(vigente, archivo);
            evaluadas.addAll(hallazgos.evaluadas());
            certificado = hallazgos.certificado();
        }
        // F14-H03 (D-104): la vigencia al subir, el orden y el firmante, si la firma nueva ya es
        // íntegra y de confianza.
        if (evaluadas.getLast().comprobacion() == Comprobacion.CONFIANZA
                && evaluadas.getLast().resultado() == Resultado.APROBADA) {
            evaluadas.addAll(
                    ComprobacionDelFirmante.evaluar(
                            certificado.orElseThrow(), ahora, turno.firmas()));
        }
        return new ResultadoValidacion(
                UUID.randomUUID(),
                documento,
                turno.turno().paso().orden(),
                quien.usuario(),
                ahora,
                FirmanteEnTurno.sha256(archivo),
                archivo.length,
                ResultadoDeComprobacion.desde(evaluadas.toArray(ResultadoDeComprobacion[]::new)),
                certificado);
    }

    // La cadena se completa con la firma del paso de mayor orden (F14-H03): el turno no se salta
    // pasos, así que los anteriores ya están firmados.
    private static boolean esElUltimoPaso(FirmasDelDocumento firmas, int orden) {
        return firmas.cadena().stream().mapToInt(PasoDeFirma::orden).max().orElse(0) == orden;
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
