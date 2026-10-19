-- D-67 (F04-H02): el Vicerrector académico escribe «Parámetros y normativa»: crea versiones de
-- normativa, las edita mientras no rigen un periodo y las asocia a un periodo. Cambia la matriz
-- D-16 de D-65 y D-66, así que va en una migración nueva (D-63).
insert into rol_permiso (rol_id, funcionalidad, tipo, alcance)
select r.id, 'PARAMETROS', 'ESCRITURA', 'TODO'
from rol r
where r.identificador = 'VICERRECTOR';
