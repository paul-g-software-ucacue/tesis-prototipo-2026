// Respuestas de la API de Roles para las pruebas de la pantalla «Roles» (contratos/roles).
export const FUNCIONALIDADES = [
  { codigo: 'USUARIOS_ROLES', nombre: 'Usuarios y roles', tipos: ['LECTURA', 'ESCRITURA'] },
  { codigo: 'CARGAS', nombre: 'Cargas de datos', tipos: ['LECTURA', 'ESCRITURA'] },
  { codigo: 'DOCENTES', nombre: 'Docentes', tipos: ['LECTURA', 'ESCRITURA'] },
  { codigo: 'VOTACION_OCS', nombre: 'Votación del OCS', tipos: ['APROBACION'] },
];

export const ADMINISTRADOR = {
  identificador: 'ADMINISTRADOR',
  nombre: 'Administrador del sistema',
  descripcion: 'Configura usuarios, roles y catálogos.',
  predefinido: true,
  permisos: [
    { funcionalidad: 'CARGAS', tipo: 'LECTURA', alcance: 'TODO' },
    { funcionalidad: 'USUARIOS_ROLES', tipo: 'ESCRITURA', alcance: 'TODO' },
    { funcionalidad: 'USUARIOS_ROLES', tipo: 'LECTURA', alcance: 'TODO' },
  ],
  usuariosAsignados: 1,
};

export const AUDITOR = {
  identificador: 'AUDITOR_DE_CARGAS',
  nombre: 'Auditor de cargas',
  descripcion: 'Revisa las cargas sin cambiarlas.',
  predefinido: false,
  permisos: [{ funcionalidad: 'CARGAS', tipo: 'LECTURA', alcance: 'TODO' }],
  usuariosAsignados: 0,
};

export const REVISOR_ASIGNADO = {
  identificador: 'REVISOR',
  nombre: 'Revisor',
  descripcion: 'Revisa docentes.',
  predefinido: false,
  permisos: [{ funcionalidad: 'DOCENTES', tipo: 'LECTURA', alcance: 'PROPIO' }],
  usuariosAsignados: 2,
};

export const USUARIOS = [
  { usuario: 'admin.demo', nombre: 'Administración del sistema (demo)', activo: true },
  { usuario: 'docente.demo', nombre: 'Docente (demo)', activo: true },
  { usuario: 'coordinacion.demo', nombre: 'Coordinación de carrera (demo)', activo: true },
];

export const ROLES_DE_USUARIOS = [
  { usuario: 'admin.demo', roles: ['ADMINISTRADOR'] },
  { usuario: 'docente.demo', roles: ['AUDITOR_DE_CARGAS'] },
  { usuario: 'coordinacion.demo', roles: ['COORDINADOR_CARRERA'] },
];

export const ADMINISTRADOR_SESION = {
  usuario: 'admin.demo',
  nombre: 'Administración del sistema (demo)',
  roles: ['ADMINISTRADOR'],
  permisos: ['USUARIOS_ROLES:ESCRITURA', 'USUARIOS_ROLES:LECTURA'],
};

export const SOLO_LECTURA_SESION = {
  usuario: 'docente.demo',
  nombre: 'Docente (demo)',
  roles: ['AUDITOR_DE_ROLES'],
  permisos: ['USUARIOS_ROLES:LECTURA'],
};
