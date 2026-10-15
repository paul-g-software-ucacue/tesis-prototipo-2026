// La API simulada de las pruebas de Vitest: reemplaza fetch por respuestas escritas a mano según
// el método y la ruta, y guarda cada petición para comprobarla (sin MSW, research §8).
import { vi } from 'vitest';

type Respuesta = Response | (() => Response | Promise<Response>);

export interface ApiSimulada {
  peticiones: Request[];
  responder(metodo: string, ruta: string, respuesta: Respuesta): void;
}

export function json(estado: number, cuerpo?: unknown): Response {
  return new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });
}

export const TOKEN_CORRECTO = {
  token: 'cabecera.cuerpo.firma',
  tipo: 'Bearer',
  venceEn: '2099-01-01T00:00:00Z',
};

export const CREDENCIALES_INVALIDAS = {
  titulo: 'Credenciales inválidas',
  descripcion: 'El usuario o la contraseña no son correctos.',
  codigo: 'USUARIOS_CREDENCIALES_INVALIDAS',
};

export function instalarApiSimulada(): ApiSimulada {
  const rutas = new Map<string, Respuesta>();
  const peticiones: Request[] = [];
  vi.stubGlobal('fetch', async (entrada: RequestInfo | URL, opciones?: RequestInit) => {
    const peticion = new Request(entrada, opciones);
    peticiones.push(peticion.clone());
    const clave = `${peticion.method} ${new URL(peticion.url).pathname}`;
    const respuesta = rutas.get(clave);
    if (!respuesta) {
      return json(404, { titulo: 'No simulada', descripcion: clave, codigo: 'PRUEBA' });
    }
    return typeof respuesta === 'function' ? respuesta() : respuesta.clone();
  });
  return {
    peticiones,
    responder(metodo, ruta, respuesta) {
      rutas.set(`${metodo} ${ruta}`, respuesta);
    },
  };
}
