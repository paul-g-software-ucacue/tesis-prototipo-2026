-- El Responsable de consolidación de cada periodo (F05-H03, D-13, ADR-003) y la copia de los
-- periodos que avisa Académico por eventos (ADR-018, research §1). Roles no lee las tablas de
-- Académico (principio V): la copia llega en academico.PeriodoActualizado. Sin datos precargados.

-- La copia de cada periodo con su estado más avanzado conocido; nunca retrocede.
create table periodo_conocido (
    codigo varchar(20) primary key check (codigo ~ '^[A-Za-z0-9._-]{1,20}$'),
    estado varchar(16) not null
        check (estado in ('PLANIFICACION', 'EN_APROBACION', 'VIGENTE', 'CERRADO')),
    actualizado_en timestamptz not null
);

-- Uno por periodo: designar a otro reemplaza la fila. Es un rol, no un cargo: no tiene horas.
create table designacion_responsable (
    periodo varchar(20) primary key references periodo_conocido (codigo),
    usuario varchar(64) not null,
    designado_en timestamptz not null,
    designado_por varchar(64) not null
);

create index designacion_responsable_por_usuario on designacion_responsable (usuario);

-- Los eventos ya aplicados, para recibir cada uno una sola vez (ADR-018).
create table evento_recibido (
    id_evento uuid primary key,
    tipo varchar(80) not null,
    origen varchar(40) not null,
    recibido_en timestamptz not null
);

alter table registro_cambio drop constraint registro_cambio_accion_check;
alter table registro_cambio add constraint registro_cambio_accion_check check (accion in (
    'CREAR_ROL', 'EDITAR_ROL', 'BORRAR_ROL', 'ASIGNAR_ROL', 'QUITAR_ROL', 'INICIALIZAR',
    'DESIGNAR_RESPONSABLE'));
