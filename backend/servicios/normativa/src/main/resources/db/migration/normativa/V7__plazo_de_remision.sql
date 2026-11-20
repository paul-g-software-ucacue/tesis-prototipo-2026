-- Los días antes del inicio del periodo en que se remite el distributivo aprobado por el OCS (RN-23,
-- POL 5.2; F12-H03, D-114). Aprobación los lee para advertir, al cargar la configuración del flujo,
-- si los plazos no dejan ese margen. RN-23 no tiene severidad: solo da un parámetro, como RN-24.
-- Se agrega a cada versión que no la tiene, aunque rija un periodo, con su registro: completa la
-- versión, no cambia un valor (D-68). Una versión nueva copia los parámetros de la anterior
-- (RepositorioVersionesJdbc).

insert into regla (version_id, identificador, severidad)
select v.id, 'RN-23', null
from version_normativa v
where not exists (select 1 from regla r
                  where r.version_id = v.id and r.identificador = 'RN-23');

-- Las versiones que todavía no tienen la cifra: son las que se completan y se registran.
create temporary table sin_remision (version_id uuid, numero integer) on commit drop;

insert into sin_remision
select v.id, v.numero
from version_normativa v
where not exists (select 1 from parametro_regla p
                  where p.version_id = v.id and p.regla = 'RN-23'
                    and p.nombre = 'dias_antes_del_inicio');

insert into parametro_regla (version_id, regla, nombre, valor)
select version_id, 'RN-23', 'dias_antes_del_inicio', 30
from sin_remision;

-- Un VERSION_EDITADA de sistema por versión completada, como en V4, V5 y V6 (D-68).
insert into registro_cambio (id, momento, usuario, accion, detalle)
select gen_random_uuid(), now(), 'sistema', 'VERSION_EDITADA',
    jsonb_build_object(
        'numero', numero,
        'cambios', jsonb_build_array(
            jsonb_build_object('campo', 'RN-23.dias_antes_del_inicio', 'anterior', null,
                               'nuevo', '30')))
from sin_remision;
