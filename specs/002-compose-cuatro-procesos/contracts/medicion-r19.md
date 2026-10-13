# Contrato: medición de memoria de R-19

Es lo que garantiza la medición del escenario «Cabe en la memoria de la VM» (FR-007 y FR-008; D-34; research §9).

## `deploy/medir_memoria.sh`

```bash
deploy/medir_memoria.sh            # en la raíz del repositorio, con el perfil ya levantado o no
```

Pasos, en este orden:

1. `docker compose up -d --wait` (no hace nada si todo ya está sano). Si falla, el script termina con código `2`.
2. **Calentamiento:** 20 llamadas a `/actuator/health/readiness` de cada proceso y del gateway, con `docker compose exec <contenedor>` y el mismo comando del *healthcheck*.
3. Espera de 30 segundos.
4. `docker stats --no-stream $(docker compose ps -q)`, una sola lectura, solo de los contenedores del proyecto. El nombre del servicio sale de `docker compose ps --format '{{.Name}};{{.Service}}'`, porque el nombre del contenedor lleva el prefijo del directorio.
5. Imprime en la salida estándar una tabla Markdown con el formato de abajo y termina con:
   - código `0` si el total es menor que 8 GiB;
   - código `1` si es de 8 GiB o más;
   - código `2` si un contenedor no está sano.

## Formato de la evidencia

```markdown
**Medición de R-19** · <fecha ISO 8601> · commit `<sha7>` · entorno: <ubuntu-latest (Docker <versión>) | local (Docker <versión>)>
Calentamiento: 20 llamadas a la salud por componente y 30 s de espera.

| Contenedor | Consumo (MiB) | Límite (MiB) |
|---|---:|---:|
| postgres | … | 512 |
| identidad | … | 512 |
| datos | … | 512 |
| distributivo | … | 512 |
| salida | … | 512 |
| gateway | … | 384 |
| **Total** | **…** (… GiB) | 2 944 |

Resultado: <cumple (< 8 GiB) | falla (≥ 8 GiB)>. <Alerta R-19: más de 7 GiB | Sin alerta de R-19 (≤ 7 GiB)>.
```

## `.github/workflows/perfil-consolidado.yml`

| Aspecto | Contrato |
|---|---|
| Disparador | `workflow_dispatch` (manual) y `pull_request` (tipos `opened`, `synchronize`, `reopened` y `ready_for_review`) con `paths: [compose.yaml, .env.ejemplo, 'deploy/**', '.github/workflows/perfil-consolidado.yml']`. No corre en borradores ni es un check obligatorio. El disparador de pull request hace falta porque GitHub solo ejecuta a mano los workflows que ya están en la rama principal. |
| Permisos | `contents: read`; acciones fijadas por SHA (pinact); `timeout-minutes: 30` |
| Pasos | *Checkout* limpio → `docker compose build`, con su tiempo → `docker compose up -d --wait`, con su tiempo (escenario 1 en un clon recién hecho y referencia de SC-001) → `deploy/medir_memoria.sh`, en un paso con `shell: bash` para que `pipefail` conserve su código de salida → la tabla al resumen del run (`$GITHUB_STEP_SUMMARY`) y como artefacto `medicion-r19` (90 días) → `docker compose down -v` |
| Falla si | Algún contenedor no queda sano o el total es de 8 GiB o más |

## Garantías

1. La medición se hace siempre después del mismo calentamiento, así que las mediciones se pueden comparar entre sí y con la de B00-H03.
2. La evidencia de R-19 dice el consumo por contenedor, el total, los límites, la fecha, el commit y el entorno (FR-008).
3. Un total de más de 7 GiB queda marcado como alerta de R-19, aunque cumpla el escenario (D-36).
