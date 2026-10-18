-- Versión 1 de la normativa: la Política SENESCYT 2024 de asignación de carga horaria (F04-H01,
-- RF-19, RN-01 y RN-03 a RN-15). Las 22 filas son las de la carga 7 (ERS 8.3); los parámetros y las
-- severidades de las reglas, los de specs/012-politica-normativa/research.md §4 (MD-08). Estas cifras
-- solo viven aquí (principio VI): una versión nueva se crea en F04-H02, nunca cambiando esta.

create table version_normativa (
    id uuid primary key,
    numero int not null unique check (numero >= 1),
    codigo varchar(40) not null unique,
    origen varchar(120) not null,
    vigente_desde date not null,
    tipo_ist varchar(2) not null check (tipo_ist in ('T1', 'T2', 'T3', 'T4', 'T5')),
    creada_en timestamptz not null
);

create table fila_carga_horaria (
    version_id uuid not null references version_normativa (id),
    orden int not null check (orden >= 1),
    aplica_a varchar(5) not null check (aplica_a in ('TODOS', 'T1', 'T2', 'T3', 'T4', 'T5')),
    perfil varchar(60) not null,
    dedicacion_total int not null check (dedicacion_total >= 0),
    horas_clase int not null check (horas_clase >= 0),
    horas_clase_minimo int not null check (horas_clase_minimo >= 0),
    horas_clase_maximo int not null check (horas_clase_maximo >= 0),
    horas_otras_docencia int check (horas_otras_docencia >= 0),
    horas_gestion int check (horas_gestion >= 0),
    horas_inv_vinc_maximo int check (horas_inv_vinc_maximo >= 0),
    severidad varchar(6) not null check (severidad in ('DURA', 'BLANDA')),
    referencia varchar(120) not null,
    observacion varchar(300),
    primary key (version_id, aplica_a, perfil),
    unique (version_id, orden)
);

create table regla (
    version_id uuid not null references version_normativa (id),
    identificador varchar(6) not null check (identificador ~ '^RN-[0-9]{2}$'),
    severidad varchar(6) check (severidad in ('DURA', 'BLANDA')),
    primary key (version_id, identificador)
);

create table parametro_regla (
    version_id uuid not null,
    regla varchar(6) not null,
    nombre varchar(60) not null,
    valor numeric(10, 2) not null,
    primary key (version_id, regla, nombre),
    foreign key (version_id, regla) references regla (version_id, identificador)
);

-- Cada versión creada, para la bitácora (FR-010). F04-H02 agrega sus acciones.
create table registro_cambio (
    id uuid primary key,
    momento timestamptz not null,
    usuario varchar(64) not null,
    accion varchar(20) not null check (accion in ('VERSION_CREADA')),
    detalle jsonb not null
);

insert into version_normativa (id, numero, codigo, origen, vigente_desde, tipo_ist, creada_en)
values (gen_random_uuid(), 1, 'SENESCYT-2024-03', 'Política SENESCYT 2024', date '2024-03-11', 'T2', now());

-- Las 22 filas, en el orden de la carga 7. Un valor vacío en la política queda en null, no en 0.
insert into fila_carga_horaria (version_id, orden, aplica_a, perfil, dedicacion_total, horas_clase,
    horas_clase_minimo, horas_clase_maximo, horas_otras_docencia, horas_gestion,
    horas_inv_vinc_maximo, severidad, referencia, observacion)
select v.id, f.*
from version_normativa v
cross join (values
    (1, 'TODOS', 'DOCENTE_TC', 40, 22, 18, 24, 18, 0, null, 'DURA', 'POL 6; RCE arts. 146 y 149', 'Por defecto 22 h clase + 18 otras de docencia (C-01)'),
    (2, 'TODOS', 'DOCENTE_MT', 20, 12, 8, 12, 8, 0, null, 'DURA', 'POL 6; RCE art. 147', 'El texto de POL 6 dice que MT no tiene otras actividades: verificar (C-03)'),
    (3, 'T1', 'COORDINADOR_CARRERA_SEDE', 40, 12, 12, 12, 16, 12, null, 'DURA', 'POL 6 (coordinadores de campus, extensión o sede y carrera)', null),
    (4, 'T2', 'COORDINADOR_CARRERA_SEDE', 40, 10, 10, 10, 16, 14, null, 'DURA', 'POL 6 (coordinadores de campus, extensión o sede y carrera)', null),
    (5, 'T3', 'COORDINADOR_CARRERA_SEDE', 40, 8, 8, 8, 16, 16, null, 'DURA', 'POL 6 (coordinadores de campus, extensión o sede y carrera)', null),
    (6, 'T4', 'COORDINADOR_CARRERA_SEDE', 40, 6, 6, 6, 16, 18, null, 'DURA', 'POL 6 (coordinadores de campus, extensión o sede y carrera)', null),
    (7, 'T5', 'COORDINADOR_CARRERA_SEDE', 40, 4, 4, 4, 16, 20, null, 'DURA', 'POL 6 (coordinadores de campus, extensión o sede y carrera)', null),
    (8, 'T1', 'GESTOR', 40, 16, 16, 16, 16, 8, null, 'DURA', 'POL 6 (docentes gestores)', null),
    (9, 'T2', 'GESTOR', 40, 14, 14, 14, 16, 10, null, 'DURA', 'POL 6 (docentes gestores)', null),
    (10, 'T3', 'GESTOR', 40, 12, 12, 12, 16, 12, null, 'DURA', 'POL 6 (docentes gestores)', null),
    (11, 'T4', 'GESTOR', 40, 10, 10, 10, 16, 14, null, 'DURA', 'POL 6 (docentes gestores)', null),
    (12, 'T5', 'GESTOR', 40, 8, 8, 8, 16, 16, null, 'DURA', 'POL 6 (docentes gestores)', null),
    (13, 'TODOS', 'COORDINADOR_ACADEMICO_INSTITUCIONAL', 40, 6, 6, 6, 4, 30, null, 'DURA', 'POL 5.4.1 y 6', 'Solo T4–T5, o T1–T3 con más de 500 estudiantes o 3 o más campus'),
    (14, 'T1', 'RECTOR_VICERRECTOR', 40, 8, 3, 12, 0, 32, null, 'DURA', 'POL 6; RCE art. 152', 'Interpretación: el resto de la dedicación es gestión'),
    (15, 'T2', 'RECTOR_VICERRECTOR', 40, 6, 3, 12, 0, 34, null, 'DURA', 'POL 6; RCE art. 152', 'Interpretación: el resto de la dedicación es gestión'),
    (16, 'T3', 'RECTOR_VICERRECTOR', 40, 5, 3, 12, 0, 35, null, 'DURA', 'POL 6; RCE art. 152', 'Interpretación: el resto de la dedicación es gestión'),
    (17, 'T4', 'RECTOR_VICERRECTOR', 40, 4, 3, 12, 0, 36, null, 'DURA', 'POL 6; RCE art. 152', 'Interpretación: el resto de la dedicación es gestión'),
    (18, 'T5', 'RECTOR_VICERRECTOR', 40, 3, 3, 12, 0, 37, null, 'DURA', 'POL 6; RCE art. 152', 'Interpretación: el resto de la dedicación es gestión'),
    (19, 'TODOS', 'SECRETARIA_GENERAL_ACADEMICA', 40, 4, 3, 5, 0, 36, null, 'BLANDA', 'POL 6', 'Interpretación del reparto entre otras de docencia y gestión'),
    (20, 'TODOS', 'DOCENTE_PROYECTO_NACIONAL_SIES', 40, 4, 3, 5, 0, 36, null, 'BLANDA', 'POL 5.5 y 6', 'La carga de gestión la definen la máxima autoridad y la SIES'),
    (21, 'TODOS', 'DOCENTE_INVESTIGACION_VINCULACION', 40, 16, 0, 16, null, null, 24, 'DURA', 'POL 6', 'Máximo 16 h de docencia con proyecto aprobado por el OCS; 10 % de la nómina'),
    (22, 'TODOS', 'CUIDADOR_SUSTITUTO', 40, 18, 18, 18, 12, 0, null, 'DURA', 'POL 5.7', '18 h clase + 12 h otras de docencia')
) as f (orden, aplica_a, perfil, dedicacion_total, horas_clase, horas_clase_minimo,
    horas_clase_maximo, horas_otras_docencia, horas_gestion, horas_inv_vinc_maximo, severidad,
    referencia, observacion)
where v.numero = 1;

-- Las reglas de RN-01 a RN-15 que las filas no expresan; RN-01 y RN-06 son solo parámetros.
insert into regla (version_id, identificador, severidad)
select v.id, r.identificador, r.severidad
from version_normativa v
cross join (values
    ('RN-01', null),
    ('RN-02', 'DURA'),
    ('RN-05', 'BLANDA'),
    ('RN-06', null),
    ('RN-09', 'BLANDA'),
    ('RN-10', 'BLANDA'),
    ('RN-13', 'DURA')
) as r (identificador, severidad)
where v.numero = 1;

insert into parametro_regla (version_id, regla, nombre, valor)
select v.id, p.regla, p.nombre, p.valor
from version_normativa v
cross join (values
    -- RN-01: dedicaciones (POL 4.4, RCE art. 144).
    ('RN-01', 'dedicacion_tiempo_completo', 40),
    ('RN-01', 'dedicacion_medio_tiempo', 20),
    ('RN-01', 'dedicacion_tiempo_parcial_maxima', 19),
    -- RN-02: el total de un docente nunca supera 40 h (RCE arts. 149 y 153).
    ('RN-02', 'horas_maximas_docente', 40),
    -- RN-05: proporción de clase y de otras actividades de docencia (RCE art. 149).
    ('RN-05', 'porcentaje_clase', 60),
    ('RN-05', 'porcentaje_otras_docencia', 40),
    -- RN-06: matriculados de cada tipo de IST (POL 6).
    ('RN-06', 't1_desde', 1),
    ('RN-06', 't1_hasta', 300),
    ('RN-06', 't2_desde', 301),
    ('RN-06', 't2_hasta', 600),
    ('RN-06', 't3_desde', 601),
    ('RN-06', 't3_hasta', 1200),
    ('RN-06', 't4_desde', 1201),
    ('RN-06', 't4_hasta', 1800),
    ('RN-06', 't5_desde', 1801),
    ('RN-06', 't5_hasta', 3500),
    -- RN-09: en T1 a T3, el coordinador académico institucional exige más de 500 estudiantes o 3
    -- o más campus (POL 5.4.1).
    ('RN-09', 'estudiantes_minimos', 500),
    ('RN-09', 'campus_minimos', 3),
    -- RN-10: docentes con cargo de gestión por tipo de IST (POL 6).
    ('RN-10', 't1_minimo', 5),
    ('RN-10', 't1_maximo', 7),
    ('RN-10', 't2_minimo', 8),
    ('RN-10', 't2_maximo', 11),
    ('RN-10', 't3_minimo', 12),
    ('RN-10', 't3_maximo', 15),
    ('RN-10', 't4_minimo', 15),
    ('RN-10', 't4_maximo', 18),
    ('RN-10', 't5_minimo', 21),
    ('RN-10', 't5_maximo', 25),
    -- RN-13: la parte de la nómina para investigación y vinculación (POL 6); las 16 h de docencia
    -- están en su fila.
    ('RN-13', 'porcentaje_nomina', 10)
) as p (regla, nombre, valor)
where v.numero = 1;

insert into registro_cambio (id, momento, usuario, accion, detalle)
select gen_random_uuid(), now(), 'sistema', 'VERSION_CREADA',
    jsonb_build_object('numero', v.numero, 'codigo', v.codigo, 'origen', v.origen)
from version_normativa v
where v.numero = 1;
