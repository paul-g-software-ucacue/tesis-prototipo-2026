// Respuestas de la API de Normativa para las pruebas de la pantalla «Normativa»
// (contratos/normativa). Un resumen de la versión 1: dos filas y dos reglas de la política 2024.
export const VERSIONES = [
  {
    numero: 1,
    codigo: 'SENESCYT-2024-03',
    origen: 'Política SENESCYT 2024',
    vigenteDesde: '2024-03-11',
    tipoIst: 'T2',
  },
];

export const VERSION_1 = {
  ...VERSIONES[0],
  reglas: [
    {
      identificador: 'RN-01',
      parametros: [
        { nombre: 'dedicacion_tiempo_completo', valor: 40 },
        { nombre: 'dedicacion_medio_tiempo', valor: 20 },
      ],
    },
    {
      identificador: 'RN-02',
      severidad: 'DURA',
      parametros: [{ nombre: 'horas_maximas_docente', valor: 40 }],
    },
    {
      identificador: 'RN-05',
      severidad: 'BLANDA',
      parametros: [{ nombre: 'porcentaje_clase', valor: 60 }],
    },
  ],
  tablaCargaHoraria: [
    {
      aplicaA: 'TODOS',
      perfil: 'DOCENTE_TC',
      dedicacionTotal: 40,
      horasClase: 22,
      horasClaseMinimo: 18,
      horasClaseMaximo: 24,
      horasOtrasDocencia: 18,
      horasGestion: 0,
      severidad: 'DURA',
      referencia: 'POL 6; RCE arts. 146 y 149',
      observacion: 'Por defecto 22 h clase + 18 otras de docencia (C-01)',
    },
    {
      aplicaA: 'TODOS',
      perfil: 'DOCENTE_INVESTIGACION_VINCULACION',
      dedicacionTotal: 40,
      horasClase: 16,
      horasClaseMinimo: 0,
      horasClaseMaximo: 16,
      horasInvestigacionVinculacionMaximo: 24,
      severidad: 'DURA',
      referencia: 'POL 6',
    },
    {
      aplicaA: 'T2',
      perfil: 'GESTOR',
      dedicacionTotal: 40,
      horasClase: 14,
      horasClaseMinimo: 14,
      horasClaseMaximo: 14,
      horasOtrasDocencia: 16,
      horasGestion: 10,
      severidad: 'DURA',
      referencia: 'POL 6 (docentes gestores)',
    },
  ],
};
