# Quickstart: validar B00-H03

Guía para comprobar los cuatro escenarios. Las garantías están en [contracts/](contracts/) y las decisiones en [research.md](research.md). Los resultados van a `evidencia.md`.

## Requisitos previos

- Lo mismo que en B00-H02: JDK 25, Gradle con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`, y Docker Engine con Docker Compose v2.24 o posterior.
- `curl` y `jq` en la máquina anfitriona, para los scripts.

## 1. Contratos, construcción y pruebas

```bash
cd backend
./gradlew build integrationTest
```

**Resultado esperado:**
- todo en verde;
- las interfaces se generan desde `contratos/`;
- pasan las pruebas de seguridad de `plataforma:seguridad` y del API Gateway, que cubren todas las causas de 401, incluido el token vencido;
- pasan las de Usuarios: dominio, aplicación, JPA, Argon2, JWT y contrato;
- pasan las de contrato de `cargas`, `generacion` y `dashboard`;
- cobertura del 60 % o más en cada módulo.

## 2. Perfil levantado y punta a punta (escenarios 1 a 3)

```bash
docker compose up -d --wait
deploy/probar_autenticacion.sh; echo "salida=$?"
```

**Resultado esperado:** los 6 pasos de [contracts/medicion-r19-sesion.md](contracts/medicion-r19-sesion.md) en `OK` y `salida=0`.

A mano, por ejemplo:

```bash
curl -si localhost:8080/api/cargas/sesion                       # 401 con SEGURIDAD_NO_AUTENTICADO
TOKEN=$(curl -s localhost:8080/api/usuarios/sesion -H 'Content-Type: application/json' \
  -d '{"usuario":"autor.prueba","clave":"cambiar-en-local-usuario-prueba"}' | jq -r .token)
curl -s localhost:8080/api/generacion/sesion -H "Authorization: Bearer $TOKEN"   # usuario y roles
```

## 3. Memoria después de iniciar sesión (escenario 4)

```bash
deploy/medir_memoria.sh --con-sesion | tee "$TMPDIR/medicion-sesion.md"; echo "salida=$?"
```

**Resultado esperado:** el total queda por debajo de 8 GiB y la salida es 0. Copia la tabla a `evidencia.md`, junto con la diferencia respecto de los 931,6 MiB de B00-H02. La referencia es el run de `perfil-consolidado` del pull request.

## 4. Lo que no debe aparecer

```bash
docker compose logs | grep -c "$TOKEN"                  # 0
docker compose logs | grep -ci 'cambiar-en-local-usuario-prueba'   # 0
git grep -n 'BEGIN .*PRIVATE KEY'                      # sin resultados
```

## 5. Cierre

- `evidencia.md` tiene las pruebas, la salida de `probar_autenticacion.sh`, las mediciones y las búsquedas del paso 4.
- El pull request de B00-H03 tiene la CI en verde, con `3 · secretos`, y `perfil-consolidado` en verde.
