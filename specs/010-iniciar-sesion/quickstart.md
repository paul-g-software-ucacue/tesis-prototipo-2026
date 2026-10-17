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
- `docente.coordinador.demo` (clave `cambiar-en-local-demo`) entra con DOCENTE y COORDINADOR_CARRERA y su nombre;
- con un nombre inventado, 5 intentos fallidos y el sexto con 429 `USUARIOS_INICIO_BLOQUEADO`;
- la renovación devuelve un token nuevo, y con un token vencido responde 401.

## 3. Revisión manual (SC-002, SC-004)

1. Abrir `http://localhost:8081/inicio` sin sesión: lleva al inicio de sesión.
2. Entrar con `vicerrectorado.demo` y `cambiar-en-local-demo`: vuelve a `/inicio` con «Hola, Vicerrectorado académico (demo)» y el rol «Vicerrector académico».
3. Cerrar sesión y probar una contraseña incorrecta: «Credenciales inválidas», sin decir cuál dato falló.

## 4. En el equipo de demostración, después de fusionar

1. `sudo deploy/equipo/preparar.sh` (ya preparado: solo crea `secretos/claves-demo`, sin tocar lo demás). Si una historia futura agrega usuarios de la demo, volver a correrlo: agrega sus claves sin tocar las existentes.
2. Esperar el despliegue automático (`journalctl -u mdd-actualizar.service`): «Sano en N s».
3. `sudo cat /opt/mdd/secretos/claves-demo` muestra las claves de los 11 usuarios (no pegarlas en ningún lado).
4. En `https://mdd.localhost`, entrar con `vicerrectorado.demo` y su clave; `autor.prueba` ya no entra (SC-006).

## 5. Lo que no debe pasar

- Que una contraseña aparezca en el repositorio, una imagen o un registro.
- Que el mensaje de bloqueo o el de credenciales inválidas cambie según exista o no el usuario.
- Que el token aparezca en `localStorage`, `sessionStorage`, cookies o la URL.
