// Los tipos, la consulta y las descargas de la pantalla «Plantillas de carga» (contratos/cargas,
// F01-H01).
import { useQuery } from '@tanstack/react-query';
import { apiCargas, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/cargas';
import { FalloDeApi } from '../../errores/traducir';

export type DiccionarioDeDatos = components['schemas']['DiccionarioDeDatos'];
export type Carga = components['schemas']['Carga'];
export type ColumnaDeCarga = components['schemas']['ColumnaDeCarga'];
export type Obligatoriedad = components['schemas']['Obligatoriedad'];
export type LecturaDeArchivo = components['schemas']['LecturaDeArchivo'];

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
  const nombre =
    /filename="([^"]+)"/.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ??
    (que === 'plantilla' ? carga.archivo : carga.archivo.replace('.csv', '_diccionario.csv'));
  guardar(data, nombre);
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

/**
 * Sube el archivo a su carga para validar su estructura (F01-H02). El servidor no lo guarda; responde
 * cuántos registros leyó o rechaza con su código.
 */
export function subirArchivo(carga: Carga, archivo: File): Promise<LecturaDeArchivo> {
  return llamar(
    apiCargas.POST('/api/cargas/plantillas/{codigo}/archivo', {
      params: { path: { codigo: carga.codigo } },
      // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
      body: { archivo: archivo as unknown as string },
      bodySerializer: (cuerpo) => {
        const formulario = new FormData();
        formulario.append('archivo', cuerpo.archivo as unknown as File);
        return formulario;
      },
    }),
  );
}
