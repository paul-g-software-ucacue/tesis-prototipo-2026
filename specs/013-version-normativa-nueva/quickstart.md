# Quickstart: Crear una versión nueva de la normativa (F04-H02)

Lo que garantiza cada pieza está en [data-model.md](data-model.md) y [contracts/normativa.md](contracts/normativa.md).

## 1. Pruebas

```bash
cd backend && ./gradlew check :servicios:normativa:integrationTest :servicios:roles:integrationTest
cd ../frontend && npm run test -- --run --coverage && npm run test:e2e
```

Resultado esperado: todo en verde, con el 60 % de líneas por módulo. En particular:
- la versión 2 se guarda y se reconstruye igual, y la 1 queda idéntica;
- dos creaciones simultáneas dan números distintos;
- una versión con un periodo no se edita (409);
- `VerificadorDePermisos` da 403 en las seis operaciones sin su permiso.

## 2. En local, con el perfil

```bash
docker compose up -d --build --wait
deploy/probar_normativa.sh
```

Resultado esperado: los pasos de F04-H01 más los nuevos en `OK`:
- **versión nueva:** `vicerrectorado.demo` asocia la versión 1 a un periodo de prueba y crea la versión 2 a partir de ella; cambia las horas de clase del docente de medio tiempo; la 2 tiene el cambio y la 1 sigue idéntica (SC-002);
- **versión en uso:** editar la 1 da 409 `NORMATIVA_VERSION_EN_USO` (SC-003);
- **periodo con versión:** asociar la 2 al mismo periodo da 409;
- **limpieza:** al final, el script borra solo las versiones y el periodo de prueba que creó.

## 3. Revisión manual (SC-001)

1. Entrar con `vicerrectorado.demo` y abrir «Normativa».
2. Crear la versión 2 a partir de la 1 y cambiar las horas de clase del docente de medio tiempo, en menos de 2 minutos.
3. Comprobar que la versión 1 sigue con sus valores.

## 4. En el equipo de demostración, después de fusionar

Repetir el paso 3 en `https://mdd.localhost`. El Vicerrector necesita volver a entrar para traer el permiso nuevo (D-62).
