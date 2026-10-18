// Los mensajes genéricos de la SPA (ADR-019): los mismos textos de contratos/comun/errores.md para
// una respuesta sin el modelo común (COMUN_ERROR_INTERNO), para un fallo de red
// (COMUN_SERVICIO_NO_DISPONIBLE) y para una pantalla sin permiso (SEGURIDAD_ACCESO_NO_AUTORIZADO,
// ADR-021). Una prueba los compara con el catálogo.

/** Título y descripción de un mensaje para el usuario. */
export interface TextoDeError {
  titulo: string;
  descripcion: string;
}

export const MENSAJE_ERROR_INTERNO: TextoDeError = {
  titulo: 'Error inesperado',
  descripcion:
    'Ocurrió un error inesperado. Intenta de nuevo en unos minutos; si el problema sigue, informa el identificador del incidente.',
};

export const MENSAJE_SERVICIO_NO_DISPONIBLE: TextoDeError = {
  titulo: 'Servicio no disponible',
  descripcion: 'El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.',
};

/** El aviso cuando la sesión se cerró por un 401 o porque venció el token (D-50). */
export const MENSAJE_SESION_TERMINADA: TextoDeError = {
  titulo: 'Sesión terminada',
  descripcion: 'Tu sesión terminó. Inicia sesión de nuevo.',
};

/** Una pantalla que los permisos del usuario no dejan ver (ADR-021), como el 403 de la API. */
export const MENSAJE_ACCESO_NO_AUTORIZADO: TextoDeError = {
  titulo: 'Acceso no autorizado',
  descripcion:
    'No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.',
};
