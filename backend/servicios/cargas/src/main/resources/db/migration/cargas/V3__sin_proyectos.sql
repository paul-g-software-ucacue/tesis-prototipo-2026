-- La confirmación «sin proyectos» (F01-H05, RF-18, glosario «Confirmación "sin proyectos"»): un periodo
-- sin proyectos aprobados asignables se confirma de forma explícita y queda en el historial de la
-- carga de proyectos, con el usuario y el momento, sin archivo ni hash. Nunca se borra (principio VII):
-- si después el periodo tiene proyectos asignables, deja de valer, pero su entrada se queda.
alter table cargas.historial_carga
    add column periodo varchar(10) check (periodo ~ '^[0-9]{4}-(I|II)$');

alter table cargas.historial_carga drop constraint historial_carga_resultado_check;
alter table cargas.historial_carga alter column resultado type varchar(13);
alter table cargas.historial_carga add constraint historial_carga_resultado_check
    check (resultado in ('APLICADA', 'RECHAZADA', 'REPETIDA', 'SIN_PROYECTOS'));

-- Solo la confirmación «sin proyectos» tiene periodo, y solo ella no tiene archivo ni hash.
alter table cargas.historial_carga alter column archivo drop not null;
alter table cargas.historial_carga alter column hash drop not null;
alter table cargas.historial_carga add constraint historial_carga_sin_proyectos_check
    check ((resultado = 'SIN_PROYECTOS') = (periodo is not null)
           and (resultado = 'SIN_PROYECTOS') = (archivo is null)
           and (resultado = 'SIN_PROYECTOS') = (hash is null));

-- Un periodo se confirma «sin proyectos» una sola vez por carga.
create unique index historial_carga_sin_proyectos on cargas.historial_carga (carga, periodo)
    where resultado = 'SIN_PROYECTOS';
