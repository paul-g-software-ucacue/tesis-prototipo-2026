#!/usr/bin/env bash
# Roles y esquemas del perfil consolidado (ADR-006, ADR-013, contracts/persistencia.md).
# Lo ejecuta la imagen de PostgreSQL la primera vez que se crea el volumen de datos
# (docker-entrypoint-initdb.d). Para repetirlo: docker compose down -v.
#
# Un rol por proceso, dueño de los esquemas de sus servicios: un proceso no puede crear ni leer
# tablas en los esquemas de otro. Las claves llegan por las variables DB_CLAVE_<PROCESO>.
set -euo pipefail

: "${DB_CLAVE_IDENTIDAD:?falta DB_CLAVE_IDENTIDAD}"
: "${DB_CLAVE_DATOS:?falta DB_CLAVE_DATOS}"
: "${DB_CLAVE_DISTRIBUTIVO:?falta DB_CLAVE_DISTRIBUTIVO}"
: "${DB_CLAVE_SALIDA:?falta DB_CLAVE_SALIDA}"

# Las claves se pasan como variables de psql y se citan con :'var', sin interpolarlas en el SQL.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v clave_identidad="$DB_CLAVE_IDENTIDAD" \
    -v clave_datos="$DB_CLAVE_DATOS" \
    -v clave_distributivo="$DB_CLAVE_DISTRIBUTIVO" \
    -v clave_salida="$DB_CLAVE_SALIDA" <<'SQL'
CREATE ROLE identidad LOGIN PASSWORD :'clave_identidad';
CREATE ROLE datos LOGIN PASSWORD :'clave_datos';
CREATE ROLE distributivo LOGIN PASSWORD :'clave_distributivo';
CREATE ROLE salida LOGIN PASSWORD :'clave_salida';

-- identidad: usuarios, roles y notificaciones
CREATE SCHEMA usuarios AUTHORIZATION identidad;
CREATE SCHEMA roles AUTHORIZATION identidad;
CREATE SCHEMA notificaciones AUTHORIZATION identidad;

-- datos: cargas, docentes, academico y normativa
CREATE SCHEMA cargas AUTHORIZATION datos;
CREATE SCHEMA docentes AUTHORIZATION datos;
CREATE SCHEMA academico AUTHORIZATION datos;
CREATE SCHEMA normativa AUTHORIZATION datos;

-- distributivo: generacion, validacionocs y firma
CREATE SCHEMA generacion AUTHORIZATION distributivo;
CREATE SCHEMA validacionocs AUTHORIZATION distributivo;
CREATE SCHEMA firma AUTHORIZATION distributivo;

-- salida: reporting y dashboard
CREATE SCHEMA reporting AUTHORIZATION salida;
CREATE SCHEMA dashboard AUTHORIZATION salida;

-- Ya es así desde PostgreSQL 15; se deja explícito.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL
