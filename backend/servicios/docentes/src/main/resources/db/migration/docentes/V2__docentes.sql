-- Los docentes con su ficha académica (F03-H04, RF-16, RF-17, carga 6). La identificación es la
-- clave y no cambia; el correo institucional es único sin distinguir mayúsculas. La ficha vive en
-- la fila del docente (RF-17): el título de tercer nivel en columnas y las cuatro listas en jsonb,
-- que el dominio valida antes de guardar (research §2). La carrera base y las asignaturas afines
-- son códigos de Académico: sin clave foránea entre esquemas (principio V). Que las horas
-- contratadas cuadren con la dedicación (RN-01) es de la normativa del periodo, no de esta tabla.
-- Nada se borra: un docente cambia de estado laboral (principio VII).

create table docente (
    identificacion varchar(13) primary key check (identificacion ~ '^[0-9A-Z]{1,13}$'),
    tipo_identificacion varchar(10) not null check (tipo_identificacion in ('CEDULA', 'PASAPORTE')),
    primer_apellido varchar(60) not null check (length(trim(primer_apellido)) > 0),
    segundo_apellido varchar(60),
    primer_nombre varchar(60) not null check (length(trim(primer_nombre)) > 0),
    segundo_nombre varchar(60),
    correo_institucional varchar(100) not null,
    telefono varchar(10) check (telefono ~ '^[0-9]{7,10}$'),
    tipo_personal varchar(10) not null
        check (tipo_personal in ('TITULAR', 'OCASIONAL', 'INVITADO', 'HONORARIO')),
    categoria_escalafon varchar(12) not null
        check (categoria_escalafon in ('AUXILIAR_1', 'AUXILIAR_2', 'AGREGADO_1', 'AGREGADO_2',
                                       'AGREGADO_3', 'PRINCIPAL_1', 'PRINCIPAL_2', 'PRINCIPAL_3',
                                       'NO_APLICA')),
    dedicacion char(2) not null check (dedicacion in ('TC', 'MT', 'TP')),
    horas_contratadas smallint not null check (horas_contratadas between 1 and 40),
    fecha_ingreso date not null,
    inicio_contrato date not null,
    fin_contrato date check (fin_contrato > inicio_contrato),
    condicion_especial varchar(25) not null
        check (condicion_especial in ('CUIDADOR_SUSTITUTO', 'MATERNIDAD_LACTANCIA', 'APOYO_SIES',
                                      'NINGUNA')),
    carrera_base varchar(20) not null check (carrera_base ~ '^[0-9A-Z-]{1,20}$'),
    titulo_tercer_nivel varchar(150) not null check (length(trim(titulo_tercer_nivel)) > 0),
    registro_senescyt varchar(30),
    campo_amplio char(2) not null check (campo_amplio ~ '^[0-9]{2}$'),
    campo_especifico char(3) not null
        check (campo_especifico ~ '^[0-9]{3}$' and left(campo_especifico, 2) = campo_amplio),
    titulos_cuarto_nivel jsonb not null default '[]' check (jsonb_typeof(titulos_cuarto_nivel) = 'array'),
    cursos jsonb not null default '[]' check (jsonb_typeof(cursos) = 'array'),
    experiencia jsonb not null default '[]' check (jsonb_typeof(experiencia) = 'array'),
    asignaturas_afines jsonb not null default '[]' check (jsonb_typeof(asignaturas_afines) = 'array'),
    estado varchar(10) not null check (estado in ('ACTIVO', 'LICENCIA', 'INACTIVO')),
    registrado_en timestamptz not null,
    registrado_por varchar(64) not null
);

create unique index docente_correo_unico on docente (lower(correo_institucional));

-- Quién, cuándo y qué cambió, con el valor anterior y el nuevo (FR-010, principio VII). El detalle
-- puede llevar datos personales del docente: vive en el esquema del servicio y no sale por la API.
create table registro_cambio (
    id uuid primary key,
    momento timestamptz not null,
    usuario varchar(64) not null,
    accion varchar(30) not null
        check (accion in ('DOCENTE_REGISTRADO', 'DOCENTE_CORREGIDO', 'DOCENTE_ESTADO_CAMBIADO')),
    detalle jsonb not null
);
