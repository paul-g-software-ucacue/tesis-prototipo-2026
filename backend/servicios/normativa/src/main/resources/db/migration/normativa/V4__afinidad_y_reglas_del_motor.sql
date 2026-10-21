-- Los pesos y el mínimo de afinidad y las reglas del motor que faltaban (F04-H03, RN-19, RN-24,
-- MD-08). Los pesos son los parámetros de RN-24, que no tiene severidad; el mínimo, el de RN-19.
-- Las reglas del motor llevan la severidad por defecto de la tabla 5.2 del modelo de dominio; las
-- marcadas (I) se confirman en F07 y F10. Los valores que la ERS no da (el tope de los cursos, el
-- mínimo y los pesos del campo amplio) los fijó el autor (specs/014-afinidad-severidad/spec.md).
-- Estas cifras solo viven aquí (principio VI).
-- Se agregan a cada versión que no las tiene, aunque rija un periodo, con su registro: completan la
-- versión, no cambian un valor (D-68). La versión anterior del servicio, que conoce V1 a V3, sigue
-- arrancando: no ve las reglas sin parámetros (D-55, research §6).

alter table regla drop constraint regla_identificador_check;
alter table regla add constraint regla_identificador_check
    check (identificador ~ '^R[NF]-[0-9]{2}$');

create temporary table regla_por_defecto (identificador varchar(6), severidad varchar(6))
    on commit drop;
insert into regla_por_defecto values
    ('RN-12', 'DURA'),    -- un coordinador de carrera por carrera (I)
    ('RN-16', 'BLANDA'),  -- reemplazo con maternidad o lactancia (I)
    ('RN-17', 'DURA'),    -- ningún docente sin horas de clase (I)
    ('RN-18', 'DURA'),    -- las horas de clase de una carrera no exceden su malla (I)
    ('RN-19', 'BLANDA'),  -- afinidad bajo el mínimo, salvo tronco común: exige justificación
    ('RN-22', 'DURA'),    -- solo actividades del catálogo
    ('RN-24', null),      -- pesos de afinidad: solo parámetros
    ('RN-25', 'DURA'),    -- investigación o vinculación solo con un proyecto aprobado y vigente
    ('RF-24', 'DURA');    -- asignatura-paralelo de la oferta sin propuesta (I)

create temporary table parametro_por_defecto (regla varchar(6), nombre varchar(60), valor numeric(10, 2))
    on commit drop;
insert into parametro_por_defecto values
    -- RN-24: un título suma más si coincide el campo específico que si solo coincide el amplio.
    ('RN-24', 'cuarto_nivel_campo_especifico', 3),
    ('RN-24', 'cuarto_nivel_campo_amplio', 2),
    ('RN-24', 'tercer_nivel_campo_especifico', 2),
    ('RN-24', 'tercer_nivel_campo_amplio', 1),
    ('RN-24', 'experiencia_relacionada', 2),
    ('RN-24', 'curso_relacionado', 1),
    ('RN-24', 'tope_cursos', 3),
    -- RN-19: el puntaje desde el cual se sugiere un docente sin justificación.
    ('RN-19', 'minimo_afinidad', 2);

-- Lo que le falta a cada versión, antes de agregarlo: es lo que se registra.
create temporary table agregado (version_id uuid, campo text, nuevo text) on commit drop;

insert into agregado
select v.id, 'reglas[' || d.identificador || '].severidad', d.severidad
from version_normativa v
cross join regla_por_defecto d
where d.severidad is not null
  and not exists (select 1 from regla r
                  where r.version_id = v.id and r.identificador = d.identificador);

insert into agregado
select v.id, 'reglas[' || d.regla || '].' || d.nombre, trim_scale(d.valor)::text
from version_normativa v
cross join parametro_por_defecto d
where not exists (select 1 from parametro_regla p
                  where p.version_id = v.id and p.regla = d.regla and p.nombre = d.nombre);

insert into regla (version_id, identificador, severidad)
select v.id, d.identificador, d.severidad
from version_normativa v
cross join regla_por_defecto d
where not exists (select 1 from regla r
                  where r.version_id = v.id and r.identificador = d.identificador);

insert into parametro_regla (version_id, regla, nombre, valor)
select v.id, d.regla, d.nombre, d.valor
from version_normativa v
cross join parametro_por_defecto d
where not exists (select 1 from parametro_regla p
                  where p.version_id = v.id and p.regla = d.regla and p.nombre = d.nombre);

-- Un VERSION_EDITADA de sistema por versión completada, como los de EditarVersionServicio.
insert into registro_cambio (id, momento, usuario, accion, detalle)
select gen_random_uuid(), now(), 'sistema', 'VERSION_EDITADA',
    jsonb_build_object(
        'numero', v.numero,
        'cambios', jsonb_agg(
            jsonb_build_object('campo', a.campo, 'anterior', null, 'nuevo', a.nuevo)
            order by a.campo))
from agregado a
join version_normativa v on v.id = a.version_id
group by v.id, v.numero;
