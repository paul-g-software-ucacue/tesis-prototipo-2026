# Quickstart: La política 2024 como versión 1 de normativa (F04-H01)

Lo que garantiza cada pieza está en [data-model.md](data-model.md) y [contracts/normativa.md](contracts/normativa.md).

## 1. Pruebas del backend y del frontend

```bash
cd backend && ./gradlew check :servicios:normativa:integrationTest :servicios:roles:integrationTest
cd ../frontend && npm run test -- --run --coverage && npm run test:e2e
```

Resultado esperado: todo en verde, con el 60 % de líneas por módulo. En particular:
- la versión 1 coincide, fila por fila y valor por valor, con la carga 7 de la política (SC-001);
- la versión 1 se reconstruye con el agregado, así que cumple sus invariantes;
- `VerificadorDePermisos` da 403 en las dos operaciones sin `PARAMETROS:LECTURA`;
- la matriz D-16 tiene el permiso nuevo del Vicerrector (D-66).

## 2. En local, con el perfil

```bash
docker compose up -d --build --wait
deploy/probar_normativa.sh
```

Resultado esperado, todos los pasos en `OK`:
- `vicerrectorado.demo` y `admin.demo` ven una sola versión, la 1, con 22 filas, el tipo de IST T2 y las 7 reglas;
- `docente.demo` recibe el 403 común;
- después de reiniciar `datos`, sigue habiendo una sola versión, igual (SC-003).

## 3. Revisión manual (SC-002)

1. Entrar con `vicerrectorado.demo`: el menú tiene «Normativa».
2. Abrir la versión 1, en menos de un minuto desde que se inicia sesión. Se ven la cabecera (Política SENESCYT 2024, `SENESCYT-2024-03`, vigente desde el 11/03/2024, Tipo 2), los parámetros por regla y las 22 filas en español.
3. Entrar con `docente.demo` y abrir `/normativa`: «Acceso no autorizado».

## 4. En el equipo de demostración, después de fusionar

1. Esperar el despliegue automático: «Sano en N s».
2. Repetir el paso 3 con `vicerrectorado.demo`. Su clave está en `sudo cat /opt/mdd/secretos/claves-demo`.
