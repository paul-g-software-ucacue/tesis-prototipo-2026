// Los tipos, las consultas y las operaciones de la pantalla «Docentes» (contratos/docentes,
// F03-H04).
import { useQuery } from '@tanstack/react-query';
import { apiDocentes, apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/docentes';
import type { components as componentesDeGeneracion } from '../../api/generado/generacion';

export type Docente = components['schemas']['Docente'];
export type DocenteNuevo = components['schemas']['DocenteNuevo'];
export type DatosDeDocente = components['schemas']['DatosDeDocente'];
export type ResumenDeDocente = components['schemas']['ResumenDeDocente'];
export type ReferenciasDeDocentes = components['schemas']['ReferenciasDeDocentes'];
export type EstadoLaboral = components['schemas']['EstadoLaboral'];
export type TipoPersonal = components['schemas']['TipoPersonal'];
export type CategoriaDeEscalafon = components['schemas']['CategoriaDeEscalafon'];
export type Dedicacion = components['schemas']['Dedicacion'];
export type CondicionEspecial = components['schemas']['CondicionEspecial'];
export type TipoDeIdentificacion = components['schemas']['TipoDeIdentificacion'];
export type TipoDeTituloDeCuartoNivel = components['schemas']['TipoDeTituloDeCuartoNivel'];
export type FichaAcademica = components['schemas']['FichaAcademica'];
export type AfinidadDelDocente = componentesDeGeneracion['schemas']['AfinidadDelDocente'];

/** La clave de los docentes, para recargarlos después de un cambio. */
export const CLAVE_DOCENTES = ['docentes'] as const;

/** Los criterios de la búsqueda; uno vacío no filtra. */
export interface Busqueda {
  texto: string;
  carrera: string;
  estado: EstadoLaboral | '';
}

/** Los docentes que cumplen los criterios, por apellidos y nombres. */
export function useDocentes({ texto, carrera, estado }: Busqueda) {
  return useQuery({
    queryKey: [...CLAVE_DOCENTES, 'busqueda', texto.trim(), carrera, estado],
    queryFn: () =>
      llamar(
        apiDocentes.GET('/api/docentes/docentes', {
          params: {
            query: {
              ...(texto.trim() && { texto: texto.trim() }),
              ...(carrera && { carrera }),
              ...(estado && { estado }),
            },
          },
        }),
      ),
  });
}

/** Un docente con su ficha académica; sin identificación, no consulta. */
export function useDocente(identificacion: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_DOCENTES, 'docente', identificacion],
    queryFn: () =>
      llamar(
        apiDocentes.GET('/api/docentes/docentes/{identificacion}', {
          params: { path: { identificacion: identificacion ?? '' } },
        }),
      ),
    enabled: Boolean(identificacion),
  });
}

/**
 * El puntaje del docente en la oferta de un periodo, que calcula Generación (RF-17, ADR-010). Sin
 * periodo, no consulta. No se guarda en caché al cerrar: siempre refleja la ficha de ese momento.
 */
export function useAfinidadDelDocente(identificacion: string, periodo: string) {
  return useQuery({
    queryKey: ['afinidad', identificacion, periodo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/docentes/{identificacion}/afinidad', {
          params: { path: { identificacion }, query: { periodo } },
        }),
      ),
    enabled: Boolean(periodo),
    gcTime: 0,
  });
}

/** Las carreras, las asignaturas y los campos amplios de Académico, para elegir. */
export function useReferenciasDeDocentes() {
  return useQuery({
    queryKey: [...CLAVE_DOCENTES, 'referencias'],
    queryFn: () => llamar(apiDocentes.GET('/api/docentes/referencias')),
  });
}

/** Registra un docente con su ficha; queda activo. */
export function registrarDocente(cuerpo: DocenteNuevo) {
  return llamar(apiDocentes.POST('/api/docentes/docentes', { body: cuerpo }));
}

/** Corrige los datos y la ficha; la identificación y el estado no cambian. */
export function corregirDocente(identificacion: string, cuerpo: DatosDeDocente) {
  return llamar(
    apiDocentes.PUT('/api/docentes/docentes/{identificacion}', {
      params: { path: { identificacion } },
      body: cuerpo,
    }),
  );
}

/** Pasa al docente a activo, en licencia o inactivo; ninguno se borra. */
export function cambiarEstadoDocente(identificacion: string, estado: EstadoLaboral) {
  return llamar(
    apiDocentes.PUT('/api/docentes/docentes/{identificacion}/estado', {
      params: { path: { identificacion } },
      body: { estado },
    }),
  );
}
