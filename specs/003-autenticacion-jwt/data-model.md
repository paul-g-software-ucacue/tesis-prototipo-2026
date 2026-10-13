# Modelo de datos (fase 1): B00-H03

Describe las entidades que nombra la especificación (Key Entities), con sus reglas de validación. La única persistida es el usuario.

## Usuario (servicio Usuarios)

| Atributo | Tipo | Regla |
|---|---|---|
| `id` | UUID | Identificador interno; no sale en el token. |
| `nombreUsuario` | texto, de 3 a 64 caracteres | Único. Es el `sub` del token. |
| `claveHash` | texto | Hash Argon2id con sal; nunca la contraseña en claro (FR-010). |
| `activo` | booleano | Solo un usuario activo puede iniciar sesión. |
| `roles` | conjunto de textos | Identificadores en mayúsculas, como `DOCENTE` o `COORDINADOR_CARRERA`. Puede estar vacío. |

**En PostgreSQL** (`V2__usuarios.sql`, esquema `usuarios`):

| Tabla | Columnas | Restricciones |
|---|---|---|
| `usuario` | `id uuid`, `nombre_usuario varchar(64)`, `clave_hash varchar(255)`, `activo boolean` | PK `id`; `nombre_usuario` único y no nulo |
| `usuario_rol` | `usuario_id uuid`, `rol varchar(64)` | PK (`usuario_id`, `rol`); FK a `usuario` con `on delete cascade` |

**Usuario de prueba:** `autor.prueba`, activo, con los roles `DOCENTE` y `COORDINADOR_CARRERA`. Solo existe si `mdd.usuarios.usuario-prueba.habilitado=true`; la contraseña llega en `MDD_USUARIO_PRUEBA_CLAVE` (research §8).

## Token (JWT)

| Parte | Campo | Regla |
|---|---|---|
| Encabezado | `alg` | Siempre `RS256`; cualquier otro valor se rechaza. |
| Encabezado | `kid` | Huella SHA-256 de la llave pública, en Base64URL. |
| Cuerpo | `iss` | `mdd-usuarios`; otro emisor se rechaza. |
| Cuerpo | `sub` | `nombreUsuario`. |
| Cuerpo | `roles` | Lista de roles; vacía si el usuario no tiene. |
| Cuerpo | `iat` y `exp` | `exp = iat + vigencia` (30 min por defecto). Un token vencido se rechaza, con 60 s de tolerancia. |

**Transiciones:** emitido → válido → vencido. No hay renovación ni revocación en esta historia.

## Par de llaves

| Atributo | Regla |
|---|---|
| Algoritmo | RSA de 2048 bits |
| Llave privada | Solo en el proceso identidad. Viene de `mdd.usuarios.llave-privada` (archivo PEM) o, si no está, se genera al arrancar y se avisa en el registro. Nunca en el repositorio, en el compose ni en los registros. |
| Llave pública | Se publica como JWKS en `GET /api/usuarios/jwks`, con su `kid`. |

## Sesión actual

| Campo | Origen |
|---|---|
| `usuario` | `sub` del token |
| `roles` | `roles` del token, ordenados alfabéticamente |

## Error 401

| Campo | Valor |
|---|---|
| `titulo` | `Sesión no válida` (token) · `Credenciales inválidas` (inicio de sesión) |
| `descripcion` | `Inicia sesión de nuevo para continuar.` · `El usuario o la contraseña no son correctos.` |
| `codigo` | `SEGURIDAD_NO_AUTENTICADO` · `USUARIOS_CREDENCIALES_INVALIDAS` |

**Reglas:**
- El cuerpo es idéntico para todas las causas de un mismo código (SC-002).
- El encabezado `WWW-Authenticate` dice solo `Bearer`.
- Nunca hay `detalles` ni `idIncidente` en un 401.

## Medición de R-19 después de iniciar sesión

Los mismos campos que en B00-H02 (`specs/002-compose-cuatro-procesos/data-model.md`), más estos:

| Campo | Contenido |
|---|---|
| Contexto | «después de iniciar sesión» |
| Calentamiento | 20 llamadas a la salud y 20 a la «sesión actual» de cada componente |
| Diferencia | MiB respecto de la medición de B00-H02 sobre `main` (931,6 MiB) |
