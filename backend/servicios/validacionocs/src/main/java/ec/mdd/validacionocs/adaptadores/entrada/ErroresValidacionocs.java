package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
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
                        "El periodo no está en los ciclos académicos. Elige otro."));
    }
}
