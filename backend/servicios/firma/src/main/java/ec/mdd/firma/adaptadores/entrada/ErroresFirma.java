package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.GeneracionFallida;
import ec.mdd.firma.dominio.ObservacionInvalida;
import ec.mdd.firma.dominio.PasoSinNombre;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de negocio de Firma, con los códigos de {@code contratos/comun/errores.md} (principio
 * XIII, D-72, F13-H02). Sin el rol de la etapa, el 403 común lo da el controlador; los fallos entre
 * procesos, {@code ErroresEntreProcesos}.
 *
 * <p>Un archivo firmado rechazado (F14-H02) tiene un código por su primera comprobación rechazada,
 * y lleva las cuatro comprobaciones en los detalles: {@code campo} es la comprobación y {@code
 * descripcion} su resultado, con el motivo del sistema (D-103).
 */
@Component
class ErroresFirma implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        EtapaNoAdmiteGeneracion.class,
                        "FIRMA_ETAPA_NO_ADMITE_GENERACION",
                        409,
                        "Etapa sin documento firmable",
                        "El documento firmable se genera cuando el distributivo está en firma."),
                EntradaCatalogo.conDetalles(
                        GeneracionFallida.class,
                        new DefinicionDeCodigo(
                                "FIRMA_GENERACION_FALLIDA",
                                409,
                                "El PDF no se pudo generar",
                                "El documento firmable no se pudo generar. Corrige lo que dice el"
                                        + " detalle y vuelve a intentarlo."),
                        error -> List.of(new DetalleRespuesta("motivo", error.motivo()))),
                EntradaCatalogo.de(
                        ArchivoNoDisponible.class,
                        "FIRMA_ARCHIVO_NO_DISPONIBLE",
                        500,
                        "Archivo no disponible",
                        "El archivo del documento firmable no está disponible. Avisa al"
                                + " administrador con el identificador del incidente."),
                rechazo(
                        ArchivoFirmadoRechazado.NoEsLaVersionVigente.class,
                        "FIRMA_NO_ES_LA_VERSION_VIGENTE",
                        "No es la versión vigente",
                        "El PDF no es la versión vigente del documento. Descarga el PDF de"
                                + " «Pendientes de firma», fírmalo y súbelo sin cambiarlo."),
                rechazo(
                        ArchivoFirmadoRechazado.SinUnaFirmaNueva.class,
                        "FIRMA_SIN_UNA_FIRMA_NUEVA",
                        "Firma nueva no encontrada",
                        "El PDF debe tener exactamente una firma nueva, la tuya. Firma una sola vez"
                                + " el PDF descargado y súbelo."),
                rechazo(
                        ArchivoFirmadoRechazado.NoIntegra.class,
                        "FIRMA_NO_INTEGRA",
                        "Firma no íntegra",
                        "La firma nueva no corresponde al contenido del PDF: el archivo cambió"
                                + " después de firmarlo. Vuelve a firmar el PDF descargado."),
                rechazo(
                        ArchivoFirmadoRechazado.SinConfianza.class,
                        "FIRMA_SIN_CONFIANZA",
                        "Certificado no reconocido",
                        "El certificado de la firma no es de una entidad de certificación de"
                                + " confianza. Firma con un certificado de una entidad acreditada."),
                rechazo(
                        ArchivoFirmadoRechazado.NoVigente.class,
                        "FIRMA_CERTIFICADO_NO_VIGENTE",
                        "Certificado no vigente",
                        "El certificado de la firma no está vigente. Firma con un certificado"
                                + " vigente y vuelve a subir el PDF."),
                rechazo(
                        ArchivoFirmadoRechazado.FueraDeOrden.class,
                        "FIRMA_FUERA_DE_ORDEN",
                        "Firma fuera de orden",
                        "La firma es de la persona de un paso posterior. Primero firma la persona"
                                + " del paso en turno."),
                rechazo(
                        ArchivoFirmadoRechazado.FirmanteEquivocado.class,
                        "FIRMA_FIRMANTE_EQUIVOCADO",
                        "Firmante equivocado",
                        "El certificado no es de la persona que el documento indica para este paso."
                                + " Firma con tu propio certificado."),
                EntradaCatalogo.de(
                        PasoSinNombre.class,
                        "FIRMA_PASO_SIN_NOMBRE",
                        409,
                        "Paso sin firmante registrado",
                        "El documento no tiene registrado el nombre de quien firma este paso. Avisa"
                                + " al Responsable de consolidación para generarlo otra vez."),
                EntradaCatalogo.de(
                        ObservacionInvalida.class,
                        "FIRMA_OBSERVACION_INVALIDA",
                        400,
                        "Observación no válida",
                        "Para rechazar en firma escribe una observación de 10 a 500 caracteres."),
                EntradaCatalogo.de(
                        ArchivoVacio.class,
                        "FIRMA_ARCHIVO_VACIO",
                        400,
                        "Archivo vacío",
                        "El archivo está vacío. Sube el PDF firmado."),
                EntradaCatalogo.de(
                        ArchivoDemasiadoGrande.class,
                        "FIRMA_ARCHIVO_DEMASIADO_GRANDE",
                        413,
                        "Archivo demasiado grande",
                        "El archivo pasa de 10 MB, el tamaño máximo de un PDF firmado."));
    }

    private static EntradaCatalogo rechazo(
            Class<? extends ArchivoFirmadoRechazado> excepcion,
            String codigo,
            String titulo,
            String descripcion) {
        return EntradaCatalogo.conDetalles(
                excepcion,
                new DefinicionDeCodigo(codigo, 422, titulo, descripcion),
                error ->
                        error.resultado().comprobaciones().stream()
                                .map(ErroresFirma::detalle)
                                .toList());
    }

    private static DetalleRespuesta detalle(ResultadoDeComprobacion una) {
        String resultado =
                switch (una.resultado()) {
                    case APROBADA -> "Aprobada.";
                    case RECHAZADA -> "Rechazada. " + una.motivo().orElseThrow();
                    case NO_EVALUADA -> "No evaluada. " + una.motivo().orElseThrow();
                };
        return new DetalleRespuesta(una.comprobacion().name(), resultado);
    }
}
