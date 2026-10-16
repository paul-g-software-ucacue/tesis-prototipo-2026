# Contrato: CI de cada pull request y regla de la rama principal

Decisiones: D-29 (jobs de frontend condicionados) y D-31 (regla de la rama principal), en `docs/decisiones.md`.

Este documento fija lo que la CI y el ruleset `main` garantizan después de B00-H01. Los jobs nuevos o los que cambian tienen que respetar exactamente estos nombres: el ruleset los usa como checks obligatorios. Si un nombre cambia, el pull request queda esperando un check que nunca llega.

## Jobs de `ci.yml`

| Check (`name:` del job) | Etapa | Corre cuando | Falla si | Escenario o FR | Historia dueña |
|---|---|---|---|---|---|
| `0 · deteccion` | — | Siempre | No puede leer el repositorio | FR-008 | B00-H01 |
| `1 · lint-backend` | Lint | Siempre | Spotless encuentra un archivo sin formato, o SpotBugs con FindSecBugs un hallazgo de confianza alta en el código de `main` | Escenarios 3 y 5; FR-007, FR-014 y FR-015 | B00-H01 |
| `1 · lint-frontend` | Lint | `deteccion.frontend == 'true'` | ESLint, Prettier, TypeScript o el cliente generado fallan | Escenario «Lint del frontend» de #23 | B00-H06 |
| `2 · test-backend` | Pruebas | Después de `lint-backend` | Falla una prueba unitaria, de arquitectura (ArchUnit o Modulith) o de contrato, o `jacocoTestCoverageVerification` (módulo < 60 %, D-32) | Escenarios 1 a 4; FR-004 a FR-006 y FR-012 | B00-H01 |
| `2 · test-integracion` | Pruebas | Después de `lint-backend`, si el PR no es borrador | Falla una prueba de `integrationTest` (Testcontainers) | Escenario 3; FR-007 | B00-H01 |
| `2 · test-frontend` | Pruebas | Después de `lint-frontend` (se omite con él) | Falla Vitest | — | B00-H06 |
| `3 · secretos` | Secretos | Siempre | Gitleaks encuentra un secreto o un certificado fuera de la CA de prueba | — | B00-H07 (detalle en `specs/008-secretos-sca-sbom/contracts/secretos.md`) |
| `4 · sbom-backend` | SCA | Si el PR no es borrador | No se pueden generar los SBOM, o falta o está vacío el de un módulo | — | B00-H07 (detalle en `specs/008-secretos-sca-sbom/contracts/sca.md`) |
| `4 · sbom-frontend` | SCA | `deteccion.frontend == 'true'` y el PR no es borrador | No se puede generar el SBOM del frontend | — | B00-H06 y B00-H07 |
| `4 · sca` | SCA | `sbom-backend` termina en éxito **y** `sbom-frontend` termina en éxito u omitido | Grype encuentra una vulnerabilidad alta o crítica con versión corregida, sin una excepción vigente (D-54) | Caso límite «repositorio sin frontend» | B00-H07 (detalle en `specs/008-secretos-sca-sbom/contracts/sca.md`) |

### Garantías

1. **La CI corre en cada pull request hacia `main`**, cuando se abre, se actualiza, se reabre o pasa a listo para revisión. No hay filtro `paths`, así que todos los checks obligatorios informan siempre un resultado.
2. **Sin `frontend/package.json`:**
   - `1 · lint-frontend`, `2 · test-frontend` y `4 · sbom-frontend` quedan como *skipped*;
   - ningún check del backend se omite por eso;
   - `4 · sca` revisa igualmente el SBOM del backend.
3. **Con `frontend/package.json`:** los jobs de frontend corren en ese mismo pull request, sin cambiar el workflow.
4. **Seguridad del workflow** (principio IX):
   - permisos `contents: read`;
   - toda acción `uses:` fijada por SHA completo de 40 caracteres, con la etiqueta como comentario (`pinact`);
   - no se usa `pull_request_target`.
5. **Nombres estables:** los 10 nombres de la tabla son parte del contrato. Para cambiarlos hay que actualizar el ruleset en el mismo pull request.

### Salida del job `0 · deteccion`

| Salida | Valores | Regla |
|---|---|---|
| `frontend` | `true` o `false` | `true` si y solo si existe `frontend/package.json` en el commit del pull request |

## Ruleset `main`

`PUT /repos/paul-g-software-ucacue/tesis-prototipo-2026/rulesets/24901935`. Es el estado completo después de B00-H01: el `PUT` reemplaza las reglas, así que se envían todas.

```json
{
  "name": "main",
  "target": "branch",
  "enforcement": "active",
  "bypass_actors": [],
  "conditions": { "ref_name": { "include": ["~DEFAULT_BRANCH"], "exclude": [] } },
  "rules": [
    { "type": "deletion" },
    { "type": "non_fast_forward" },
    {
      "type": "pull_request",
      "parameters": {
        "allowed_merge_methods": ["squash"],
        "dismiss_stale_reviews_on_push": false,
        "require_code_owner_review": false,
        "require_last_push_approval": false,
        "required_approving_review_count": 0,
        "required_review_thread_resolution": false,
        "require_extra_approval_for_unattributed_changes": true,
        "required_reviewers": []
      }
    },
    {
      "type": "required_status_checks",
      "parameters": {
        "strict_required_status_checks_policy": true,
        "do_not_enforce_on_create": false,
        "required_status_checks": [
          { "context": "0 · deteccion",       "integration_id": 15368 },
          { "context": "1 · lint-backend",    "integration_id": 15368 },
          { "context": "1 · lint-frontend",   "integration_id": 15368 },
          { "context": "2 · test-backend",    "integration_id": 15368 },
          { "context": "2 · test-integracion","integration_id": 15368 },
          { "context": "2 · test-frontend",   "integration_id": 15368 },
          { "context": "3 · secretos",        "integration_id": 15368 },
          { "context": "4 · sbom-backend",    "integration_id": 15368 },
          { "context": "4 · sbom-frontend",   "integration_id": 15368 },
          { "context": "4 · sca",             "integration_id": 15368 }
        ]
      }
    }
  ]
}
```

### Garantías

1. **No se fusiona un pull request con un check obligatorio en `failure`**, `cancelled` o pendiente (escenarios 1, 3 y 4; FR-009).
2. **El pull request tiene que estar al día con `main`** (política estricta). Junto con la fusión *squash*, esto hace que el commit de `main` sea el mismo código que pasó las etapas 1 a 4, que es lo que supone `cd.yml`.
3. **`main` solo recibe cambios por pull request.** No se puede borrar ni reescribir su historial.
4. **Sin excepciones:** el ruleset no tiene actores con permiso para saltárselo (`bypass_actors: []`, enviado de forma explícita), así que la regla también vale para el dueño del repositorio. Se verifica después del `PUT`:

   ```bash
   gh api repos/paul-g-software-ucacue/tesis-prototipo-2026/rulesets/24901935 \
     -q '{bypass: .bypass_actors, metodos: (.rules[]|select(.type=="pull_request")|.parameters.allowed_merge_methods)}'
   # esperado: {"bypass":[],"metodos":["squash"]}
   ```
5. **Excepción transitoria:** si `3 · secretos`, `4 · sbom-backend` o `4 · sca` fallan en el pull request de B00-H01 por algo que es de B00-H07, se quitan de la lista, se anota en `evidencia.md` y B00-H07 los vuelve a agregar (research §8).
6. **Se conservan las reglas que ya tenía:** prohibido borrar `main` o reescribir su historial, pull request obligatorio y `require_extra_approval_for_unattributed_changes: true`. El `PUT` reemplaza todas las reglas, así que el JSON las incluye todas.

`integration_id` 15368 es la GitHub App de GitHub Actions. Así, un check con el mismo nombre publicado por otra fuente no satisface la regla.
