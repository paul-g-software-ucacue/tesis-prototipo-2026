# Quickstart: Registrar cada cambio en la bitácora

## Pruebas del backend

```bash
cd backend
./gradlew check jacocoTestReport   # unitarias, contrato, arquitectura, Spotless y SpotBugs
./gradlew integrationTest           # Testcontainers con PostgreSQL real
```

Lo que tiene que pasar, además de lo que ya pasaba:

- **Usuarios:** `EntradaBitacora` (invariantes y datos sensibles), el receptor con los seis tipos, la idempotencia, y la tabla que rechaza `UPDATE`, `DELETE` y `TRUNCATE`.
- **Cada servicio emisor:** cada acción respeta su clase (crea, cambia o quita), y un cambio rechazado no publica nada.
- **`:plataforma:eventos`:** la entrega local, y su fallo como `EntregaFallida`.
- **Contrato:** ninguna operación de `contratos/` borra ni modifica la bitácora; el código nuevo está en el catálogo.
- **Arquitectura:** Roles no depende de Usuarios; Spring Modulith verifica `identidad`.

## De punta a punta

```bash
docker compose up -d --build --wait
deploy/probar_bitacora.sh
```

Recorre los 11 pasos de [contracts/demostracion.md](contracts/demostracion.md): el escenario del issue (4 → 6 horas), crear y quitar, un rechazo sin entrada, un cambio por servicio, una carga, la caída de `identidad` con 10 cambios, la repetición, los intentos de borrar o modificar, la búsqueda de secretos y el tiempo.

## A mano, en la demo

No hay pantalla de la bitácora: llega con F17-H02. La comprobación del autor usa la SPA para los cambios y una consulta de solo lectura para verlos.

Si la demo está vacía (por ejemplo, después de reinstalar el equipo), `sudo deploy/equipo/poblar_demo.sh` carga los datos de prueba y deja la actividad 140-b con 4 horas; al final dice qué carrera y qué docente usar.

1. Entra con `coordinacion.demo` y cambia las horas de una actividad no lectiva de 4 a 6.
2. Consulta las entradas recientes:

   ```sql
   SELECT servicio, accion, usuario, momento, periodo, anterior, nuevo
   FROM usuarios.bitacora ORDER BY momento DESC LIMIT 5;
   ```

   Debe aparecer `ACTIVIDAD_GUARDADA` con `{"horas": "4"}` y `{"horas": "6"}`.
3. Con `vicerrectorado.demo`, en **Periodos**, designa al Responsable de consolidación del periodo y vuelve a consultar: aparece la entrada de `roles`.
4. Intenta borrar una entrada, primero con el rol del proceso y después con el superusuario. Desde `docker exec -it mdd-postgres-1 psql -U postgres -d mdd`, una instrucción por línea:

   ```sql
   SET ROLE identidad;
   DELETE FROM usuarios.bitacora WHERE accion = 'ACTIVIDAD_GUARDADA' AND nuevo ->> 'horas' = '6';
   RESET ROLE;
   DELETE FROM usuarios.bitacora WHERE accion = 'ACTIVIDAD_GUARDADA' AND nuevo ->> 'horas' = '6';
   ```

   Con el rol del proceso falla con «permission denied for table bitacora», porque no tiene el permiso. Con el superusuario falla con «La bitácora no se puede modificar ni borrar.», por el disparador.

## Evidencia

Los resultados van a `specs/039-bitacora-cambios/evidencia.md`, con el mismo formato que las historias anteriores: pruebas, cobertura por módulo, el script, el despliegue en el equipo de demostración y la comprobación del autor.
