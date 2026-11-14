package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.GeneracionFallida;
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
                                + " administrador con el identificador del incidente."));
    }
}
