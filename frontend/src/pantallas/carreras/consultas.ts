// Los tipos, las consultas y las operaciones de la pantalla «Carreras y mallas»
// (contratos/academico, F03-H02).
import { useQuery } from '@tanstack/react-query';
import { apiAcademico, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/academico';

export type Carrera = components['schemas']['Carrera'];
export type CarreraNueva = components['schemas']['CarreraNueva'];
export type DatosDeCarrera = components['schemas']['DatosDeCarrera'];
export type ResumenDeMalla = components['schemas']['ResumenDeMalla'];
export type Asignatura = components['schemas']['Asignatura'];
export type AsignaturaNueva = components['schemas']['AsignaturaNueva'];
export type DatosDeAsignatura = components['schemas']['DatosDeAsignatura'];
export type CampoAmplio = components['schemas']['CampoAmplio'];
export type NivelDeFormacion = components['schemas']['NivelDeFormacion'];
export type Modalidad = components['schemas']['Modalidad'];
export type Jornada = components['schemas']['Jornada'];
export type EstadoDeCarrera = components['schemas']['EstadoDeCarrera'];
export type EstadoDeMalla = components['schemas']['EstadoDeMalla'];
export type UnidadDeOrganizacionCurricular =
  components['schemas']['UnidadDeOrganizacionCurricular'];

/** La clave de las carreras y de sus asignaturas, para recargarlas después de un cambio. */
export const CLAVE_CARRERAS = ['academico', 'carreras'] as const;

/** Todas las carreras, con sus mallas y las horas de cada una. */
export function useCarreras() {
  return useQuery({
    queryKey: CLAVE_CARRERAS,
    queryFn: () => llamar(apiAcademico.GET('/api/academico/carreras')),
  });
}

/** Las asignaturas de una carrera, de todas sus mallas; sin carrera, no consulta. */
export function useAsignaturas(carrera: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_CARRERAS, carrera, 'asignaturas'],
    queryFn: () =>
      llamar(
        apiAcademico.GET('/api/academico/carreras/{codigo}/asignaturas', {
          params: { path: { codigo: carrera ?? '' } },
        }),
      ),
    enabled: Boolean(carrera),
  });
}

/** Los 11 campos amplios de la CINE; el catálogo no cambia. */
export function useCamposAmplios() {
  return useQuery({
    queryKey: ['academico', 'campos-amplios'],
    queryFn: () => llamar(apiAcademico.GET('/api/academico/campos-amplios')),
    staleTime: Infinity,
  });
}

/** Registra una carrera vigente; su código no cambia después. */
export function registrarCarrera(cuerpo: CarreraNueva) {
  return llamar(apiAcademico.POST('/api/academico/carreras', { body: cuerpo }));
}

/** Corrige todo menos el código y el estado. */
export function corregirCarrera(codigo: string, cuerpo: DatosDeCarrera) {
  return llamar(
    apiAcademico.PUT('/api/academico/carreras/{codigo}', {
      params: { path: { codigo } },
      body: cuerpo,
    }),
  );
}

/** Cambia el estado de la carrera; solo una vigente admite asignaturas nuevas y oferta. */
export function cambiarEstadoCarrera(codigo: string, estado: EstadoDeCarrera) {
  return llamar(
    apiAcademico.PUT('/api/academico/carreras/{codigo}/estado', {
      params: { path: { codigo } },
      body: { estado },
    }),
  );
}

/** Registra una malla vigente en la carrera; su código de versión no cambia. */
export function registrarMalla(carrera: string, codigo: string) {
  return llamar(
    apiAcademico.POST('/api/academico/carreras/{codigo}/mallas', {
      params: { path: { codigo: carrera } },
      body: { codigo },
    }),
  );
}

/** Deroga la malla o la vuelve a poner vigente; conserva sus asignaturas. */
export function cambiarEstadoMalla(codigo: string, estado: EstadoDeMalla) {
  return llamar(
    apiAcademico.PUT('/api/academico/mallas/{codigo}/estado', {
      params: { path: { codigo } },
      body: { estado },
    }),
  );
}

/** Registra una asignatura activa en una malla vigente. */
export function registrarAsignatura(cuerpo: AsignaturaNueva) {
  return llamar(apiAcademico.POST('/api/academico/asignaturas', { body: cuerpo }));
}

/** Corrige todo menos el código, la carrera, la malla y el estado. */
export function corregirAsignatura(codigo: string, cuerpo: DatosDeAsignatura) {
  return llamar(
    apiAcademico.PUT('/api/academico/asignaturas/{codigo}', {
      params: { path: { codigo } },
      body: cuerpo,
    }),
  );
}

/** Inactiva o reactiva la asignatura; ninguna se borra. */
export function cambiarEstadoAsignatura(codigo: string, activa: boolean) {
  return llamar(
    apiAcademico.PUT('/api/academico/asignaturas/{codigo}/estado', {
      params: { path: { codigo } },
      body: { activa },
    }),
  );
}
