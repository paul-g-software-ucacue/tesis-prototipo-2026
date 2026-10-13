# Quickstart: validar B00-H04

Guía para comprobar los tres escenarios. Las garantías están en [contracts/](contracts/) y las decisiones en [research.md](research.md). Los resultados van a `evidencia.md`.

## Requisitos previos

Los mismos que en B00-H03: JDK 25, Gradle, Docker Engine con Docker Compose, `curl` y `jq`.

## 1. Construcción y pruebas

```bash
cd backend
./gradlew build integrationTest
```

**Resultado esperado:**
- todo en verde;
- las pruebas de `plataforma:errores` cubren:
  - el error de dominio de prueba (escenario 1) y el que no está registrado;
  - un error técnico (escenario 2) con su `idIncidente` en el registro;
  - la validación con varios campos (escenario 3);
  - los errores del framework y la página `/error`;
- las pruebas de contrato de Usuarios cubren el 400 y el 500;
- `VerificadorDeCatalogo` pasa en Usuarios, en `plataforma:errores` y en `plataforma:seguridad`;
- la prueba de integración de identidad, con la base detenida, da 500 con `idIncidente`;
- las pruebas del API Gateway cubren 404, 503 y 500.

## 2. Sobre el perfil levantado

```bash
docker compose up -d --wait --build
deploy/probar_autenticacion.sh                      # sigue pasando (SC-005)
TOKEN=$(curl -s localhost:8080/api/usuarios/sesion -H 'Content-Type: application/json' \
  -d '{"usuario":"autor.prueba","clave":"cambiar-en-local-usuario-prueba"}' | jq -r .token)

# Escenario 3: datos vacíos → 400 con detalles (D-42)
curl -s localhost:8080/api/usuarios/sesion -H 'Content-Type: application/json' -d '{"usuario":""}' | jq

# Ruta inexistente con token → 404 COMUN_RECURSO_NO_ENCONTRADO
curl -s localhost:8080/api/cargas/no-existe -H "Authorization: Bearer $TOKEN" | jq

# Proceso detenido detrás del API Gateway → 503 COMUN_SERVICIO_NO_DISPONIBLE (D-40)
docker compose stop salida
curl -s localhost:8080/api/dashboard/sesion -H "Authorization: Bearer $TOKEN" | jq
docker compose start salida

# Escenario 2: base detenida → 500 con idIncidente, y el mismo id en el registro
docker compose stop postgres
ID=$(curl -s localhost:8080/api/usuarios/sesion -H 'Content-Type: application/json' \
  -d '{"usuario":"autor.prueba","clave":"x"}' | jq -r .idIncidente)
docker compose logs identidad | grep "$ID"
docker compose start postgres
```

**Resultado esperado:** cada respuesta tiene la forma del esquema `Error`, sin `timestamp`, `path`, `trace` ni nombres de clases. El `idIncidente` del 500 aparece en el registro de identidad junto con la causa.

## 3. Cierre

- `evidencia.md` tiene las pruebas, las respuestas del paso 2 y la línea del registro con el `idIncidente`.
- El pull request de B00-H04 tiene la CI en verde y `perfil-consolidado` en verde.
