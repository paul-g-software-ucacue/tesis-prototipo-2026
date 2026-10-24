-- La oferta del periodo (F03-H03, RF-15, carga 5 del diccionario de datos): las asignaturas-paralelo
-- de cada periodo, con su jornada, su modalidad, sus estudiantes previstos y, si se indican, su sede
-- y sus horas. Sin sede ni horas, son las de la carrera y las de la asignatura, que se leen al
-- consultar: no se copian (research §2). Que las horas no pasen las de la asignatura (RN-18), que la
-- jornada sea de la carrera y que el periodo esté en Planificación lo decide el dominio.
-- Nada se borra: una asignatura-paralelo se retira (principio VII). Sin las franjas horarias
-- (RF-60, Should).

create table asignatura_paralelo (
    periodo varchar(20) not null references periodo_academico (codigo),
    asignatura varchar(30) not null references asignatura (codigo),
    paralelo varchar(20) not null check (paralelo ~ '^[0-9A-Z_-]{1,20}$'),
    jornada varchar(15) not null
        check (jornada in ('MATUTINA', 'VESPERTINA', 'NOCTURNA', 'INTENSIVA')),
    modalidad varchar(15) not null
        check (modalidad in ('PRESENCIAL', 'SEMIPRESENCIAL', 'EN_LINEA', 'A_DISTANCIA', 'DUAL',
                             'HIBRIDA')),
    sede varchar(20) references unidad_organica (codigo),
    estudiantes integer not null check (estudiantes between 0 and 9999),
    horas_semanales numeric(4, 2) check (horas_semanales between 0.5 and 20),
    estado varchar(10) not null check (estado in ('OFERTADA', 'RETIRADA')),
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null,
    primary key (periodo, asignatura, paralelo)
);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check
    check (accion in ('PERIODO_REGISTRADO', 'PERIODO_CORREGIDO', 'PERIODO_ABIERTO',
                      'PERIODO_ESTADO_CAMBIADO', 'UNIDAD_REGISTRADA', 'UNIDAD_CORREGIDA',
                      'UNIDAD_INACTIVADA', 'UNIDAD_REACTIVADA', 'CARRERA_REGISTRADA',
                      'CARRERA_CORREGIDA', 'CARRERA_ESTADO_CAMBIADO', 'MALLA_REGISTRADA',
                      'MALLA_ESTADO_CAMBIADO', 'ASIGNATURA_REGISTRADA', 'ASIGNATURA_CORREGIDA',
                      'ASIGNATURA_INACTIVADA', 'ASIGNATURA_REACTIVADA',
                      'ASIGNATURA_PARALELO_REGISTRADA', 'ASIGNATURA_PARALELO_CORREGIDA',
                      'ASIGNATURA_PARALELO_RETIRADA', 'ASIGNATURA_PARALELO_REOFERTADA'));
