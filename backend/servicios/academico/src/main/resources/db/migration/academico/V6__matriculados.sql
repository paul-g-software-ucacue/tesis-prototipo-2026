-- Los matriculados por carrera y periodo (F01-H04, carga 10, glosario «Matriculados»): totales, sin
-- datos personales (RNF-SEG-06). Entran solo por la carga 10; el tipo de IST por matriculados es F23.
create table matricula (
    periodo varchar(20) not null references periodo_academico (codigo),
    carrera varchar(20) not null references carrera (codigo),
    total integer not null check (total between 0 and 99999),
    fecha_corte date not null,
    fuente varchar(60) check (fuente is null or length(fuente) > 0),
    registrada_en timestamptz not null,
    registrada_por varchar(64) not null,
    primary key (periodo, carrera)
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
                      'ASIGNATURA_PARALELO_RETIRADA', 'ASIGNATURA_PARALELO_REOFERTADA',
                      'MATRICULA_REGISTRADA', 'MATRICULA_CORREGIDA'));
