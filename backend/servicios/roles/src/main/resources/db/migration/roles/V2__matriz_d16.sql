-- Funcionalidades, roles y permisos de la matriz D-16 (F05-H02, D-63, D-65).
-- Las funcionalidades y los 10 roles de D-16 son fijos: cambiarlos exige otra migración y una
-- decisión. La interpretación de cada celda de la matriz está en specs/011-roles-permisos/data-model.md.

create table funcionalidad (
    codigo varchar(40) primary key,
    nombre varchar(80) not null unique,
    orden int not null unique
);

create table funcionalidad_tipo (
    funcionalidad varchar(40) not null references funcionalidad (codigo),
    tipo varchar(12) not null check (tipo in ('LECTURA', 'ESCRITURA', 'APROBACION', 'FIRMA')),
    primary key (funcionalidad, tipo)
);

create table rol (
    id uuid primary key,
    identificador varchar(64) not null unique check (identificador ~ '^[A-Z0-9_]{1,64}$'),
    nombre varchar(80) not null unique,
    descripcion varchar(300) not null,
    predefinido boolean not null,
    creado_en timestamptz not null,
    actualizado_en timestamptz not null
);

create table rol_permiso (
    rol_id uuid not null references rol (id) on delete cascade,
    funcionalidad varchar(40) not null,
    tipo varchar(12) not null,
    alcance varchar(8) not null check (alcance in ('TODO', 'PROPIO')),
    primary key (rol_id, funcionalidad, tipo),
    foreign key (funcionalidad, tipo) references funcionalidad_tipo (funcionalidad, tipo)
);

insert into funcionalidad (codigo, nombre, orden) values
    ('USUARIOS_ROLES', 'Usuarios y roles', 1),
    ('PARAMETROS', 'Parámetros y normativa', 2),
    ('CATALOGOS', 'Catálogos y datos maestros', 3),
    ('FLUJO_FIRMAS', 'Flujo y árbol de firmas', 4),
    ('BITACORA', 'Bitácora', 5),
    ('CARGAS', 'Cargas de datos', 6),
    ('DOCENTES', 'Docentes', 7),
    ('OFERTA', 'Oferta y propuesta de docentes', 8),
    ('PROYECTOS', 'Proyectos y horas de participantes', 9),
    ('CONSOLIDACION', 'Consolidación e informe sustentado', 10),
    ('DISTRIBUTIVO', 'Distributivo del periodo', 11),
    ('REPORTES', 'Reportes', 12),
    ('VOTACION_OCS', 'Votación del OCS', 13),
    ('RESOLUCION_OCS', 'Resolución del OCS', 14),
    ('PUBLICACION', 'Publicación del distributivo', 15),
    ('MI_DISTRIBUTIVO', 'Mi distributivo', 16);

insert into funcionalidad_tipo (funcionalidad, tipo) values
    ('USUARIOS_ROLES', 'LECTURA'), ('USUARIOS_ROLES', 'ESCRITURA'),
    ('PARAMETROS', 'LECTURA'), ('PARAMETROS', 'ESCRITURA'),
    ('CATALOGOS', 'LECTURA'), ('CATALOGOS', 'ESCRITURA'),
    ('FLUJO_FIRMAS', 'LECTURA'), ('FLUJO_FIRMAS', 'ESCRITURA'),
    ('BITACORA', 'LECTURA'),
    ('CARGAS', 'LECTURA'), ('CARGAS', 'ESCRITURA'),
    ('DOCENTES', 'LECTURA'), ('DOCENTES', 'ESCRITURA'),
    ('OFERTA', 'LECTURA'), ('OFERTA', 'ESCRITURA'), ('OFERTA', 'APROBACION'),
    ('PROYECTOS', 'LECTURA'), ('PROYECTOS', 'ESCRITURA'), ('PROYECTOS', 'APROBACION'),
    ('CONSOLIDACION', 'LECTURA'), ('CONSOLIDACION', 'ESCRITURA'), ('CONSOLIDACION', 'APROBACION'),
    ('DISTRIBUTIVO', 'LECTURA'), ('DISTRIBUTIVO', 'ESCRITURA'), ('DISTRIBUTIVO', 'APROBACION'),
    ('DISTRIBUTIVO', 'FIRMA'),
    ('REPORTES', 'LECTURA'),
    ('VOTACION_OCS', 'APROBACION'),
    ('RESOLUCION_OCS', 'LECTURA'), ('RESOLUCION_OCS', 'ESCRITURA'),
    ('PUBLICACION', 'APROBACION'),
    ('MI_DISTRIBUTIVO', 'LECTURA'), ('MI_DISTRIBUTIVO', 'ESCRITURA');

-- Los identificadores son los de F05-H01; los nombres, los de la SPA (sesion/roles.ts).
insert into rol (id, identificador, nombre, descripcion, predefinido, creado_en, actualizado_en)
select gen_random_uuid(), v.identificador, v.nombre, v.descripcion, true, now(), now()
from (values
    ('ADMINISTRADOR', 'Administrador del sistema',
        'Configura usuarios, roles, catálogos, parámetros normativos y el flujo de firmas. Lee todo, incluida la bitácora.'),
    ('OPERADOR_DATOS', 'Talento Humano (operador de datos)',
        'Ejecuta las cargas que se le deleguen y completa los datos de los docentes.'),
    ('COORDINADOR_CARRERA', 'Coordinador de carrera',
        'Planifica la oferta de su carrera, propone un docente por asignatura y paralelo y envía su borrador.'),
    ('GESTOR_UNIDAD', 'Gestor de investigación o vinculación',
        'Registra los proyectos aprobados de su unidad y las horas de sus participantes, y envía su planificación.'),
    ('RESPONSABLE_CONSOLIDACION', 'Responsable de consolidación',
        'Consolida el distributivo del periodo, emite el informe sustentado y firma F1 (elaborado).'),
    ('VICERRECTOR', 'Vicerrector académico',
        'Revisa y ajusta la propuesta consolidada, la aprueba y la congela, y firma F2 (revisado).'),
    ('RECTOR', 'Rector',
        'Aprueba el distributivo institucional, puede devolverlo y firma F3 (aprobado).'),
    ('MIEMBRO_OCS', 'Miembro del OCS',
        'Revisa la versión congelada y los reportes, y vota cuando la votación electrónica está activa.'),
    ('SECRETARIA_GENERAL', 'Secretaría general académica',
        'Registra la resolución del OCS y publica el distributivo aprobado.'),
    ('DOCENTE', 'Docente',
        'Consulta su distributivo y su estado, y acusa recibo.')
) as v (identificador, nombre, descripcion);

insert into rol_permiso (rol_id, funcionalidad, tipo, alcance)
select r.id, v.funcionalidad, v.tipo, v.alcance
from rol r
join (values
    -- Administrador del sistema: «Todo, incluida la bitácora» · «Usuarios, roles, parámetros,
    -- catálogos, archivo de configuración del flujo y del árbol de firmas».
    ('ADMINISTRADOR', 'USUARIOS_ROLES', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'PARAMETROS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'CATALOGOS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'FLUJO_FIRMAS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'BITACORA', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'CARGAS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'DOCENTES', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'OFERTA', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'PROYECTOS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'CONSOLIDACION', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'REPORTES', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'RESOLUCION_OCS', 'LECTURA', 'TODO'),
    ('ADMINISTRADOR', 'USUARIOS_ROLES', 'ESCRITURA', 'TODO'),
    ('ADMINISTRADOR', 'PARAMETROS', 'ESCRITURA', 'TODO'),
    ('ADMINISTRADOR', 'CATALOGOS', 'ESCRITURA', 'TODO'),
    ('ADMINISTRADOR', 'FLUJO_FIRMAS', 'ESCRITURA', 'TODO'),
    -- Talento Humano: «Cargas y docentes» · «Cargas que se le deleguen y datos del docente».
    ('OPERADOR_DATOS', 'CARGAS', 'LECTURA', 'TODO'),
    ('OPERADOR_DATOS', 'DOCENTES', 'LECTURA', 'TODO'),
    ('OPERADOR_DATOS', 'CARGAS', 'ESCRITURA', 'TODO'),
    ('OPERADOR_DATOS', 'DOCENTES', 'ESCRITURA', 'TODO'),
    -- Coordinador de carrera: «Su carrera y las horas de cada docente en otras unidades» ·
    -- «Oferta propia y propuesta de docentes» · «Envía su borrador».
    ('COORDINADOR_CARRERA', 'OFERTA', 'LECTURA', 'PROPIO'),
    ('COORDINADOR_CARRERA', 'DISTRIBUTIVO', 'LECTURA', 'PROPIO'),
    ('COORDINADOR_CARRERA', 'DOCENTES', 'LECTURA', 'TODO'),
    ('COORDINADOR_CARRERA', 'OFERTA', 'ESCRITURA', 'PROPIO'),
    ('COORDINADOR_CARRERA', 'OFERTA', 'APROBACION', 'PROPIO'),
    -- Gestor: «Su unidad y sus proyectos» · «Proyectos aprobados y horas de sus participantes» ·
    -- «Envía su planificación».
    ('GESTOR_UNIDAD', 'PROYECTOS', 'LECTURA', 'PROPIO'),
    ('GESTOR_UNIDAD', 'PROYECTOS', 'ESCRITURA', 'PROPIO'),
    ('GESTOR_UNIDAD', 'PROYECTOS', 'APROBACION', 'PROPIO'),
    -- Responsable de consolidación: «Todo el distributivo del periodo» · «Consolidación e informe
    -- sustentado» · «Envía a revisión; devuelve» · «F1 (elaborado)».
    ('RESPONSABLE_CONSOLIDACION', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'OFERTA', 'LECTURA', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'PROYECTOS', 'LECTURA', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'CONSOLIDACION', 'LECTURA', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'CONSOLIDACION', 'ESCRITURA', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'CONSOLIDACION', 'APROBACION', 'TODO'),
    ('RESPONSABLE_CONSOLIDACION', 'DISTRIBUTIVO', 'FIRMA', 'TODO'),
    -- Vicerrector académico: «Todo el distributivo, reportes y bitácora» · «Ajustes a la
    -- propuesta» · «Aprueba y congela; devuelve» · «F2 (revisado)».
    ('VICERRECTOR', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('VICERRECTOR', 'REPORTES', 'LECTURA', 'TODO'),
    ('VICERRECTOR', 'BITACORA', 'LECTURA', 'TODO'),
    ('VICERRECTOR', 'DISTRIBUTIVO', 'ESCRITURA', 'TODO'),
    ('VICERRECTOR', 'DISTRIBUTIVO', 'APROBACION', 'TODO'),
    ('VICERRECTOR', 'DISTRIBUTIVO', 'FIRMA', 'TODO'),
    -- Rector: «Todo el distributivo y reportes» · «Devuelve» · «F3 (aprobado)».
    ('RECTOR', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('RECTOR', 'REPORTES', 'LECTURA', 'TODO'),
    ('RECTOR', 'DISTRIBUTIVO', 'APROBACION', 'TODO'),
    ('RECTOR', 'DISTRIBUTIVO', 'FIRMA', 'TODO'),
    -- Miembro del OCS: «Versión congelada y reportes» · «Voto (opcional, C-15)».
    ('MIEMBRO_OCS', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('MIEMBRO_OCS', 'REPORTES', 'LECTURA', 'TODO'),
    ('MIEMBRO_OCS', 'VOTACION_OCS', 'APROBACION', 'TODO'),
    -- Secretaría general académica: «Todo el distributivo y reportes» · «Resolución del OCS» ·
    -- «Publica».
    ('SECRETARIA_GENERAL', 'DISTRIBUTIVO', 'LECTURA', 'TODO'),
    ('SECRETARIA_GENERAL', 'REPORTES', 'LECTURA', 'TODO'),
    ('SECRETARIA_GENERAL', 'RESOLUCION_OCS', 'LECTURA', 'TODO'),
    ('SECRETARIA_GENERAL', 'RESOLUCION_OCS', 'ESCRITURA', 'TODO'),
    ('SECRETARIA_GENERAL', 'PUBLICACION', 'APROBACION', 'TODO'),
    -- Docente: «Su distributivo y su estado» · «Acuse de recibo».
    ('DOCENTE', 'MI_DISTRIBUTIVO', 'LECTURA', 'PROPIO'),
    ('DOCENTE', 'MI_DISTRIBUTIVO', 'ESCRITURA', 'PROPIO')
) as v (identificador, funcionalidad, tipo, alcance) on r.identificador = v.identificador;
