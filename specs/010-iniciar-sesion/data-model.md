# Data Model: Iniciar sesión (F05-H01)

Esquema `usuarios` del proceso `identidad`. Los detalles están en [research.md](research.md).

## Usuario (cambia)

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | uuid | Sin cambios |
| `nombre_usuario` | varchar(64), único | **En minúsculas** (V4 normaliza las filas existentes); la búsqueda normaliza con `trim` y minúsculas |
| `nombre_para_mostrar` | varchar(120), no nulo | **Nuevo.** «Coordinación de carrera (demo)»; para las filas existentes, el nombre de usuario |
| `clave_hash` | varchar(255) | Solo hash Argon2id (`$argon2id$…`), nunca la contraseña (RNF-SEG-02) |
| `activo` | boolean | `false` no inicia sesión y responde igual que credenciales inválidas |

`usuario_rol` sin cambios: uno o más de los 10 identificadores de [research §1](research.md#1-roles-identificadores).

## Intento fallido (nuevo, D-60)

Tabla `intento_fallido`.

| Campo | Tipo | Reglas |
|---|---|---|
| `nombre` | varchar(64), clave primaria | Nombre **intentado**, normalizado; no referencia a `usuario`: cuenta también los que no existen |
| `fallidos` | int | Intentos fallidos seguidos desde el último correcto o el último bloqueo |
| `bloqueado_hasta` | timestamptz, nulo | Con valor y futuro: el nombre está bloqueado |
| `actualizado_en` | timestamptz | Para la limpieza de las filas inactivas por 24 horas |

**Estados:**

```text
sin fila ──fallo──► fallidos=1..4 ──quinto fallo──► bloqueado (hasta = ahora + 15 min)
   ▲                    │                                   │
   └──── inicio correcto (borra la fila) ◄──────────────────┘ al vencer el bloqueo, un fallo vuelve a fallidos=1
```

Un intento durante el bloqueo no cuenta, no verifica la contraseña y responde `USUARIOS_INICIO_BLOQUEADO`.

## Usuarios de la demo (archivo, D-59)

`usuarios-demo.yaml`, en los recursos del servicio Usuarios: una lista de `{usuario, nombre, roles}`, sin contraseñas. Los 11 usuarios de [research §2](research.md#2-usuarios-de-la-demo-d-59).

**Contraseñas por entorno:**

| Entorno | Fuente | Ejemplo |
|---|---|---|
| Equipo de demostración | `MDD_USUARIOS_DEMO_CLAVES=/run/mdd/claves-demo` (archivo `usuario=clave`; dueño 10001, grupo `mdd`, modo 0440; generado por `preparar.sh`, que agrega las líneas que falten) | Una clave aleatoria por usuario |
| Local y CI | `MDD_USUARIOS_DEMO_CLAVE` en `.env.ejemplo` | `cambiar-en-local-demo` para todos |

## Token de sesión (cambia)

| Claim | Valor |
|---|---|
| `sub` | Nombre de usuario (sin cambios) |
| `roles` | Roles ordenados (sin cambios) |
| `nombre` | **Nuevo.** Nombre para mostrar |
| `inicio` | **Nuevo.** Instante del inicio de sesión (segundos desde la época); se conserva al renovar |
| `exp` | `iat` + 30 min (sin cambios) |

La renovación emite un token nuevo con el mismo `inicio` mientras `ahora < inicio + 8 h`.

## Sesión en la SPA (cambia)

| Campo | Reglas |
|---|---|
| `token`, `venceEn` | Solo en memoria (D-51), sin cambios |
| `ultimaActividad` | **Nuevo.** Instante de la última navegación, tecla, clic o petición a la API iniciada por el usuario. La renovación y las peticiones automáticas (con `X-MDD-Automatica`) **no** cuentan |
| `ultimaRenovacion` | **Nuevo.** Instante de la última renovación o del inicio de sesión |

Renueva una vez si `ultimaActividad > ultimaRenovacion` y al token le quedan menos de 20 minutos.
