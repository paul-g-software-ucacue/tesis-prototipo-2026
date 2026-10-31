-- La carrera de cada Coordinador de carrera (F06-H05, D-64): una por usuario. El código es de
-- Académico, en otro proceso: sin llave foránea (principio V), y no se comprueba al guardar (D-76).

create table carrera_de_coordinador (
    nombre_usuario varchar(64) primary key,
    carrera varchar(20) not null,
    indicada_en timestamptz not null,
    indicada_por varchar(64) not null
);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check check (accion in (
    'CREAR_ROL', 'EDITAR_ROL', 'BORRAR_ROL', 'ASIGNAR_ROL', 'QUITAR_ROL', 'INICIALIZAR',
    'DESIGNAR_RESPONSABLE', 'INDICAR_CARRERA', 'QUITAR_CARRERA'));
