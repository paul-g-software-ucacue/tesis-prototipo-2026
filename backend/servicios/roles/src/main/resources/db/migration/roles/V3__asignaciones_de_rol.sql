-- Asignaciones de rol a usuarios, roles iniciales y registro de cambios (F05-H02, research §1, §7
-- y §8). El nombre de usuario va normalizado (minúsculas, F05-H01). No hay llave foránea a
-- usuarios.usuario: es el esquema de otro servicio (principio V).

create table asignacion_rol (
    nombre_usuario varchar(64) not null,
    rol_id uuid not null references rol (id),
    asignado_en timestamptz not null,
    asignado_por varchar(64) not null,
    primary key (nombre_usuario, rol_id)
);

create index asignacion_rol_por_rol on asignacion_rol (rol_id);

-- Un usuario aparece aquí cuando ya recibió sus roles iniciales del archivo de la demo: desde
-- entonces, solo el administrador cambia sus roles (FR-009).
create table inicializacion (
    nombre_usuario varchar(64) primary key,
    inicializado_en timestamptz not null
);

-- Cada cambio de roles y de asignaciones, para la bitácora de historias posteriores (FR-011).
-- Nunca guarda datos sensibles.
create table registro_cambio (
    id uuid primary key,
    momento timestamptz not null,
    usuario varchar(64) not null,
    accion varchar(20) not null check (accion in (
        'CREAR_ROL', 'EDITAR_ROL', 'BORRAR_ROL', 'ASIGNAR_ROL', 'QUITAR_ROL', 'INICIALIZAR')),
    detalle jsonb not null
);
