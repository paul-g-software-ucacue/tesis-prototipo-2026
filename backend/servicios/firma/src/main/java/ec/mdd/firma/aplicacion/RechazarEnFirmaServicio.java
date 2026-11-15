package ec.mdd.firma.aplicacion;

import ec.mdd.firma.aplicacion.FirmanteEnTurno.DocumentoEnTurno;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.RechazarEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoRegistrado;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.AvisoDeRechazoEnFirma;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El rechazo en firma (F14-H04, RF-36, D-105).
 *
 * <ol>
 *   <li><b>El turno</b>, primero, con la regla de la subida ({@link FirmanteEnTurno}): a quien no
 *       es el firmante en turno, y por un documento que no existe, no está generado, ya se rechazó
 *       o tiene la cadena completa, {@code SinTurnoDeFirma}, sin mirar la observación. Así el 403
 *       no dice nada del documento.
 *   <li><b>La observación</b>, de 10 a 500 caracteres recortada ({@code RechazoDelFirmante}).
 *   <li><b>El registro</b>, en una transacción con su cambio para la bitácora y el aviso a
 *       Aprobación. El repositorio bloquea la generación y comprueba que nadie firmó ni rechazó
 *       entretanto; si alguien lo hizo, quien rechaza ya no está en turno: {@code SinTurnoDeFirma},
 *       sin cambio ni aviso. Aprobación decide a qué etapa vuelve el distributivo (MD-05).
 * </ol>
 */
public final class RechazarEnFirmaServicio implements RechazarEnFirma {

    private final FirmanteEnTurno enTurno;
    private final RepositorioDocumentos documentos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final AvisoDeRechazoEnFirma aviso;
    private final Clock reloj;

    /**
     * @param documentos los documentos con sus firmas, y el registro del rechazo
     * @param almacen los archivos, para la regla del turno compartida con la subida
     * @param identificaciones la identificación de las cuentas, de Usuarios
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción del registro
     * @param aviso el aviso de rechazo, para Aprobación
     * @param reloj el momento del rechazo
     */
    public RechazarEnFirmaServicio(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones,
            RegistroDeCambios registro,
            Transaccion transaccion,
            AvisoDeRechazoEnFirma aviso,
            Clock reloj) {
        this.enTurno = new FirmanteEnTurno(documentos, almacen, identificaciones);
        this.documentos = documentos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.aviso = aviso;
        this.reloj = reloj;
    }

    @Override
    public RechazoRegistrado rechazar(UUID documento, String observacion, Solicitante quien) {
        DocumentoEnTurno turno = enTurno.documentoEnTurno(documento, quien);
        RechazoDelFirmante rechazo =
                new RechazoDelFirmante(
                        turno.turno().paso().orden(),
                        quien.usuario(),
                        reloj.instant(),
                        observacion);
        FirmasDelDocumento firmas = turno.firmas();
        boolean registrado =
                transaccion.ejecutar(
                        () -> {
                            if (!documentos.registrarRechazo(rechazo, firmas)) {
                                return false;
                            }
                            registro.registrar(cambio(rechazo, firmas));
                            aviso.avisar(
                                    new RechazoEnFirma(
                                            firmas.documento().periodo(),
                                            firmas.documento().version(),
                                            rechazo.orden(),
                                            turno.turno().paso().rol(),
                                            rechazo.rechazadoPor(),
                                            rechazo.rechazadoEn(),
                                            rechazo.observacion()));
                            return true;
                        });
        if (!registrado) {
            throw new SinTurnoDeFirma();
        }
        return new RechazoRegistrado(
                documento,
                firmas.documento().periodo(),
                firmas.documento().version(),
                rechazo.orden(),
                turno.turno().rotulo(),
                rechazo.rechazadoPor(),
                rechazo.rechazadoEn(),
                rechazo.observacion());
    }

    // El cambio para la bitácora: el documento y el paso, y la observación como valor nuevo.
    private static CambioDeFirma cambio(RechazoDelFirmante rechazo, FirmasDelDocumento firmas) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", firmas.documento().periodo());
        detalle.put("version", firmas.documento().version());
        detalle.put("orden", rechazo.orden());
        return new CambioDeFirma(
                rechazo.rechazadoEn(),
                rechazo.rechazadoPor(),
                CambioDeFirma.RECHAZO_EN_FIRMA,
                detalle,
                Map.of("observacion", rechazo.observacion()));
    }
}
