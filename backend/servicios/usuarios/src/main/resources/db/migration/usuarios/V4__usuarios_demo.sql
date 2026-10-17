-- Usuarios de la demo e inicio de sesión de F05-H01.
-- D-59: nombre para mostrar; el nombre de usuario no distingue mayúsculas y se guarda en minúsculas.
-- D-60: intentos fallidos por nombre intentado, exista o no, para el bloqueo de 15 minutos.
alter table usuario add column nombre_para_mostrar varchar(120);
update usuario set nombre_para_mostrar = nombre_usuario;
alter table usuario alter column nombre_para_mostrar set not null;

update usuario set nombre_usuario = lower(nombre_usuario);

-- Sin referencia a usuario: también cuenta los nombres que no existen, para no revelar cuáles sí.
create table intento_fallido (
    nombre varchar(64) primary key,
    fallidos int not null,
    bloqueado_hasta timestamptz,
    actualizado_en timestamptz not null
);
