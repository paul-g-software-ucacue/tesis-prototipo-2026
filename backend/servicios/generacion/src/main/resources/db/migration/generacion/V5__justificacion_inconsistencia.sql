-- Las justificaciones de las inconsistencias globales blandas (F10-H01, RF-29, MD-07). El panel se
-- calcula al abrirlo: solo esto se guarda. Es solo de inserción: cambiar una justificación agrega
-- una fila y la vigente es la última (principio VII). Vale para esa versión, ese tipo y ese objeto.

create table justificacion_inconsistencia (
    id uuid primary key,
    version_id uuid not null references version_distributivo (id) on delete cascade,
    tipo varchar(30) not null check (tipo in (
        'DOCENTE_SIN_CLASE', 'PARALELO_SIN_PROPUESTA', 'COORDINADORES_DE_LA_CARRERA',
        'GESTORES_FUERA_DE_RANGO', 'HORAS_SOBRE_LA_MALLA', 'SOBRECARGA', 'PROPORCION',
        'ACTIVIDADES_OBLIGATORIAS')),
    objeto varchar(60) not null check (length(objeto) > 0),
    texto varchar(500) not null check (length(texto) >= 10),
    justificada_por varchar(64) not null,
    justificada_en timestamptz not null
);

create index justificacion_vigente
    on justificacion_inconsistencia (version_id, tipo, objeto, justificada_en desc);

alter table registro_cambio alter column accion type varchar(30);
alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check check (accion in (
    'DISTRIBUTIVO_CREADO', 'PROPUESTA_GUARDADA', 'PROPUESTA_QUITADA',
    'ACTIVIDAD_GUARDADA', 'ACTIVIDAD_QUITADA', 'INCONSISTENCIA_JUSTIFICADA'));
