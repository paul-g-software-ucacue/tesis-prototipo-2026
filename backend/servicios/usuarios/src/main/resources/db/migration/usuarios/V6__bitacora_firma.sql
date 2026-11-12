-- Firma registra en la bitácora desde F13-H02 (D-101): el documento firmable generado.
-- El check de V5 no tiene nombre propio; PostgreSQL lo llamó bitacora_servicio_check.
alter table bitacora drop constraint bitacora_servicio_check;
alter table bitacora add constraint bitacora_servicio_check
    check (servicio in ('generacion', 'academico', 'docentes', 'normativa', 'roles',
                        'validacionocs', 'firma'));
