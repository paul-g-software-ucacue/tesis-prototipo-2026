# Contrato: demostración con el evento de prueba (D-48)

Es lo que garantizan el endpoint de prueba de Generación y `deploy/probar_eventos.sh`.

## `POST /api/generacion/eventos-prueba`

Está en `contratos/generacion/openapi.yaml`, con token de usuario y sin cuerpo.

| Situación | Estado | Cuerpo y efecto |
|---|---|---|
| Encendido (`mdd.generacion.eventos-prueba.habilitado=true`) | 202 | `{ "idEvento": "<uuid>" }`. En una transacción, una fila en `generacion.evento_prueba` y una publicación `generacion.EventoDePrueba` hacia `usuarios`. |
| Apagado (por defecto) | 404 | `COMUN_RECURSO_NO_ENCONTRADO`; la ruta no existe |
| Sin token | 401 | `SEGURIDAD_NO_AUTENTICADO` |
| Fallo técnico (por ejemplo, la base no responde) | 500 | `COMUN_ERROR_INTERNO`; no queda ni la fila ni la publicación |

Se enciende con `MDD_EVENTOS_PRUEBA_HABILITADO=true`: está en `.env.ejemplo`, en local y en la CI. En el VPS no se enciende (B00-H08).

## `deploy/probar_eventos.sh`

**Uso:** `deploy/probar_eventos.sh`, con el perfil levantado (`docker compose up -d --wait`). Necesita `curl`, `jq` y `docker compose`.

**Salida:** una línea `OK` o `FALLA` por paso. Termina con 0 si pasan todos y con 1 si falla alguno. No imprime el token ni la clave del proceso.

| Paso | Qué hace | Comprueba |
|---|---|---|
| 0 | Inicia sesión con `autor.prueba` a través del API Gateway | Recibe un token |
| 1 | Publica un evento con identidad en marcha | En menos de 10 s: 1 fila del `idEvento` en `usuarios.evento_recibido` y la publicación `COMPLETED` (escenario 1, SC-001) |
| 2 | `docker compose stop identidad` y publica | 202, la fila en `generacion.evento_prueba` y la publicación no `COMPLETED` (escenario 2, SC-002) |
| 3 | `docker compose start identidad` y espera a que esté sano | En menos de 75 s (2 ciclos de 30 s y un margen): 1 fila en `evento_recibido` y la publicación `COMPLETED` (escenario 2, SC-003) |
| 4 | Vuelve a marcar `FAILED` la publicación del paso 1, como si se hubiera perdido la respuesta | En menos de 75 s: la publicación vuelve a `COMPLETED` y `evento_recibido` sigue con 1 fila (escenario 3, SC-004) |
| 5 | `POST /interno/usuarios/eventos` a través del API Gateway, sin token y con el de usuario | 401 y 403; identidad no registra nada (FR-012, SC-006) |

Las consultas a la base usan `docker compose exec -T postgres psql -U postgres -d mdd -tAc`, con el `idEvento` validado como UUID antes de usarlo en el SQL.

En el paso 2, con identidad detenido, el token de usuario lo validan el API Gateway y distributivo con el JWKS que ya tienen en caché (5 min por defecto). El paso 2 corre segundos después del 1, dentro de ese caché; si responde 401, la causa es que el caché se renovó con identidad caído.

Después del paso 3, el token de usuario del paso 0 deja de valer, porque identidad arrancó con una llave efímera nueva (ADR-014). El paso 5 vuelve a iniciar sesión.

## `perfil-consolidado`

El workflow agrega el paso «Eventos de punta a punta» (`deploy/probar_eventos.sh`) después de `deploy/probar_errores.sh` y antes de la medición de R-19.
