// Los tipos, las consultas y las operaciones de la pantalla «Oferta del periodo»
// (contratos/academico, F03-H03).
import { useQuery } from '@tanstack/react-query';
import { apiAcademico, apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/academico';
import type { components as componentesDeGeneracion } from '../../api/generado/generacion';

export type OfertaDelPeriodo = components['schemas']['OfertaDelPeriodo'];
export type AsignaturaParalelo = components['schemas']['AsignaturaParalelo'];
export type AsignaturaParaleloNueva = components['schemas']['AsignaturaParaleloNueva'];
export type DatosDeAsignaturaParalelo = components['schemas']['DatosDeAsignaturaParalelo'];
export type EstadoDeAsignaturaParalelo = components['schemas']['EstadoDeAsignaturaParalelo'];
export type Sugerencia = componentesDeGeneracion['schemas']['Sugerencia'];
export type Candidato = componentesDeGeneracion['schemas']['Candidato'];
export type Parciales = componentesDeGeneracion['schemas']['Parciales'];
export type ParcialDeCarrera = componentesDeGeneracion['schemas']['ParcialDeCarrera'];
export type Propuesta = componentesDeGeneracion['schemas']['Propuesta'];
export type PropuestaNueva = componentesDeGeneracion['schemas']['PropuestaNueva'];
export type TipoDeJustificacion = componentesDeGeneracion['schemas']['TipoDeJustificacion'];

/** Cómo se llama cada justificación en pantalla. */
export const NOMBRE_DE_LA_JUSTIFICACION: Record<TipoDeJustificacion, string> = {
  AFINIDAD: 'Afinidad',
  DISPONIBILIDAD: 'Disponibilidad',
  EXCEPCION: 'Excepción',
};

/** La clave de la oferta de todos los periodos, para recargarla después de un cambio. */
export const CLAVE_OFERTA = ['academico', 'oferta'] as const;

/**
 * Los docentes sugeridos para una asignatura-paralelo, que arma Generación (RF-25, ADR-010). No se
 * guarda en caché al cerrar: siempre refleja las fichas de ese momento.
 */
export function useSugerencia(periodo: string, asignatura: string, paralelo: string) {
  return useQuery({
    queryKey: ['sugerencia', periodo, asignatura, paralelo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET(
          '/api/generacion/periodos/{periodo}/oferta/{asignatura}/{paralelo}/sugerencia',
          { params: { path: { periodo, asignatura, paralelo } } },
        ),
      ),
    gcTime: 0,
  });
}

/** La oferta del periodo con sus horas a cubrir; sin periodo, no consulta. */
export function useOferta(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_OFERTA, periodo],
    queryFn: () =>
      llamar(
        apiAcademico.GET('/api/academico/periodos/{codigo}/oferta', {
          params: { path: { codigo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
  });
}

/**
 * Los parciales del periodo, con su avance y sus propuestas (RF-24). Van bajo la clave de la oferta:
 * retirar o volver a ofertar cambia su avance.
 */
export function useParciales(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_OFERTA, 'parciales', periodo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/periodos/{periodo}/parciales', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
  });
}

/** Propone un docente, o reemplaza al que tenga. La justificación la decide el servidor. */
export function proponerDocente(
  periodo: string,
  asignatura: string,
  paralelo: string,
  cuerpo: PropuestaNueva,
) {
  return llamar(
    apiGeneracion.PUT(
      '/api/generacion/periodos/{periodo}/oferta/{asignatura}/{paralelo}/propuesta',
      { params: { path: { periodo, asignatura, paralelo } }, body: cuerpo },
    ),
  );
}

/** Quita la propuesta de una asignatura-paralelo. */
export function quitarPropuesta({ periodo, asignatura, paralelo }: AsignaturaParalelo) {
  return llamar(
    apiGeneracion.DELETE(
      '/api/generacion/periodos/{periodo}/oferta/{asignatura}/{paralelo}/propuesta',
      { params: { path: { periodo, asignatura, paralelo } } },
    ),
  );
}

/** Oferta una asignatura activa de una carrera vigente en un paralelo del periodo. */
export function ofertarAsignatura(periodo: string, cuerpo: AsignaturaParaleloNueva) {
  return llamar(
    apiAcademico.POST('/api/academico/periodos/{codigo}/oferta', {
      params: { path: { codigo: periodo } },
      body: cuerpo,
    }),
  );
}

/** Corrige la jornada, la modalidad, la sede, los estudiantes o las horas. */
export function corregirAsignaturaParalelo(
  { periodo, asignatura, paralelo }: AsignaturaParalelo,
  cuerpo: DatosDeAsignaturaParalelo,
) {
  return llamar(
    apiAcademico.PUT('/api/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}', {
      params: { path: { codigo: periodo, asignatura, paralelo } },
      body: cuerpo,
    }),
  );
}

/** Retira una asignatura-paralelo o la vuelve a ofertar; ninguna se borra. */
export function cambiarEstadoAsignaturaParalelo(
  { periodo, asignatura, paralelo }: AsignaturaParalelo,
  estado: EstadoDeAsignaturaParalelo,
) {
  return llamar(
    apiAcademico.PUT('/api/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}/estado', {
      params: { path: { codigo: periodo, asignatura, paralelo } },
      body: { estado },
    }),
  );
}
