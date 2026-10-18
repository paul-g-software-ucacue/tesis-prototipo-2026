# Quickstart: Roles con permisos (F05-H02)

Lo que garantiza cada pieza está en [contracts/roles.md](contracts/roles.md) y en [data-model.md](data-model.md).

## 1. Pruebas del backend y del frontend

```bash
cd backend && ./gradlew check :servicios:roles:integrationTest :servicios:usuarios:integrationTest
cd ../frontend && npm run test -- --run --coverage && npm run test:e2e
```

Resultado esperado: todo en verde, con el 60 % de líneas por módulo. En particular:
- la prueba de la matriz D-16 compara, rol por rol, la base con `data-model.md` (SC-002);
- `VerificadorDePermisos` da 403 en cada operación con `x-mdd-permiso` cuando falta ese permiso (SC-003).

## 2. En local, con el perfil

```bash
docker compose up -d --build --wait
deploy/probar_autenticacion.sh
```

Resultado esperado: los pasos de F05-H01 más los nuevos en `OK`:
- **rol nuevo:** `admin.demo` crea «Auditor de roles» con solo `USUARIOS_ROLES:LECTURA` y se lo asigna a `docente.demo`; la nueva sesión de `docente.demo` trae ese permiso;
- **solo lectura:** con esa sesión, `docente.demo` lista los roles (200), pero crear uno da 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, y el rol no se crea;
- **varios roles:** `docente.coordinador.demo` trae en `permisos` los de Docente y los de Coordinador de carrera, sin repetir;
- **limpieza:** al final, el script le quita el rol a `docente.demo` y lo borra, así que se puede repetir.

## 3. Revisión manual (SC-001, SC-004, SC-005)

1. Entrar con `admin.demo`: el menú tiene «Roles». Crear un rol con 3 permisos y asignárselo a `docente.demo`, en menos de 2 minutos.
2. Abrir uno de los roles de D-16: se ve su matriz, pero no se puede editar ni borrar.
3. Entrar con `docente.coordinador.demo`: «Tus funciones» muestra las de los dos roles, sin repetir.
4. Entrar con `docente.demo` y abrir `/roles`: «Acceso no autorizado», sin datos de los roles.

## 4. En el equipo de demostración, después de fusionar

1. Esperar el despliegue automático: «Sano en N s».
2. Con `admin.demo` (su clave en `sudo cat /opt/mdd/secretos/claves-demo`), repetir el paso 3.1. Después, reiniciar `identidad` y comprobar que el rol y la asignación siguen ahí (SC-006).

## 5. Lo que no debe pasar

- Que una operación que cambia datos responda algo distinto de 403 a un usuario sin el permiso.
- Que el 403 diga qué permiso faltó o muestre códigos internos en la pantalla.
- Que un reinicio o un despliegue devuelva a los usuarios de la demo los roles del archivo después de que el administrador los cambió.
