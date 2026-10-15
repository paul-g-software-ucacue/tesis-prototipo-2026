# Quickstart: SPA base con el cliente generado (B00-H06)

Lo que garantiza cada pieza está en [contracts/interfaz.md](contracts/interfaz.md) y [contracts/perfil.md](contracts/perfil.md).

## Requisitos

- Node 24 (`frontend/.nvmrc`). En el equipo del autor, el binario oficial en `~/.local/node-24` o el contenedor `node:24-alpine` sobre una copia (research §13).
- Docker con Compose, `curl` y `jq`, para el perfil.

## 1. Pruebas del frontend

```bash
cd frontend
npm ci
npm run lint && npm run formato:verificar && npm run tipos
npm run test -- --run --coverage     # umbral de 60 % de líneas
npx playwright install chromium
npm run test:e2e                     # accesibilidad (axe-core) y flujos con la API simulada
npm run verificar:ruptura            # escenario 1: dos cambios de contrato rompen la compilación
```

Resultado esperado:
- todo en verde;
- axe-core sin fallas en `/iniciar-sesion`, en sus tres estados;
- `verificar:ruptura` dice que los dos cambios rompieron la compilación y que el contrato real compila.

## 2. Con el perfil

```bash
docker compose up -d --build --wait
deploy/probar_spa.sh
```

Resultado esperado: cinco líneas `OK` y código de salida 0.

## 3. Revisión manual (SC-005)

1. Abrir `http://localhost:8081/` en Chrome, Firefox o Edge, con al menos 1366 px de ancho.
2. Ver que redirige a «Iniciar sesión».
3. Entrar con `autor.prueba` y la clave de `.env.ejemplo`.
4. Ver «Hola, autor.prueba» con sus roles.
5. Abrir las herramientas del navegador y comprobar que el token no está en el almacenamiento local, el de la sesión ni las cookies.
6. Recargar: vuelve a «Iniciar sesión».
7. Entrar otra vez y usar «Cerrar sesión».

Una contraseña incorrecta muestra «Credenciales inválidas» sin decir cuál dato falló.

## 4. Lo que no debe pasar

- Un estado HTTP, un código `COMUN_…`, una traza o un mensaje en inglés en pantalla.
- El token en `localStorage`, `sessionStorage`, cookies o la URL.
- Una ruta interna visible sin sesión.
- La compilación pasando con un contrato que cambió un campo que la SPA usa.
- La memoria del perfil por encima de la alerta de 7 GiB (R-19); se mide en el pull request.
