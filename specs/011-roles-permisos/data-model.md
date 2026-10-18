# Data Model: Roles con permisos (F05-H02)

Esquema `roles` del proceso `identidad`. Los detalles están en [research.md](research.md).

## Tablas

| Tabla | Campos | Reglas |
|---|---|---|
| `funcionalidad` | `codigo` varchar(40) PK, `nombre` varchar(80), `orden` int | Fijas: las inserta `V2__matriz_d16.sql`; las historias siguientes agregan las suyas con otra migración |
| `funcionalidad_tipo` | `funcionalidad` FK, `tipo` varchar(12); PK (`funcionalidad`, `tipo`) | `tipo` ∈ `LECTURA`, `ESCRITURA`, `APROBACION`, `FIRMA` (RF-02) |
| `rol` | `id` uuid PK, `identificador` varchar(64) único, `nombre` varchar(80) único, `descripcion` varchar(300), `predefinido` boolean, `creado_en`, `actualizado_en` timestamptz | `identificador` en mayúsculas con guion bajo; se deriva del nombre al crear y no cambia. `predefinido = true` solo para los 10 de D-16 (D-63) |
| `rol_permiso` | `rol_id` FK, `funcionalidad`, `tipo`, `alcance` varchar(8); PK (`rol_id`, `funcionalidad`, `tipo`); FK (`funcionalidad`, `tipo`) → `funcionalidad_tipo` | `alcance` ∈ `TODO`, `PROPIO` (D-64). Un rol tiene al menos un permiso |
| `asignacion` | `nombre_usuario` varchar(64), `rol_id` FK, `asignado_en` timestamptz, `asignado_por` varchar(64); PK (`nombre_usuario`, `rol_id`) | El nombre de usuario va normalizado (minúsculas, F05-H01). Sin FK a Usuarios: otro esquema (principio V) |
| `inicializacion` | `nombre_usuario` varchar(64) PK, `inicializado_en` timestamptz | Marca que el usuario ya recibió sus roles iniciales (research §8) |
| `registro_cambio` | `id` uuid PK, `momento` timestamptz, `usuario` varchar(64), `accion` varchar(20), `detalle` jsonb | `accion` ∈ `CREAR_ROL`, `EDITAR_ROL`, `BORRAR_ROL`, `ASIGNAR`, `QUITAR`, `INICIALIZAR`. Nunca datos sensibles (FR-011) |

`usuarios.usuario_rol` queda sin uso y no se borra (research §1).

## Permisos efectivos

Unión de los `rol_permiso` de todos los roles asignados. Para la misma funcionalidad y el mismo tipo con alcances distintos, gana `TODO` (FR-005). En el token, ordenados alfabéticamente:

| Forma | Ejemplo |
|---|---|
| `FUNCIONALIDAD:TIPO` (alcance todo) | `USUARIOS_ROLES:ESCRITURA` |
| `FUNCIONALIDAD:TIPO:PROPIO` | `OFERTA:LECTURA:PROPIO` |

En los procesos, cada permiso es la autoridad `PERMISO_FUNCIONALIDAD:TIPO` (sin el alcance).

## Funcionalidades

| Código | Nombre | Tipos que admite |
|---|---|---|
| `USUARIOS_ROLES` | Usuarios y roles | L, E |
| `PARAMETROS` | Parámetros y normativa | L, E |
| `CATALOGOS` | Catálogos y datos maestros | L, E |
| `FLUJO_FIRMAS` | Flujo y árbol de firmas | L, E |
| `BITACORA` | Bitácora | L |
| `CARGAS` | Cargas de datos | L, E |
| `DOCENTES` | Docentes | L, E |
| `OFERTA` | Oferta y propuesta de docentes | L, E, A |
| `PROYECTOS` | Proyectos y horas de participantes | L, E, A |
| `CONSOLIDACION` | Consolidación e informe sustentado | L, E, A |
| `DISTRIBUTIVO` | Distributivo del periodo | L, E, A, F |
| `REPORTES` | Reportes | L |
| `VOTACION_OCS` | Votación del OCS | A |
| `RESOLUCION_OCS` | Resolución del OCS | L, E |
| `PUBLICACION` | Publicación del distributivo | A |
| `MI_DISTRIBUTIVO` | Mi distributivo | L, E |

L = lectura, E = escritura, A = aprobación (incluye devolver), F = firma.

## Matriz D-16

Interpretación de cada fila del ERS (sección 3) en permisos. «(P)» = alcance «solo lo propio» (D-64); sin marca = todo. Aprobada como D-65.

| Rol (identificador) | Lectura | Escritura | Aprobación | Firma | Celda del ERS que lo origina |
|---|---|---|---|---|---|
| Administrador del sistema (`ADMINISTRADOR`) | Las 13 funcionalidades con lectura, salvo `MI_DISTRIBUTIVO` | `USUARIOS_ROLES`, `PARAMETROS`, `CATALOGOS`, `FLUJO_FIRMAS` | — | — | «Todo, incluida la bitácora» · «Usuarios, roles, parámetros, catálogos, archivo de configuración del flujo y del árbol de firmas» |
| Talento Humano / Operador de datos (`OPERADOR_DATOS`) | `CARGAS`, `DOCENTES` | `CARGAS`, `DOCENTES` | — | — | «Cargas y docentes» · «Cargas que se le deleguen y datos del docente» |
| Coordinador de carrera (`COORDINADOR_CARRERA`) | `OFERTA` (P), `DISTRIBUTIVO` (P), `DOCENTES` | `OFERTA` (P) | `OFERTA` (P) | — | «Su carrera y las horas de cada docente en otras unidades» · «Oferta propia y propuesta de docentes» · «Envía su borrador» |
| Gestor de investigación o de vinculación (`GESTOR_UNIDAD`) | `PROYECTOS` (P) | `PROYECTOS` (P) | `PROYECTOS` (P) | — | «Su unidad y sus proyectos» · «Proyectos aprobados y horas de sus participantes» · «Envía su planificación» |
| Responsable de consolidación (`RESPONSABLE_CONSOLIDACION`) | `DISTRIBUTIVO`, `OFERTA`, `PROYECTOS`, `CONSOLIDACION` | `CONSOLIDACION` | `CONSOLIDACION` | `DISTRIBUTIVO` | «Todo el distributivo del periodo» · «Consolidación e informe sustentado» · «Envía a revisión; devuelve» · «F1 (elaborado)» |
| Vicerrector académico (`VICERRECTOR`) | `DISTRIBUTIVO`, `REPORTES`, `BITACORA` | `DISTRIBUTIVO` | `DISTRIBUTIVO` | `DISTRIBUTIVO` | «Todo el distributivo, reportes y bitácora» · «Ajustes a la propuesta» · «Aprueba y congela; devuelve» · «F2 (revisado)» |
| Rector (`RECTOR`) | `DISTRIBUTIVO`, `REPORTES` | — | `DISTRIBUTIVO` | `DISTRIBUTIVO` | «Todo el distributivo y reportes» · «Devuelve» · «F3 (aprobado)» |
| Miembro del OCS (`MIEMBRO_OCS`) | `DISTRIBUTIVO`, `REPORTES` | — | `VOTACION_OCS` | — | «Versión congelada y reportes» · «Voto (opcional, C-15)» |
| Secretaría general académica (`SECRETARIA_GENERAL`) | `DISTRIBUTIVO`, `REPORTES`, `RESOLUCION_OCS` | `RESOLUCION_OCS` | `PUBLICACION` | — | «Todo el distributivo y reportes» · «Resolución del OCS» · «Publica» |
| Docente (`DOCENTE`) | `MI_DISTRIBUTIVO` (P) | `MI_DISTRIBUTIVO` (P) | — | — | «Su distributivo y su estado» · «Acuse de recibo» |

Notas de la interpretación:
- «Versión congelada» (OCS) y «solo la versión aprobada» son estados del distributivo, no permisos: los filtra la historia del flujo (F06 y siguientes), no este modelo.
- Qué firma cada rol (F1, F2 o F3) y en qué orden lo fija el árbol de firmas (ADR-007). Aquí solo se da el permiso de firma.
- El Coordinador lee `DOCENTES` con alcance todo porque D-16 le deja ver las horas de cada docente en otras unidades; qué campos ve lo decide la historia de docentes.

## Token de sesión (cambia)

| Claim | Valor |
|---|---|
| `roles` | Identificadores de los roles asignados, ordenados (ahora desde Roles) |
| `permisos` | **Nuevo.** Permisos efectivos, ordenados (tabla de arriba) |
| `sub`, `nombre`, `inicio`, `exp` | Sin cambios (ADR-014, ADR-020) |

La renovación vuelve a consultar roles y permisos (D-62).

## Sesión en la SPA (cambia)

`SesionActual` gana `permisos` (opcional, solo lo devuelve Usuarios). `tienePermiso(sesion, funcionalidad, tipo)` decide qué se muestra; el servidor decide qué se permite.
