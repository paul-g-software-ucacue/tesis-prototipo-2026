# Contrato: autenticación

Es lo que garantizan el API Gateway, los 4 procesos y Usuarios sobre el token (escenarios 1 a 3; FR-001 a FR-010; D-37, D-38; ADR-014). Las decisiones están en `research.md` §1 a §8.

## Quién hace qué

| Componente | Firma | Valida | Rutas sin token |
|---|---|---|---|
| Usuarios (proceso identidad) | Sí, con la llave privada | Sí, sus propios endpoints | `POST /api/usuarios/sesion`, `GET /api/usuarios/jwks` y `/actuator/health/**` |
| API Gateway | No | Sí, toda `/api/**` antes de reenviar | `POST /api/usuarios/sesion` y `/actuator/health/**` |
| datos, distributivo y salida | No | Sí, toda `/api/**` | `/actuator/health/**` |

## Validación (API Gateway y procesos)

1. El encabezado `Authorization: Bearer <token>` está presente y bien formado.
2. `alg` es `RS256`.
3. La firma es válida con una llave del JWKS de Usuarios, elegida por `kid`. Si el `kid` es desconocido, se vuelve a pedir el JWKS.
4. `iss` es `mdd-usuarios`.
5. `exp` no pasó, con 60 s de tolerancia.

Si una falla, la respuesta es el 401 de abajo. Si pasan todas, el usuario es el `sub` y sus autoridades son `ROLE_<rol>` por cada valor de `roles`.

**Configuración** (`application.yaml` y variables):

| Propiedad | Valor por defecto | Quién |
|---|---|---|
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `http://identidad:8080/api/usuarios/jwks`; en identidad, `http://localhost:8080/api/usuarios/jwks` | API Gateway y procesos |
| `mdd.seguridad.emisor` | `mdd-usuarios` | API Gateway, procesos y Usuarios |
| `mdd.seguridad.rutas-publicas` | `/actuator/health/**` y, en identidad, las dos rutas de Usuarios | procesos |
| `mdd.usuarios.token.vigencia` | `PT30M` | Usuarios |
| `mdd.usuarios.llave-privada` | sin valor, así que la llave es efímera | Usuarios |
| `mdd.usuarios.usuario-prueba.habilitado` | `false`; `true` en `.env.ejemplo` | Usuarios |

## Respuesta 401 por token

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
WWW-Authenticate: Bearer

{"titulo":"Sesión no válida","descripcion":"Inicia sesión de nuevo para continuar.","codigo":"SEGURIDAD_NO_AUTENTICADO"}
```

**Mismo cuerpo, byte por byte,** para todas estas causas:
- sin encabezado;
- encabezado mal formado;
- token mal formado;
- firma alterada;
- otra llave;
- `alg` distinto de RS256 o `none`;
- otro emisor;
- token vencido.

La causa se registra en nivel DEBUG, sin el token.

## Garantías

1. Ninguna petición a `/api/**`, salvo las rutas sin token de la tabla, llega a un controlador sin un token válido, tanto por el API Gateway como directo a un proceso (FR-003, FR-004).
2. Un 401 por token no permite saber qué falló: estado, encabezados y cuerpo son idénticos para todas las causas (FR-005, SC-002).
3. Solo Usuarios puede firmar: la llave privada no sale del proceso identidad y no está en el repositorio, el compose ni los registros (FR-002).
4. Los *healthchecks* de B00-H02 siguen funcionando sin token (FR-008).
5. Ni el token ni la contraseña aparecen en las respuestas ni en los registros (FR-009).
