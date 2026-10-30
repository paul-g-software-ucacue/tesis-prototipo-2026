// Los tipos, la consulta y las descargas de la pantalla «Plantillas de carga» (contratos/cargas,
// F01-H01), la subida (F01-H02), el informe de las filas (F01-H03), la vista previa, la
// confirmación y el historial (F01-H04), y el estado de cada carga y «sin proyectos» (F01-H05).
import { useQuery } from '@tanstack/react-query';
import { apiCargas, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/cargas';
import { FalloDeApi } from '../../errores/traducir';

export type DiccionarioDeDatos = components['schemas']['DiccionarioDeDatos'];
export type Carga = components['schemas']['Carga'];
export type ColumnaDeCarga = components['schemas']['ColumnaDeCarga'];
export type Obligatoriedad = components['schemas']['Obligatoriedad'];
export type LecturaDeArchivo = components['schemas']['LecturaDeArchivo'];
export type ProblemaDeFila = components['schemas']['ProblemaDeFila'];
export type VistaPrevia = components['schemas']['VistaPrevia'];
export type RegistroDeLaVista = components['schemas']['RegistroDeLaVista'];
export type ResultadoDeConfirmacion = components['schemas']['ResultadoDeConfirmacion'];
export type EntradaDeHistorial = components['schemas']['EntradaDeHistorial'];
export type EstadoDeCarga = components['schemas']['EstadoDeCarga'];
export type CargaQueFalta = components['schemas']['CargaQueFalta'];

/** La clave del estado de las cargas, para invalidarlo después de confirmar (F01-H05). */
export const CLAVE_ESTADO = ['cargas', 'estado'] as const;

/** El código de un periodo, como en la carga 3: «2026-I» o «2026-II» (contrato, principio XIV). */
export const PERIODO = /^[0-9]{4}-(I|II)$/;

/** El diccionario de datos de las 10 cargas; no cambia mientras el servidor corre. */
export function useDiccionario() {
  return useQuery({
    queryKey: ['cargas', 'plantillas'],
    queryFn: () => llamar(apiCargas.GET('/api/cargas/plantillas')),
    staleTime: Infinity,
  });
}

/** Qué se descarga de una carga. */
export type Descarga = 'plantilla' | 'diccionario';

/**
 * Pide el CSV con el token, que está solo en memoria (D-51), y lo guarda con el nombre que manda el
 * servidor en Content-Disposition (research §9).
 */
export async function descargar(carga: Carga, que: Descarga): Promise<void> {
  const opciones = { params: { path: { codigo: carga.codigo } }, parseAs: 'blob' } as const;
  const { data, error, response } =
    que === 'plantilla'
      ? await apiCargas.GET('/api/cargas/plantillas/{codigo}/plantilla', opciones)
      : await apiCargas.GET('/api/cargas/plantillas/{codigo}/diccionario', opciones);
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(
    data,
    nombreDelServidor(response) ??
      (que === 'plantilla' ? carga.archivo : carga.archivo.replace('.csv', '_diccionario.csv')),
  );
}

function nombreDelServidor(respuesta: Response): string | undefined {
  return /filename="([^"]+)"/.exec(respuesta.headers.get('Content-Disposition') ?? '')?.[1];
}

function guardar(contenido: Blob, nombre: string) {
  const enlace = document.createElement('a');
  enlace.href = URL.createObjectURL(contenido);
  enlace.download = nombre;
  document.body.append(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(enlace.href);
}

/** El tamaño máximo de un archivo de carga, el mismo del servidor (F01-H02, research §3). */
export const MAXIMO_DE_UN_ARCHIVO = 5 * 1024 * 1024;

/** El mismo texto de CARGAS_ARCHIVO_DEMASIADO_GRANDE, para no subir en vano un archivo grande. */
export const ARCHIVO_DEMASIADO_GRANDE = {
  titulo: 'Archivo demasiado grande',
  descripcion: 'El archivo pasa de 5 MB, el tamaño máximo de una carga.',
  detalles: [],
};

/** El cuerpo multipart con el archivo, tal como lo piden las dos operaciones de la subida. */
function conElArchivo(carga: Carga, archivo: File) {
  return {
    params: { path: { codigo: carga.codigo } },
    // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
    body: { archivo: archivo as unknown as string },
    bodySerializer: (cuerpo: { archivo: string }) => {
      const formulario = new FormData();
      formulario.append('archivo', cuerpo.archivo as unknown as File);
      return formulario;
    },
  };
}

/**
 * Sube el archivo a su carga para validar su estructura (F01-H02) y sus filas (F01-H03). El servidor
 * no lo guarda; responde el informe, también si las filas tienen errores, o rechaza la estructura
 * con su código.
 */
export function subirArchivo(carga: Carga, archivo: File): Promise<LecturaDeArchivo> {
  return llamar(
    apiCargas.POST('/api/cargas/plantillas/{codigo}/archivo', conElArchivo(carga, archivo)),
  );
}

/**
 * Vuelve a enviar el mismo archivo para descargar todo su informe en CSV (F01-H03, research §12): el
 * servidor no guardó nada y lo valida de nuevo.
 */
export async function descargarInforme(carga: Carga, archivo: File): Promise<void> {
  const { data, error, response } = await apiCargas.POST(
    '/api/cargas/plantillas/{codigo}/archivo/informe',
    { ...conElArchivo(carga, archivo), parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(data, nombreDelServidor(response) ?? `informe-${carga.codigo}.csv`);
}

/**
 * Vuelve a enviar el mismo archivo con la huella de la vista previa revisada, para que su servicio
 * dueño la aplique completa o nada (F01-H04, MD-13). Si los datos cambiaron desde la vista previa,
 * el servidor no aplica nada y lo dice.
 */
export function confirmarCarga(
  carga: Carga,
  archivo: File,
  huella: string,
): Promise<ResultadoDeConfirmacion> {
  return llamar(
    apiCargas.POST('/api/cargas/plantillas/{codigo}/archivo/confirmacion', {
      params: { path: { codigo: carga.codigo } },
      // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
      body: { archivo: archivo as unknown as string, huella },
      bodySerializer: (cuerpo: { archivo: string; huella: string }) => {
        const formulario = new FormData();
        formulario.append('archivo', cuerpo.archivo as unknown as File);
        formulario.append('huella', cuerpo.huella);
        return formulario;
      },
    }),
  );
}

/**
 * El estado de las 10 cargas: si su servicio dueño tiene datos, qué cargas anteriores le faltan y,
 * en la de proyectos, los periodos «sin proyectos» (F01-H05, RF-56). Cambia con cada confirmación.
 */
export function useEstadoDeLasCargas() {
  return useQuery({
    queryKey: CLAVE_ESTADO,
    queryFn: () => llamar(apiCargas.GET('/api/cargas/estado')),
  });
}

/** Confirma que un periodo no tiene proyectos aprobados (F01-H05, RF-18). */
export function confirmarSinProyectos(periodo: string): Promise<EntradaDeHistorial> {
  return llamar(apiCargas.POST('/api/cargas/sin-proyectos', { body: { periodo } }));
}

/** El historial de una carga, del intento más reciente al más antiguo (F01-H04, RF-10). */
export function useHistorial(carga: Carga) {
  return useQuery({
    queryKey: ['cargas', 'historial', carga.codigo],
    queryFn: () =>
      llamar(
        apiCargas.GET('/api/cargas/plantillas/{codigo}/historial', {
          params: { path: { codigo: carga.codigo } },
        }),
      ),
  });
}
