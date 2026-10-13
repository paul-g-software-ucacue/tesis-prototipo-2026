# Contrato: catálogo de códigos

Es lo que garantiza el catálogo: cómo se registra un error y cómo se comprueba que el código y la documentación dicen lo mismo (FR-002, FR-004, FR-011; ADR-016; research §2 a §4).

## `contratos/comun/errores.md`

Es el catálogo normativo: lo leen el autor, las especificaciones y la SPA. Tiene una tabla `Código | Estado | Título | Descripción | Cuándo`, con una fila por código. Después de esta historia lleva:
- `SEGURIDAD_NO_AUTENTICADO` y `USUARIOS_CREDENCIALES_INVALIDAS` (B00-H03, D-38), sin cambios;
- `COMUN_ERROR_INTERNO`, `COMUN_SOLICITUD_INVALIDA`, `COMUN_CUERPO_ILEGIBLE`, `COMUN_RECURSO_NO_ENCONTRADO`, `COMUN_METODO_NO_PERMITIDO`, `COMUN_FORMATO_NO_ACEPTADO`, `COMUN_TIPO_NO_SOPORTADO` y `COMUN_SERVICIO_NO_DISPONIBLE`.

**Reglas de los códigos:** en mayúsculas con guion bajo, con el prefijo de su servicio; `COMUN_` y `SEGURIDAD_` son solo de la plataforma. Un código no se renombra ni se reutiliza.

## Registrar un error de un servicio

1. La excepción vive en el dominio o en la aplicación del servicio, como clase Java común, sin HTTP.
2. Su fila se agrega a `errores.md`.
3. Se agrega una `EntradaCatalogo` en la clase `Errores<Servicio>` de `adaptadores/entrada`, que implementa `CatalogoDeErrores` y lleva `@Component`.
4. Su respuesta se declara en el contrato de la operación que puede producirla.
5. La prueba de contrato cubre esa respuesta, y la prueba del catálogo del servicio (`VerificadorDeCatalogo`) comprueba que coincide con `errores.md`.

## `VerificadorDeCatalogo` (`plataforma:pruebas`)

```java
VerificadorDeCatalogo.verificar(new ErroresUsuarios());      // en las pruebas de Usuarios
VerificadorDeCatalogo.verificar(CodigosComunes.TODOS);       // en las pruebas de plataforma:errores
```

Lee la tabla de `errores.md` y, por cada entrada, falla si el código no está, o si el estado, el título o la descripción no son iguales a los de su fila.

## Garantías

1. Cada código que puede salir de una aplicación está en `errores.md` con el mismo estado y los mismos textos.
2. Dos servicios no pueden registrar el mismo código ni la misma excepción en un proceso: el proceso no arranca.
3. El dominio y la aplicación no dependen del catálogo ni de HTTP (principio I).
