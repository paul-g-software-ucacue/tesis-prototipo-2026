// El cliente de la API (ADR-019): openapi-fetch con los tipos generados desde contratos/, en el
// mismo origen que la SPA. Agrega el token de la sesión en memoria (ADR-014, D-51) y, si una
// petición con sesión responde 401, la cierra como terminada (D-50).
import createClient, { type Middleware } from 'openapi-fetch';
import { FalloDeApi } from '../errores/traducir';
import { cerrarSesion, obtenerToken } from '../sesion/almacen';
import type { paths } from './generado/usuarios';

const conToken: Middleware = {
  onRequest({ request }) {
    const token = obtenerToken();
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`);
    }
    return request;
  },
  onResponse({ request, response }) {
    if (response.status === 401 && request.headers.has('Authorization')) {
      cerrarSesion('sesion-terminada');
    }
    return response;
  },
};

export const api = createClient<paths>({
  baseUrl: globalThis.location?.origin ?? '',
  // fetch se lee en cada llamada, para que las pruebas puedan reemplazarlo.
  fetch: (peticion) => globalThis.fetch(peticion),
});
api.use(conToken);

/**
 * Espera la respuesta: con 2xx devuelve los datos; con otro estado, rechaza con un
 * {@link FalloDeApi} que traduce traducirError. Un fallo de red rechaza con su TypeError.
 */
export async function llamar<T>(
  respuesta: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await respuesta;
  if (!response.ok) {
    throw new FalloDeApi(response.status, error);
  }
  return data as T;
}
