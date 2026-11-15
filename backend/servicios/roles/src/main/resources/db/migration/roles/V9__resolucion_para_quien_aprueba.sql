-- D-107 (F15-H01): el Vicerrector académico, el Rector y los miembros del OCS leen la resolución del
-- OCS que aprobó el distributivo, su respaldo. Cambia la matriz D-16, así que va en una migración
-- nueva (D-63). Escribirla sigue solo en la Secretaría general académica.
insert into rol_permiso (rol_id, funcionalidad, tipo, alcance)
select r.id, 'RESOLUCION_OCS', 'LECTURA', 'TODO'
from rol r
where r.identificador in ('VICERRECTOR', 'RECTOR', 'MIEMBRO_OCS');
