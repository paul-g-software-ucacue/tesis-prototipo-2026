# Data Model: Registrar cada cambio en la bitácora

## Entrada de la bitácora (Usuarios, dominio)

Un hecho que ya ocurrió en otro servicio. Se crea una vez y no cambia nunca.

| Campo | Tipo | Regla |
|---|---|---|
| `idEvento` | UUID | El del evento que la trajo. Identifica la entrada y hace la idempotencia (FR-007). |
| `servicio` | texto | Uno de `generacion`, `academico`, `docentes`, `normativa`, `roles`, `validacionocs`. Igual al `origen` del evento y al prefijo de su tipo. |
| `accion` | texto | `^[A-Z][A-Z_]{2,39}$`, el nombre de la acción del servicio (por ejemplo, `ACTIVIDAD_GUARDADA`). |
| `usuario` | texto | El nombre de usuario que inició el cambio, o `sistema`. De 1 a 64 caracteres, sin otro formato: es la definición de Usuarios (`nombre_usuario varchar(64)`) y del parámetro `Usuario` de Roles (`minLength: 1`, `maxLength: 64`), confirmada en T002 (principio XIV). |
| `momento` | instante | Cuándo ocurrió el cambio en su servicio, no cuándo llegó. |
| `periodo` | texto, opcional | `^[A-Za-z0-9._-]{1,20}$`, el código del periodo, si el cambio pertenece a uno. |
| `objeto` | objeto | Sobre qué se hizo: los identificadores del elemento y su contexto, como los guarda el servicio. |
| `anterior` | objeto, opcional | Los valores antes del cambio. Falta al crear. |
| `nuevo` | objeto, opcional | Los valores después del cambio. Falta al quitar. |
| `recibidaEn` | instante | Cuándo la registró Usuarios. La pone la base. |

**Invariantes** (se validan al construirla; si fallan, `USUARIOS_ENTRADA_BITACORA_INVALIDA`):
- `anterior` y `nuevo` no faltan los dos.
- Ninguna clave de `objeto`, `anterior` ni `nuevo`, a cualquier profundidad, contiene `clave`, `contrasena`, `password`, `token`, `secreto` ni `jwt`, sin importar mayúsculas (FR-011).
- El tamaño del contenido no pasa de 64 KB: una entrada describe un cambio, no un archivo.
- Una entrada de `docentes` no lleva en `anterior` ni en `nuevo` las claves `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `correoInstitucional`, `telefono` ni `condicionEspecial` (FR-014, D-91); esos campos solo se nombran en `objeto.camposPersonales`.

## Clase de una acción (cada servicio emisor)

| Clase | `anterior` | `nuevo` | Ejemplo |
|---|---|---|---|
| Crea | — | obligatorio | `DOCENTE_REGISTRADO` |
| Cambia | obligatorio | obligatorio | `ACTIVIDAD_GUARDADA` de 4 a 6 horas |
| Quita | obligatorio | — | `PROPUESTA_QUITADA` |

Una acción que puede crear o cambiar (research §2) decide al registrar, según haya o no un valor anterior. El registro de cambio de cada servicio rechaza un cambio que no cumple su clase, así que el error aparece en las pruebas del servicio y no en la bitácora.

## Registro de cambio de cada servicio (cambio)

`CambioDeDistributivo`, `CambioRegistrado` (Académico y Docentes), `CambioDeNormativa` y `CambioDeRoles` pasan de

```
(momento, usuario, accion, detalle)
```

a

```
(momento, usuario, accion, detalle, anterior, nuevo)
```

- `detalle` no cambia de forma y sigue en la tabla local: es el `objeto` de la entrada.
- `anterior` y `nuevo` son mapas inmutables, vacíos cuando no aplican.
- Validación OCS no tenía registro de cambio: agrega `CambioDeFlujo(momento, usuario, accion, detalle, anterior, nuevo)` solo para publicar, porque su fila de carga ya es el registro local.

## Tabla `usuarios.bitacora` (migración `V5__bitacora.sql`)

| Columna | Tipo | Nota |
|---|---|---|
| `id_evento` | `uuid` | Clave primaria. |
| `servicio` | `varchar(20)` | `check` con los seis servicios. |
| `accion` | `varchar(40)` | `check` con el patrón. |
| `usuario` | `varchar(64)` | |
| `momento` | `timestamptz` | |
| `periodo` | `varchar(20)` | Nulo si no aplica. |
| `objeto` | `jsonb` | |
| `anterior` | `jsonb` | Nulo al crear. |
| `nuevo` | `jsonb` | Nulo al quitar. |
| `recibida_en` | `timestamptz` | `default now()`. |

- `check (anterior is not null or nuevo is not null)`.
- Índices: `(periodo, momento)` y `(momento)`, para F17-H02.
- Disparadores `bitacora_sin_cambios` (`BEFORE UPDATE OR DELETE`, por fila) y `bitacora_sin_truncar` (`BEFORE TRUNCATE`, por sentencia): lanzan `La bitácora no se puede modificar ni borrar.`
- `REVOKE UPDATE, DELETE, TRUNCATE ON usuarios.bitacora FROM identidad`.

La entrada y su `evento_recibido` se insertan en la misma transacción: si una falla, ninguna queda.

## Tabla retirada

`generacion.evento_prueba` (migración `V7__sin_evento_de_prueba.sql`, `DROP TABLE`), con el evento de prueba de B00-H05 (research §7).

## Lo que no cambia

- `<servicio>.registro_cambio` de los cinco servicios: misma forma y mismas filas.
- `validacionocs.configuracion_flujo`.
- `usuarios.evento_recibido`.
- Los esquemas `eventos_<proceso>`: `eventos_identidad` ya existe y lo migra `:plataforma:eventos` al arrancar.
