-- D-66 (F04-H01): el Vicerrector académico lee «Parámetros y normativa», la normativa que elige
-- para su periodo. Cambia la matriz D-16 de D-65, así que va en una migración nueva (D-63). La
-- escritura sigue solo en el Administrador del sistema.
insert into rol_permiso (rol_id, funcionalidad, tipo, alcance)
select r.id, 'PARAMETROS', 'LECTURA', 'TODO'
from rol r
where r.identificador = 'VICERRECTOR';
