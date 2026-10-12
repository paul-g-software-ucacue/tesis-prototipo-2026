# Contrato: medición de memoria de R-19

Es lo que garantiza la medición del escenario «Cabe en la memoria de la VM» (FR-007 y FR-008; D-34; research §9).

## `deploy/medir_memoria.sh`

```bash
deploy/medir_memoria.sh            # en la raíz del repositorio, con el perfil ya levantado o no
```

Pasos, en este orden:

1. `docker compose up -d --wait` (no hace nada si todo ya está sano).
2. **Calentamiento:** 20 llamadas a `/actuator/health/readiness` de cada proceso y del gateway, con `docker compose exec <contenedor>` y el mismo comando del *healthcheck*.
3. Espera de 30 segundos.
4. `docker stats --no-stream`, una sola lectura.
5. Imprime en la salida estándar una tabla Markdown con el formato de abajo y termina con:
   - código `0` si el total es menor que 4 GiB;
   - código `1` si es de 4 GiB o más;
   - código `2` si un contenedor no está sano.

## Formato de la evidencia

```markdown
**Medición de R-19** · <fecha ISO 8601> · commit `<sha7>` · entorno: <ubuntu-latest (Docker <versión>) | local (Podman <versión>)>
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

Resultado: <cumple (< 4 GiB) | falla (≥ 4 GiB)>. <Alerta R-19: más de 3,5 GiB | Sin alerta de R-19 (≤ 3,5 GiB)>.
```

## `.github/workflows/perfil-consolidado.yml`

| Aspecto | Contrato |
|---|---|
| Disparador | `workflow_dispatch` (manual). No corre en los pull requests ni es un check obligatorio. |
| Permisos | `contents: read`; acciones fijadas por SHA (pinact) |
| Pasos | *Checkout* limpio → `docker compose up -d --wait` (escenario 1 en un clon recién hecho) → `deploy/medir_memoria.sh` → la tabla al resumen del run (`$GITHUB_STEP_SUMMARY`) y como artefacto `medicion-r19` (90 días) → `docker compose down -v` |
| Falla si | Algún contenedor no queda sano o el total es de 4 GiB o más |

## Garantías

1. La medición se hace siempre después del mismo calentamiento, así que las mediciones se pueden comparar entre sí y con la de B00-H03.
2. La evidencia de R-19 dice el consumo por contenedor, el total, los límites, la fecha, el commit y el entorno (FR-008).
3. Un total de 3,5 GiB o más queda marcado como alerta de R-19, aunque cumpla el escenario.
