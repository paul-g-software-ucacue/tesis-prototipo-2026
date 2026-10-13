# Contrato: pruebas de punta a punta y medición de R-19 después de iniciar sesión

Es lo que garantizan `deploy/probar_autenticacion.sh` y `deploy/medir_memoria.sh --con-sesion` (escenarios 1 a 4; D-34, D-36). Amplía el contrato de B00-H02 (`specs/002-compose-cuatro-procesos/contracts/medicion-r19.md`) sin cambiar su formato.

## `deploy/probar_autenticacion.sh`

```bash
deploy/probar_autenticacion.sh      # con el perfil levantado; usa las variables de .env.ejemplo (y .env si existe)
```

Pasos, en orden, contra `http://localhost:8080` (el API Gateway):

| # | Petición | Esperado |
|---|---|---|
| 1 | `GET /api/cargas/sesion` sin token | 401, cuerpo de [autenticacion.md](autenticacion.md), `WWW-Authenticate: Bearer` |
| 2 | `POST /api/usuarios/sesion` con el usuario de prueba | 200 con `token` |
| 3 | `GET /api/{usuarios,cargas,generacion,dashboard}/sesion` con el token | 200 con `usuario: autor.prueba` y `roles: [COORDINADOR_CARRERA, DOCENTE]` en los 4 |
| 4 | `GET /api/generacion/sesion` con el token y un carácter de la firma cambiado | 401 con el mismo cuerpo que el paso 1 |
| 5 | `GET /api/dashboard/sesion` directo al proceso salida, sin token (`docker compose exec salida bash -c …`) | 401 con el mismo cuerpo |
| 6 | `POST /api/usuarios/sesion` con una contraseña incorrecta | 401 con `USUARIOS_CREDENCIALES_INVALIDAS` |

Imprime una línea por paso (`OK` o `FALLA`, con lo esperado y lo recibido) y termina con 0 si todos pasan y con 1 si alguno falla. No imprime el token ni la contraseña.

## `deploy/medir_memoria.sh --con-sesion`

Igual que sin la opción, con estos cambios:

1. Después de `docker compose up -d --wait`, inicia sesión con el usuario de prueba a través del API Gateway; si falla, termina con código 2.
2. Al calentamiento de la salud le suma 20 llamadas a la «sesión actual» de cada proceso, con el token.
3. La primera línea de la tabla dice `**Medición de R-19 después de iniciar sesión**`.
4. Los umbrales y los códigos de salida no cambian: menos de 8 GiB cumple y más de 7 GiB es alerta.

## `.github/workflows/perfil-consolidado.yml`

Después de `docker compose up -d --wait` agrega dos pasos, en este orden, los dos con `shell: bash`:
- `deploy/probar_autenticacion.sh`;
- `deploy/medir_memoria.sh --con-sesion`, en lugar de la medición sin sesión, con su tabla en el resumen del run y en el artefacto `medicion-r19`.

El filtro `paths` agrega `contratos/**`.

## Garantías

1. Los escenarios 1 a 3 se demuestran de punta a punta en el perfil levantado, en local y en el runner.
2. La medición después de iniciar sesión se compara con la de B00-H02 en el mismo formato.
3. Ni el token ni la contraseña aparecen en la salida de los scripts ni en el resumen del run.
