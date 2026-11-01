// Las actividades de 2026-2 para las pruebas: ficticias. Un docente con una actividad de docencia y
// otra con proyecto, y las actividades posibles: libre, con proyecto, sin proyectos, sin cargo y una
// de gestión que no pide cargo.
import type { ActividadesDelPeriodo, ActividadesPosibles } from './consultas';

export const CARRERA = '550613A01-P-0301';

export const ACTIVIDADES: ActividadesDelPeriodo = {
  creado: true,
  carreras: [
    {
      carrera: CARRERA,
      docentes: [
        {
          docente: '0109990010',
          nombreDocente: 'PERALTA MALDONADO JUAN ANDRES',
          actividades: [
            {
              actividad: '140-b',
              nombre: 'Planificar y actualizar contenidos de clases',
              componente: 'OTRAS_DOCENCIA',
              horas: 4.25,
            },
            {
              actividad: '141-a',
              nombre: 'Diseñar y ejecutar proyectos de investigación',
              componente: 'INVESTIGACION',
              horas: 6,
              proyecto: 'INV-2025-01',
              nombreProyecto: 'Monitoreo de hatos',
            },
          ],
          horasPorComponente: { otrasDocencia: 4.25, investigacion: 6, vinculacion: 0, gestion: 0 },
        },
      ],
    },
  ],
};

export const SIN_ACTIVIDADES: ActividadesDelPeriodo = {
  creado: true,
  carreras: [{ carrera: CARRERA, docentes: [] }],
};

export const SIN_DISTRIBUTIVO: ActividadesDelPeriodo = { creado: false, carreras: [] };

export const POSIBLES: ActividadesPosibles = {
  docente: '0109990010',
  actividades: [
    {
      actividad: '140-b',
      nombre: 'Planificar y actualizar contenidos de clases',
      componente: 'OTRAS_DOCENCIA',
      disponible: true,
      exigeProyecto: false,
      proyectos: [],
    },
    {
      actividad: '141-a',
      nombre: 'Diseñar y ejecutar proyectos de investigación',
      componente: 'INVESTIGACION',
      disponible: true,
      exigeProyecto: true,
      proyectos: [{ codigo: 'INV-2025-01', nombre: 'Monitoreo de hatos' }],
    },
    {
      actividad: '142-a',
      nombre: 'Desarrollar proyectos de vinculación',
      componente: 'VINCULACION',
      disponible: false,
      exigeProyecto: true,
      proyectos: [],
      motivo: 'El docente no tiene proyectos asignables de este tipo en el periodo.',
    },
    {
      actividad: '143-d',
      nombre: 'Coordinar carreras o programas',
      componente: 'GESTION',
      disponible: false,
      exigeProyecto: false,
      proyectos: [],
      motivo: 'El docente no tiene vigente el cargo que exige esta actividad.',
    },
    {
      actividad: '143-k',
      nombre: 'Organizar o dirigir eventos académicos',
      componente: 'GESTION',
      disponible: true,
      exigeProyecto: false,
      proyectos: [],
    },
  ],
};
