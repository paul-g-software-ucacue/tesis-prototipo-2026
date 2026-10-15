# Quickstart: Eventos persistentes entre procesos (B00-H05)

Cómo comprobar los tres escenarios. Lo que garantiza cada pieza está en [contracts/entrega.md](contracts/entrega.md) y [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- Docker con Compose, `curl` y `jq`.
- JDK 25. Gradle se ejecuta con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25` si el JDK por defecto es otro.
- **Un volumen de PostgreSQL nuevo**, porque esta historia agrega los esquemas `eventos_<proceso>` a `deploy/postgres/01-esquemas.sh`:

  ```bash
  docker compose down -v
  ```

## 1. Pruebas automáticas

```bash
cd backend
./gradlew check            # unitarias, de arquitectura, de contrato y cobertura del 60 %
./gradlew integrationTest  # registro, reenvío, reversión e idempotencia con PostgreSQL (Testcontainers)
```

Resultado esperado:
- todo en verde;
- `plataforma:eventos` cubre la entrega, el receptor caído y su vuelta, la reversión y las publicaciones atascadas;
- Usuarios cubre el registro único con entregas repetidas y simultáneas, y el token de proceso;
- `plataforma:seguridad` cubre la separación de tokens.

## 2. De punta a punta

```bash
docker compose up -d --build --wait
deploy/probar_eventos.sh
```

Resultado esperado: seis líneas `OK` (pasos 0 a 5) y código de salida 0. El paso 3 tarda hasta 75 s, porque espera el reenvío de la tarea programada.

## 3. Revisión manual (opcional)

Publicaciones del proceso distributivo:

```bash
docker compose exec -T postgres psql -U postgres -d mdd -c \
  "SELECT status, completion_attempts, publication_date, completion_date FROM eventos_distributivo.event_publication ORDER BY publication_date"
```

Eventos registrados por Usuarios:

```bash
docker compose exec -T postgres psql -U postgres -d mdd -c \
  "SELECT id_evento, tipo, origen, recibido_en FROM usuarios.evento_recibido ORDER BY recibido_en"
```

Registro del emisor durante la caída de identidad: líneas WARN con `idEvento`, `tipo` y `destino`, sin el contenido ni el token:

```bash
docker compose logs distributivo | grep -i evento
```

## 4. Lo que no debe pasar

- Una publicación de la caída de identidad que nunca llega a `COMPLETED`.
- Más de una fila en `usuarios.evento_recibido` para el mismo `idEvento`.
- `/interno/usuarios/eventos` respondiendo algo distinto de 401 o 403 a través del API Gateway.
- El endpoint de prueba respondiendo algo distinto de 404 con `MDD_EVENTOS_PRUEBA_HABILITADO=false`.
- El contenido del evento, la clave del proceso o un token en el registro.
- La memoria del perfil por encima de la alerta de 7 GiB (R-19). Se mide en el PR de evidencia.
