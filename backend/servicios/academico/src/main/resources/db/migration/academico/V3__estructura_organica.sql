-- La estructura orgánica de la institución (F03-H01, RF-12) y el catálogo de las 21 gestiones
-- autorizadas de POL 6 (RN-10, V-02, D-07).
-- El catálogo es un dato precargado: ninguna operación lo cambia, y si la norma cambia se amplía con
-- una migración nueva. Así se interpreta el criterio de RF-12 «sin permiso de administrador»: nadie
-- registra una gestión fuera de las 21, tampoco el Administrador (D-70).
-- Las comisiones son un dato informativo, sin horas ni gestión autorizada (C-12). Ninguna unidad se
-- borra: se inactiva (principio VII).

create table gestion_autorizada (
    codigo varchar(3) primary key check (codigo ~ '^G(0[1-9]|1[0-9]|2[01])$'),
    nombre varchar(150) not null
);

insert into gestion_autorizada (codigo, nombre) values
    ('G01', 'Rector'),
    ('G02', 'Vicerrector'),
    ('G03', 'Coordinador Académico Institucional'),
    ('G04', 'Coordinador de Sede, Extensión o Campus'),
    ('G05', 'Coordinadores de Carrera'),
    ('G06', 'Gestor de Vinculación con la Sociedad'),
    ('G07', 'Gestor de Investigación, Desarrollo Tecnológico e Innovación'),
    ('G08', 'Gestor de Centro de Idiomas'),
    ('G09', 'Gestor de Centro de Formación Integral y Servicios Especializados'),
    ('G10', 'Coordinador de Aseguramiento de la Calidad'),
    ('G11', 'Secretario General Académico'),
    ('G12', 'Procurador'),
    ('G13', 'Gestor de Bienestar Institucional'),
    ('G14', 'Gestor Administrativo Financiero'),
    ('G15', 'Gestor de Planificación Estratégica'),
    ('G16', 'Gestor Unidad de Talento Humano'),
    ('G17', 'Gestor Unidad de Servicios de Biblioteca'),
    ('G18', 'Gestor Unidad de Comunicación'),
    ('G19', 'Gestor Unidad de Relaciones Internacionales e Institucionales'),
    ('G20', 'Gestor Unidad de TIC'),
    ('G21', 'Gestor Unidad de Estadísticas');

create table unidad_organica (
    codigo varchar(20) primary key check (codigo ~ '^[A-Z0-9_-]{1,20}$'),
    nombre varchar(150) not null check (length(trim(nombre)) > 0),
    tipo varchar(20) not null
        check (tipo in ('SEDE_MATRIZ', 'SEDE', 'EXTENSION', 'CAMPUS', 'GOBIERNO',
                        'UNIDAD_ACADEMICA', 'GESTION', 'COMISION')),
    padre varchar(20) references unidad_organica (codigo),
    gestion_autorizada varchar(3) references gestion_autorizada (codigo),
    comision_permanente boolean,
    activa boolean not null,
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null,
    -- Solo la sede matriz no depende de otra unidad.
    check ((tipo = 'SEDE_MATRIZ') = (padre is null)),
    -- Una gestión representa una gestión autorizada; una comisión, nunca; los demás, si quieren.
    check (tipo <> 'GESTION' or gestion_autorizada is not null),
    check (tipo <> 'COMISION' or gestion_autorizada is null),
    -- Solo una comisión dice si es permanente, y siempre lo dice.
    check ((tipo = 'COMISION') = (comision_permanente is not null))
);

-- Una sola sede matriz.
create unique index una_sede_matriz on unidad_organica ((true)) where tipo = 'SEDE_MATRIZ';

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check
    check (accion in ('PERIODO_REGISTRADO', 'PERIODO_CORREGIDO', 'PERIODO_ABIERTO',
                      'PERIODO_ESTADO_CAMBIADO', 'UNIDAD_REGISTRADA', 'UNIDAD_CORREGIDA',
                      'UNIDAD_INACTIVADA', 'UNIDAD_REACTIVADA'));
