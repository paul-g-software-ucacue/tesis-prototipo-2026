-- El periodo de una confirmación «sin proyectos» usa la regla de Académico, su dueño (F02-H02): V3
-- pedía el formato de la carga 3 (AAAA-I o AAAA-II), pero un periodo registrado en su pantalla puede
-- tener otro código, como 003. Las filas que ya existen cumplen la regla nueva.
alter table cargas.historial_carga drop constraint historial_carga_periodo_check;
alter table cargas.historial_carga alter column periodo type varchar(20);
alter table cargas.historial_carga add constraint historial_carga_periodo_check
    check (periodo ~ '^[A-Za-z0-9._-]{1,20}$');
