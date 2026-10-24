-- Las carreras, sus mallas y sus asignaturas (F03-H02, RF-12, RF-14, cargas 2 y 4), con los datos
-- para la afinidad: el campo de conocimiento CINE, las palabras clave y el tronco común (RN-19,
-- RN-24). Los 11 campos amplios de la CINE son un dato precargado, los del catálogo del
-- diccionario de datos. Una malla tiene su código de versión y su estado (aclaración 2); cada
-- asignatura es de una malla de su misma carrera. Nada se borra (principio VII).

create table campo_amplio (
    codigo char(2) primary key check (codigo ~ '^(0[0-9]|10)$'),
    nombre varchar(80) not null
);

insert into campo_amplio (codigo, nombre) values
    ('00', 'Programas genéricos'),
    ('01', 'Educación'),
    ('02', 'Artes y humanidades'),
    ('03', 'Ciencias sociales, periodismo e información'),
    ('04', 'Administración de empresas y derecho'),
    ('05', 'Ciencias naturales, matemáticas y estadística'),
    ('06', 'Tecnologías de la información y la comunicación'),
    ('07', 'Ingeniería, industria y construcción'),
    ('08', 'Agricultura, silvicultura, pesca y veterinaria'),
    ('09', 'Salud y bienestar'),
    ('10', 'Servicios');

create table carrera (
    codigo varchar(20) primary key check (codigo ~ '^[0-9A-Z-]{1,20}$'),
    nombre varchar(150) not null check (length(trim(nombre)) > 0),
    siglas varchar(10) not null unique check (siglas ~ '^[0-9A-Z]{1,10}$'),
    sede varchar(20) not null references unidad_organica (codigo),
    nivel_formacion varchar(40) not null
        check (nivel_formacion in ('TECNICO_SUPERIOR', 'TECNOLOGICO_SUPERIOR',
                                   'TECNOLOGICO_SUPERIOR_UNIVERSITARIO')),
    modalidad varchar(15) not null
        check (modalidad in ('PRESENCIAL', 'SEMIPRESENCIAL', 'EN_LINEA', 'A_DISTANCIA', 'DUAL',
                             'HIBRIDA')),
    -- Al menos una jornada, cada una de las cuatro.
    jornadas varchar(15)[] not null
        check (cardinality(jornadas) between 1 and 4
               and jornadas <@ array['MATUTINA', 'VESPERTINA', 'NOCTURNA', 'INTENSIVA']::varchar(15)[]),
    campo_amplio char(2) not null references campo_amplio (codigo),
    campo_especifico char(3) not null
        check (campo_especifico ~ '^[0-9]{3}$' and left(campo_especifico, 2) = campo_amplio),
    campo_detallado char(4)
        check (campo_detallado ~ '^[0-9]{4}$' and left(campo_detallado, 3) = campo_especifico),
    numero_ciclos smallint not null check (numero_ciclos between 1 and 10),
    estado varchar(12) not null check (estado in ('VIGENTE', 'NO_VIGENTE', 'EN_CIERRE')),
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null
);

create table malla (
    codigo varchar(20) primary key check (codigo ~ '^[0-9A-Z-]{1,20}$'),
    carrera varchar(20) not null references carrera (codigo),
    estado varchar(10) not null check (estado in ('VIGENTE', 'DEROGADA')),
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null,
    -- Para que una asignatura apunte a una malla de su misma carrera.
    unique (codigo, carrera)
);

create table asignatura (
    codigo varchar(30) primary key check (codigo ~ '^[0-9A-Z-]{1,30}$'),
    carrera varchar(20) not null,
    malla varchar(20) not null,
    nombre varchar(150) not null check (length(trim(nombre)) > 0),
    ciclo smallint not null check (ciclo between 1 and 10),
    unidad_organizacion_curricular varchar(25) not null
        check (unidad_organizacion_curricular in ('BASICA', 'PROFESIONAL',
                                                  'INTEGRACION_CURRICULAR')),
    horas_semanales_clase numeric(4, 2) not null check (horas_semanales_clase between 0.5 and 20),
    horas_practicas_semanales numeric(4, 2) check (horas_practicas_semanales between 0 and 20),
    creditos numeric(4, 2) check (creditos > 0),
    tronco_comun boolean not null,
    campo_amplio char(2) not null references campo_amplio (codigo),
    campo_especifico char(3) not null
        check (campo_especifico ~ '^[0-9]{3}$' and left(campo_especifico, 2) = campo_amplio),
    palabras_clave varchar(300)[] not null check (cardinality(palabras_clave) >= 2),
    activa boolean not null,
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null,
    foreign key (malla, carrera) references malla (codigo, carrera)
);

create index asignatura_por_carrera on asignatura (carrera);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check
    check (accion in ('PERIODO_REGISTRADO', 'PERIODO_CORREGIDO', 'PERIODO_ABIERTO',
                      'PERIODO_ESTADO_CAMBIADO', 'UNIDAD_REGISTRADA', 'UNIDAD_CORREGIDA',
                      'UNIDAD_INACTIVADA', 'UNIDAD_REACTIVADA', 'CARRERA_REGISTRADA',
                      'CARRERA_CORREGIDA', 'CARRERA_ESTADO_CAMBIADO', 'MALLA_REGISTRADA',
                      'MALLA_ESTADO_CAMBIADO', 'ASIGNATURA_REGISTRADA', 'ASIGNATURA_CORREGIDA',
                      'ASIGNATURA_INACTIVADA', 'ASIGNATURA_REACTIVADA'));
