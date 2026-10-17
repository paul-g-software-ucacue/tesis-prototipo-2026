// La API simulada de las pruebas en navegador (research §8): page.route sobre las dos operaciones
// de Usuarios que usa la SPA, con las respuestas de contracts/interfaz.md «Errores».
import type { Page, Route } from '@playwright/test';

export interface RespuestaSimulada {
  estado: number;
  cuerpo?: unknown;
  html?: string;
  sinConexion?: boolean;
}

export const RESPUESTAS = {
  tokenCorrecto: {
    estado: 200,
    cuerpo: { token: 'cabecera.cuerpo.firma', tipo: 'Bearer', venceEn: '2099-01-01T00:00:00Z' },
  },
  sesionActual: {
    estado: 200,
    cuerpo: { usuario: 'autor.prueba', roles: ['COORDINADOR_CARRERA', 'DOCENTE'] },
  },
  sesionDemo: {
    estado: 200,
    cuerpo: {
      usuario: 'docente.coordinador.demo',
      nombre: 'Docente y coordinador (demo)',
      roles: ['COORDINADOR_CARRERA', 'DOCENTE'],
    },
  },
  inicioBloqueado: {
    estado: 429,
    cuerpo: {
      titulo: 'Demasiados intentos',
      descripcion: 'Espera unos minutos y vuelve a intentarlo.',
      codigo: 'USUARIOS_INICIO_BLOQUEADO',
    },
  },
  credencialesInvalidas: {
    estado: 401,
    cuerpo: {
      titulo: 'Credenciales inválidas',
      descripcion: 'El usuario o la contraseña no son correctos.',
      codigo: 'USUARIOS_CREDENCIALES_INVALIDAS',
    },
  },
  conDetalles: {
    estado: 400,
    cuerpo: {
      titulo: 'Solicitud inválida',
      descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
      codigo: 'COMUN_SOLICITUD_INVALIDA',
      detalles: [{ campo: 'usuario', descripcion: 'es obligatorio' }],
    },
  },
  errorInterno: {
    estado: 500,
    cuerpo: {
      titulo: 'Error inesperado',
      descripcion:
        'Ocurrió un error inesperado. Intenta de nuevo en unos minutos; si el problema sigue, informa el identificador del incidente.',
      codigo: 'COMUN_ERROR_INTERNO',
      idIncidente: '6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
    },
  },
  servicioNoDisponible: {
    estado: 503,
    cuerpo: {
      titulo: 'Servicio no disponible',
      descripcion:
        'El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.',
      codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
    },
  },
  recursoNoEncontrado: {
    estado: 404,
    cuerpo: {
      titulo: 'Recurso no encontrado',
      descripcion: 'La dirección solicitada no existe. Revisa la dirección y vuelve a intentarlo.',
      codigo: 'COMUN_RECURSO_NO_ENCONTRADO',
    },
  },
  sesionNoValida: {
    estado: 401,
    cuerpo: {
      titulo: 'Sesión no válida',
      descripcion: 'Inicia sesión de nuevo para continuar.',
      codigo: 'SEGURIDAD_NO_AUTENTICADO',
    },
  },
  htmlDeUnProxy: { estado: 502, html: '<html><body><h1>502 Bad Gateway</h1>nginx</body></html>' },
  sinConexion: { estado: 0, sinConexion: true },
} satisfies Record<string, RespuestaSimulada>;

async function responder(ruta: Route, respuesta: RespuestaSimulada) {
  if (respuesta.sinConexion) {
    await ruta.abort('failed');
  } else if (respuesta.html) {
    await ruta.fulfill({
      status: respuesta.estado,
      contentType: 'text/html',
      body: respuesta.html,
    });
  } else {
    await ruta.fulfill({ status: respuesta.estado, json: respuesta.cuerpo });
  }
}

/** Simula el inicio de sesión (POST) y la sesión actual (GET) de Usuarios. */
export async function simularApi(
  pagina: Page,
  {
    inicio = RESPUESTAS.tokenCorrecto,
    sesion = RESPUESTAS.sesionActual,
  }: { inicio?: RespuestaSimulada; sesion?: RespuestaSimulada } = {},
) {
  await pagina.route('**/api/usuarios/sesion', (ruta) =>
    responder(ruta, ruta.request().method() === 'POST' ? inicio : sesion),
  );
}

/** Completa el formulario de inicio de sesión y lo envía. */
export async function ingresar(pagina: Page, usuario = 'autor.prueba', clave = 'clave-de-prueba') {
  if (usuario) await pagina.getByLabel('Usuario').fill(usuario);
  if (clave) await pagina.getByLabel('Contraseña').fill(clave);
  await pagina.getByRole('button', { name: 'Iniciar sesión' }).click();
}

/** Lo que nunca debe verse en pantalla (FR-010). */
export const TEXTO_TECNICO = [
  /\b[45]\d\d\b/,
  /COMUN_/,
  /USUARIOS_/,
  /SEGURIDAD_/,
  /Exception/,
  /Error:/,
  /Failed to fetch/,
  /NetworkError/,
  /Bad Gateway/,
];
