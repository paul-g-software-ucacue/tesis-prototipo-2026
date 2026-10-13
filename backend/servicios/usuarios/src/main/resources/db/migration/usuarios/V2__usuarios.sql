-- Usuarios que pueden iniciar sesión y sus roles (B00-H03, D-37).
-- Los roles viven aquí hasta que F05-H02 decida su origen; ningún otro servicio lee estas tablas.
create table usuario (
    id uuid primary key,
    nombre_usuario varchar(64) not null unique,
    clave_hash varchar(255) not null,
    activo boolean not null
);

create table usuario_rol (
    usuario_id uuid not null references usuario (id) on delete cascade,
    rol varchar(64) not null,
    primary key (usuario_id, rol)
);
