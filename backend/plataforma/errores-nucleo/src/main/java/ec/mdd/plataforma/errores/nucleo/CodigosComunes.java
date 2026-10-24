package ec.mdd.plataforma.errores.nucleo;

import java.util.List;

/**
 * Códigos de la plataforma, comunes a todos los procesos y al API Gateway (ADR-016).
 *
 * <p>Sus textos son los de {@code contratos/comun/errores.md}; una prueba lo comprueba.
 */
public final class CodigosComunes {

    /** 500: error técnico inesperado, siempre con {@code idIncidente}. */
    public static final DefinicionDeCodigo ERROR_INTERNO =
            new DefinicionDeCodigo(
                    "COMUN_ERROR_INTERNO",
                    500,
                    "Error inesperado",
                    "Ocurrió un error inesperado. Intenta de nuevo en unos minutos; si el problema"
                            + " sigue, informa el identificador del incidente.");

    /** 400: uno o más campos no cumplen el contrato. */
    public static final DefinicionDeCodigo SOLICITUD_INVALIDA =
            new DefinicionDeCodigo(
                    "COMUN_SOLICITUD_INVALIDA",
                    400,
                    "Solicitud inválida",
                    "Revisa los campos marcados y vuelve a intentarlo.");

    /** 400: el cuerpo no es JSON válido o no tiene la forma esperada. */
    public static final DefinicionDeCodigo CUERPO_ILEGIBLE =
            new DefinicionDeCodigo(
                    "COMUN_CUERPO_ILEGIBLE",
                    400,
                    "Solicitud ilegible",
                    "El contenido enviado no tiene un formato válido. Revisa los datos y vuelve a"
                            + " intentarlo.");

    /** 404: la ruta no existe. */
    public static final DefinicionDeCodigo RECURSO_NO_ENCONTRADO =
            new DefinicionDeCodigo(
                    "COMUN_RECURSO_NO_ENCONTRADO",
                    404,
                    "Recurso no encontrado",
                    "La dirección solicitada no existe. Revisa la dirección y vuelve a intentarlo.");

    /** 405: la ruta existe, pero no con ese método. */
    public static final DefinicionDeCodigo METODO_NO_PERMITIDO =
            new DefinicionDeCodigo(
                    "COMUN_METODO_NO_PERMITIDO",
                    405,
                    "Operación no permitida",
                    "Esta dirección no admite la operación solicitada.");

    /** 406: el formato pedido no está disponible. */
    public static final DefinicionDeCodigo FORMATO_NO_ACEPTADO =
            new DefinicionDeCodigo(
                    "COMUN_FORMATO_NO_ACEPTADO",
                    406,
                    "Formato no disponible",
                    "La respuesta no está disponible en el formato solicitado.");

    /**
     * 413: el cuerpo supera el máximo que acepta el proceso, por ejemplo un archivo de carga de más
     * de 10 MB (F01-H02).
     */
    public static final DefinicionDeCodigo CONTENIDO_DEMASIADO_GRANDE =
            new DefinicionDeCodigo(
                    "COMUN_CONTENIDO_DEMASIADO_GRANDE",
                    413,
                    "Contenido demasiado grande",
                    "El contenido enviado supera el tamaño máximo que admite el sistema.");

    /** 415: el tipo de contenido no está soportado. */
    public static final DefinicionDeCodigo TIPO_NO_SOPORTADO =
            new DefinicionDeCodigo(
                    "COMUN_TIPO_NO_SOPORTADO",
                    415,
                    "Tipo de contenido no soportado",
                    "El contenido enviado está en un tipo que esta operación no acepta.");

    /** 503: el API Gateway no puede conectar con el proceso (D-40). */
    public static final DefinicionDeCodigo SERVICIO_NO_DISPONIBLE =
            new DefinicionDeCodigo(
                    "COMUN_SERVICIO_NO_DISPONIBLE",
                    503,
                    "Servicio no disponible",
                    "El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.");

    /** 400: un receptor de eventos recibe un tipo sin manejador (ADR-018). */
    public static final DefinicionDeCodigo EVENTO_NO_RECONOCIDO =
            new DefinicionDeCodigo(
                    "COMUN_EVENTO_NO_RECONOCIDO",
                    400,
                    "Evento no reconocido",
                    "El receptor no admite este tipo de evento.");

    /** 401: sin token o con un token inválido (ADR-014). */
    public static final DefinicionDeCodigo SEGURIDAD_NO_AUTENTICADO =
            new DefinicionDeCodigo(
                    "SEGURIDAD_NO_AUTENTICADO",
                    401,
                    "Sesión no válida",
                    "Inicia sesión de nuevo para continuar.");

    /**
     * 403: token válido sin el permiso de la operación, un token de usuario en {@code /interno/**}
     * o uno de proceso fuera de él. No dice qué faltó (ADR-021).
     */
    public static final DefinicionDeCodigo ACCESO_NO_AUTORIZADO =
            new DefinicionDeCodigo(
                    "SEGURIDAD_ACCESO_NO_AUTORIZADO",
                    403,
                    "Acceso no autorizado",
                    "No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al"
                            + " administrador que revise tus roles.");

    /** Todos los códigos de la plataforma, para verificarlos contra el catálogo documentado. */
    public static final List<DefinicionDeCodigo> TODOS =
            List.of(
                    ERROR_INTERNO,
                    SOLICITUD_INVALIDA,
                    CUERPO_ILEGIBLE,
                    RECURSO_NO_ENCONTRADO,
                    METODO_NO_PERMITIDO,
                    FORMATO_NO_ACEPTADO,
                    CONTENIDO_DEMASIADO_GRANDE,
                    TIPO_NO_SOPORTADO,
                    SERVICIO_NO_DISPONIBLE,
                    EVENTO_NO_RECONOCIDO,
                    SEGURIDAD_NO_AUTENTICADO,
                    ACCESO_NO_AUTORIZADO);

    private CodigosComunes() {}

    /**
     * El código común de un error que no tiene uno propio, según su estado (data-model,
     * «Correspondencia entre el estado y el código común»).
     */
    public static DefinicionDeCodigo segunEstado(int estado) {
        return switch (estado) {
            case 401 -> SEGURIDAD_NO_AUTENTICADO;
            case 403 -> ACCESO_NO_AUTORIZADO;
            case 404 -> RECURSO_NO_ENCONTRADO;
            case 405 -> METODO_NO_PERMITIDO;
            case 406 -> FORMATO_NO_ACEPTADO;
            case 413 -> CONTENIDO_DEMASIADO_GRANDE;
            case 415 -> TIPO_NO_SOPORTADO;
            case 503 -> SERVICIO_NO_DISPONIBLE;
            default -> estado >= 500 ? ERROR_INTERNO : SOLICITUD_INVALIDA;
        };
    }
}
