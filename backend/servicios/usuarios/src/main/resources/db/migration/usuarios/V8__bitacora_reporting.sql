-- Reporting registra en la bitácora desde F20-H01 (D-115): cada descarga de un reporte individual.
alter table bitacora drop constraint bitacora_servicio_check;
alter table bitacora add constraint bitacora_servicio_check
    check (servicio in ('generacion', 'academico', 'docentes', 'normativa', 'roles',
                        'validacionocs', 'firma', 'reporting'));
