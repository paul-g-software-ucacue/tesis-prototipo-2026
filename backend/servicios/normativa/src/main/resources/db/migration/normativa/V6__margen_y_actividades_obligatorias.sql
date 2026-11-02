-- Lo que el panel de inconsistencias lee de cada versión (F10-H01, RN-05, D-86): el margen de 60/40
-- y las actividades que tienen que constar en la carga de quien da clases. Se agrega a cada versión,
-- aunque rija un periodo, con su registro: completa la versión, no cambia un valor (D-68). Una
-- versión nueva copia el catálogo (RepositorioVersionesJdbc) y los parámetros de la anterior.

alter table actividad_catalogo add column obligatoria boolean not null default false;

update actividad_catalogo set obligatoria = true where codigo in ('140-b', '140-g');

insert into parametro_regla (version_id, regla, nombre, valor)
select v.id, 'RN-05', 'margen_puntos', 5
from version_normativa v
where not exists (select 1 from parametro_regla p
                  where p.version_id = v.id and p.regla = 'RN-05'
                    and p.nombre = 'margen_puntos');

-- Un VERSION_EDITADA de sistema por versión completada, como en V4 y V5 (D-68).
insert into registro_cambio (id, momento, usuario, accion, detalle)
select gen_random_uuid(), now(), 'sistema', 'VERSION_EDITADA',
    jsonb_build_object(
        'numero', v.numero,
        'cambios', jsonb_build_array(
            jsonb_build_object('campo', 'RN-05.margen_puntos', 'anterior', null, 'nuevo', '5'),
            jsonb_build_object('campo', 'obligatorias', 'anterior', null,
                               'nuevo', '140-b, 140-g')))
from version_normativa v;
