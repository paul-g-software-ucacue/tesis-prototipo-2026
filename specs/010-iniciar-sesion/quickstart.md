# Quickstart: Iniciar sesión (F05-H01)

Lo que garantiza cada pieza está en [contracts/usuarios.md](contracts/usuarios.md) y en [data-model.md](data-model.md).

## 1. Pruebas del backend y del frontend

```bash
cd backend && ./gradlew :servicios:usuarios:check :apps:identidad:check
cd ../frontend && npm run test -- --run --coverage && npm run test:e2e
```

Resultado esperado: todo en verde, con el 60 % de líneas por módulo; la regla de bloqueo, la renovación y la carga de la demo cubiertas.

## 2. En local, con el perfil

```bash
docker compose up -d --build --wait
deploy/probar_autenticacion.sh
```

Resultado esperado: los pasos de siempre más los nuevos en `OK`:
- paso 7: los 11 usuarios de la demo (clave `cambiar-en-local-demo`) entran con su nombre y exactamente sus roles;
- paso 8: con un nombre inventado y con `ocs.demo`, 5 intentos fallidos y el sexto con 429 `USUARIOS_INICIO_BLOQUEADO`, con las mismas respuestas (al final se borran los intentos de `ocs.demo`, así que el script se puede repetir);
- paso 9: la renovación devuelve un token que sirve, y con un token alterado responde el 401 uniforme.

## 3. Revisión manual (SC-002, SC-004)

1. Abrir `http://localhost:8081/inicio` sin sesión: lleva al inicio de sesión.
2. Entrar con `vicerrectorado.demo` y `cambiar-en-local-demo`: vuelve a `/inicio` con «Hola, Vicerrectorado académico (demo)» y el rol «Vicerrector académico».
3. Cerrar sesión y probar una contraseña incorrecta: «Credenciales inválidas», sin decir cuál dato falló.

## 4. En el equipo de demostración

1. **Antes de fusionar**, desde la rama `010-iniciar-sesion`: `sudo deploy/equipo/preparar.sh`. En un equipo ya preparado solo crea `secretos/claves-demo` (dueño 10001, grupo `mdd`, modo 0440), sin tocar lo demás. Si una historia futura agrega usuarios de la demo, volver a correrlo: agrega sus claves sin tocar las existentes.
2. Fusionar y esperar el despliegue automático (`journalctl -u mdd-actualizar.service`): «Sano en N s».
3. `sudo cat /opt/mdd/secretos/claves-demo` muestra las claves de los 11 usuarios (no pegarlas en ningún lado).
4. En `https://mdd.localhost`, entrar con `vicerrectorado.demo` y su clave; `autor.prueba` ya no entra (SC-006).

Si se fusionó sin correr `preparar.sh`, el despliegue falla sin crear nada («Docker no pudo levantar la versión») y vuelve a la versión anterior. Se arregla con el paso 1 y `sudo -u mdd env RAIZ=/opt/mdd /opt/mdd/bin/actualizar.sh --forzar`.

## 5. Lo que no debe pasar

- Que una contraseña aparezca en el repositorio, una imagen o un registro.
- Que el mensaje de bloqueo o el de credenciales inválidas cambie según exista o no el usuario.
- Que el token aparezca en `localStorage`, `sessionStorage`, cookies o la URL.
