-- Los cargos (carga 8) y los proyectos aprobados (carga 9, RF-18) de F03-H05. El dominio valida
-- cada campo antes de guardar (principio XIV); estas reglas protegen lo mismo en la base. La unidad,
-- la carrera y las carreras del proyecto son códigos de Académico: sin clave foránea entre esquemas
-- (principio V). El docente del cargo y el director del proyecto sí la tienen, porque están en este
-- esquema. Nada se borra: un cargo se finaliza y un proyecto cambia de estado (principio VII).

create table cargo (
    docente varchar(13) not null references docente (identificacion),
    tipo varchar(40) not null
        check (tipo in ('RECTOR', 'VICERRECTOR', 'COORDINADOR_ACADEMICO_INSTITUCIONAL',
                        'COORDINADOR_SEDE', 'COORDINADOR_CARRERA', 'G06_GESTOR_VINCULACION',
                        'G07_GESTOR_INVESTIGACION', 'G08_GESTOR_IDIOMAS',
                        'G09_GESTOR_FORMACION_INTEGRAL', 'G10_COORDINADOR_CALIDAD',
                        'G11_SECRETARIO_GENERAL', 'G12_PROCURADOR', 'G13_GESTOR_BIENESTAR',
                        'G14_GESTOR_ADMINISTRATIVO_FINANCIERO', 'G15_GESTOR_PLANIFICACION',
                        'G16_GESTOR_TALENTO_HUMANO', 'G17_GESTOR_BIBLIOTECA',
                        'G18_GESTOR_COMUNICACION', 'G19_GESTOR_RELACIONES_INTERNACIONALES',
                        'G20_GESTOR_TIC', 'G21_GESTOR_ESTADISTICAS', 'MIEMBRO_COMISION')),
    fecha_designacion date not null,
    unidad varchar(20) check (unidad ~ '^[A-Z0-9_-]{1,20}$'),
    carrera varchar(20) check (carrera ~ '^[0-9A-Z-]{1,20}$'),
    -- Hasta dos años por designación (POL 5.4).
    fecha_fin date check (fecha_fin > fecha_designacion
                          and fecha_fin <= fecha_designacion + interval '2 years'),
    documento varchar(60) not null check (length(trim(documento)) > 0),
    estado varchar(10) not null check (estado in ('VIGENTE', 'FINALIZADO')),
    registrado_en timestamptz not null,
    registrado_por varchar(64) not null,
    primary key (docente, tipo, fecha_designacion),
    -- El coordinador de carrera tiene su carrera; los demás, su unidad y ninguna carrera.
    check ((tipo = 'COORDINADOR_CARRERA' and carrera is not null)
           or (tipo <> 'COORDINADOR_CARRERA' and unidad is not null and carrera is null))
);

-- Un solo coordinador vigente por carrera (RN-12).
create unique index cargo_coordinador_vigente on cargo (carrera)
    where tipo = 'COORDINADOR_CARRERA' and estado = 'VIGENTE';

create table proyecto (
    codigo varchar(30) primary key check (codigo ~ '^[0-9A-Z-]{1,30}$'),
    nombre varchar(250) not null check (length(trim(nombre)) > 0),
    tipo varchar(30) not null
        check (tipo in ('INVESTIGACION', 'VINCULACION', 'PRACTICAS_PREPROFESIONALES',
                        'SERVICIO_COMUNITARIO')),
    carreras jsonb not null check (jsonb_typeof(carreras) = 'array' and jsonb_array_length(carreras) > 0),
    resolucion_ocs varchar(60) not null check (length(trim(resolucion_ocs)) > 0),
    fecha_resolucion date not null,
    estado varchar(15) not null
        check (estado in ('APROBADO', 'EN_EJECUCION', 'FINALIZADO', 'SUSPENDIDO')),
    vigencia_inicio date not null,
    vigencia_fin date not null check (vigencia_fin > vigencia_inicio),
    director varchar(13) not null references docente (identificacion),
    -- [{"docente": "...", "rol": "...", "horas": 14}, ...]
    participantes jsonb not null
        check (jsonb_typeof(participantes) = 'array' and jsonb_array_length(participantes) > 0),
    -- Hasta dos decimales y 5 caracteres (aclaración 7); los 5 caracteres los revisa el dominio.
    horas_aprobadas numeric(5, 2) not null check (horas_aprobadas > 0 and horas_aprobadas < 1000),
    registrado_en timestamptz not null,
    registrado_por varchar(64) not null
);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check
    check (accion in ('DOCENTE_REGISTRADO', 'DOCENTE_CORREGIDO', 'DOCENTE_ESTADO_CAMBIADO',
                      'CARGO_REGISTRADO', 'CARGO_CORREGIDO', 'PROYECTO_REGISTRADO',
                      'PROYECTO_CORREGIDO'));
