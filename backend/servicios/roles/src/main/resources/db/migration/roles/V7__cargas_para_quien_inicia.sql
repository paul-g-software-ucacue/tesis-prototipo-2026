-- D-71 (F01-H02): el Responsable de consolidación y el Vicerrector académico ejecutan las cargas,
-- como el rol que inicia el proceso de construcción del distributivo (ERS §8.3); Talento Humano
-- las conserva. Cambia la matriz D-16 de D-65, así que va en una migración nueva (D-63).
insert into rol_permiso (rol_id, funcionalidad, tipo, alcance)
select r.id, p.funcionalidad, p.tipo, 'TODO'
from rol r
cross join (values ('CARGAS', 'LECTURA'), ('CARGAS', 'ESCRITURA')) as p (funcionalidad, tipo)
where r.identificador in ('RESPONSABLE_CONSOLIDACION', 'VICERRECTOR');
