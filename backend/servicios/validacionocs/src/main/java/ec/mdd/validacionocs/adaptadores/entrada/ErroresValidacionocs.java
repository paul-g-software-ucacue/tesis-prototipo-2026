package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.ConsolidadoDesactualizado;
import ec.mdd.validacionocs.dominio.EtapaConOperacionPropia;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteEnvio;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteRevision;
import ec.mdd.validacionocs.dominio.InconsistenciasAbiertas;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.dominio.ParcialYaEnviado;
import ec.mdd.validacionocs.dominio.ParcialesPendientes;
import ec.mdd.validacionocs.dominio.SinConsolidar;
import ec.mdd.validacionocs.dominio.SinInforme;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Los errores de validacionocs (principio XIII). */
@Component
class ErroresValidacionocs implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.conDetalles(
                        ConfiguracionInvalida.class,
                        new DefinicionDeCodigo(
                                "VALIDACIONOCS_CONFIGURACION_INVALIDA",
                                400,
                                "Configuración con problemas",
                                "Corrige lo que se indica y vuelve a cargar el archivo."),
                        error ->
                                error.problemas().stream()
                                        .map(uno -> new DetalleRespuesta("archivo", uno))
                                        .toList()),
                EntradaCatalogo.conDetalles(
                        ArchivoIlegible.class,
                        new DefinicionDeCodigo(
                                "VALIDACIONOCS_ARCHIVO_ILEGIBLE",
                                400,
                                "Archivo no válido",
                                "No se pudo leer el archivo. Parte de la plantilla y vuelve a"
                                        + " cargarlo."),
                        error ->
                                error.linea().stream()
                                        .mapToObj(
                                                linea ->
                                                        new DetalleRespuesta(
                                                                "archivo",
                                                                "Revisa la línea " + linea + "."))
                                        .toList()),
                EntradaCatalogo.de(
                        ArchivoDemasiadoGrande.class,
                        "VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE",
                        413,
                        "Archivo demasiado grande",
                        "El archivo pasa de 1 MB. Parte de la plantilla y vuelve a cargarlo."),
                EntradaCatalogo.de(
                        PeriodoSinPlanificacion.class,
                        "VALIDACIONOCS_PERIODO_SIN_PLANIFICACION",
                        409,
                        "Periodo en curso",
                        "La configuración solo cambia mientras el periodo está en Planificación."),
                EntradaCatalogo.de(
                        PeriodoNoEncontrado.class,
                        "VALIDACIONOCS_PERIODO_NO_ENCONTRADO",
                        404,
                        "Periodo no encontrado",
                        "El periodo no está en los ciclos académicos. Elige otro."),
                // El flujo del distributivo (F12-H02).
                EntradaCatalogo.de(
                        SinDistributivo.class,
                        "VALIDACIONOCS_SIN_DISTRIBUTIVO",
                        404,
                        "Sin distributivo",
                        "El periodo todavía no tiene distributivo. Créalo en «Preparación del"
                                + " periodo»."),
                EntradaCatalogo.de(
                        EtapaNoAdmiteEnvio.class,
                        "VALIDACIONOCS_ETAPA_NO_ADMITE_ENVIO",
                        409,
                        "Etapa sin envíos",
                        "El distributivo ya pasó de la propuesta de carrera: los parciales ya no"
                                + " se envían."),
                EntradaCatalogo.de(
                        ParcialYaEnviado.class,
                        "VALIDACIONOCS_PARCIAL_YA_ENVIADO",
                        409,
                        "Parcial ya enviado",
                        "Este parcial ya se envió."),
                EntradaCatalogo.de(
                        EtapaConOperacionPropia.class,
                        "VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA",
                        409,
                        "Etapa con su propia acción",
                        "Esta etapa no se avanza así: tiene su propia acción."),
                EntradaCatalogo.de(
                        ParcialNoEncontrado.class,
                        "VALIDACIONOCS_PARCIAL_NO_ENCONTRADO",
                        404,
                        "Parcial no encontrado",
                        "Esa carrera no tiene parcial en el distributivo del periodo."),
                EntradaCatalogo.de(
                        PeriodoCerrado.class,
                        "VALIDACIONOCS_PERIODO_CERRADO",
                        409,
                        "Periodo cerrado",
                        "El periodo está cerrado: el distributivo ya no cambia de etapa."),
                // El envío a revisión (F10-H03, D-97), en el orden en que se comprueban.
                EntradaCatalogo.de(
                        EtapaNoAdmiteRevision.class,
                        "VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION",
                        409,
                        "Etapa sin envío a revisión",
                        "Solo se envía a revisión desde la consolidación."),
                EntradaCatalogo.de(
                        ParcialesPendientes.class,
                        "VALIDACIONOCS_PARCIALES_PENDIENTES",
                        409,
                        "Parciales sin enviar",
                        "Hay parciales sin enviar: su carrera tiene que enviarlos antes de la"
                                + " revisión."),
                EntradaCatalogo.de(
                        SinConsolidar.class,
                        "VALIDACIONOCS_SIN_CONSOLIDAR",
                        409,
                        "Sin consolidar",
                        "Consolida el periodo antes de enviarlo a revisión."),
                EntradaCatalogo.de(
                        ConsolidadoDesactualizado.class,
                        "VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO",
                        409,
                        "Consolidado desactualizado",
                        "Algo cambió después de consolidar. Consolida otra vez antes de enviarlo"
                                + " a revisión."),
                EntradaCatalogo.de(
                        SinInforme.class,
                        "VALIDACIONOCS_SIN_INFORME",
                        409,
                        "Sin informe sustentado",
                        "Adjunta el informe sustentado antes de enviarlo a revisión."),
                EntradaCatalogo.conDetalles(
                        InconsistenciasAbiertas.class,
                        new DefinicionDeCodigo(
                                "VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS",
                                409,
                                "Inconsistencias abiertas",
                                "Quedan inconsistencias abiertas. Corrige las duras y justifica las"
                                        + " blandas en el panel de inconsistencias."),
                        error ->
                                List.of(
                                        new DetalleRespuesta(
                                                "inconsistencias", cuantasQuedan(error)))));
    }

    // «Quedan 3: 2 duras y 1 blanda sin justificar.», con el singular cuando es una.
    private static String cuantasQuedan(InconsistenciasAbiertas error) {
        return (error.total() == 1 ? "Queda " : "Quedan ")
                + error.total()
                + ": "
                + error.duras()
                + (error.duras() == 1 ? " dura y " : " duras y ")
                + error.blandas()
                + (error.blandas() == 1 ? " blanda" : " blandas")
                + " sin justificar.";
    }
}
