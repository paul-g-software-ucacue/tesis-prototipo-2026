# Investigación (fase 0): B00-H03 · Autenticación JWT de punta a punta

**Fecha:** 2026-10-13 · **Especificación:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Fuentes leídas:
- constitución 2.1.0 (principios I, IV, V, VIII, IX, XI y XIII; sección 3) y `docs/decisiones.md` (D-34 a D-39, ADR-013);
- lo que dejó B00-H02: plantillas, convenciones, `compose.yaml`, `java.Dockerfile` y `medir_memoria.sh`;
- el BOM de Spring Boot 4.1.1 (Spring Security 7.1.1, Hibernate 7.4.5) y Maven Central.

Las decisiones que afectan a varias features están en **ADR-014** (autenticación) y **ADR-015** (contratos OpenAPI), aprobados; ver §14.

---

## 1. Formato del token

- **Decisión:** JWT firmado con **RS256**, con estos *claims*:

  | Claim | Contenido |
  |---|---|
  | `iss` | `mdd-usuarios`, el emisor |
  | `sub` | el nombre de usuario |
  | `roles` | la lista de roles del usuario, por ejemplo `["DOCENTE", "COORDINADOR_CARRERA"]` |
  | `iat` y `exp` | emisión y vencimiento |

  El encabezado lleva `kid`, la huella SHA-256 de la llave pública, para que quien valida sepa con qué llave hacerlo.
- **Vigencia:** 30 minutos por defecto, configurable con `mdd.usuarios.token.vigencia`. No hay renovación ni cierre de sesión (spec, Assumptions).
- **Por qué:** son los datos mínimos que pide el escenario «Token válido» (el usuario y sus roles), y RS256 es el algoritmo que fija la constitución (sección 3).
- **Alternativas consideradas:** HS256 con un secreto compartido, que exigiría repartir el secreto que firma a todos los procesos; un token opaco con introspección, que agrega una llamada a Usuarios en cada petición.

## 2. Llaves: quién firma y cómo validan los demás

- **Decisión:**
  - Solo Usuarios tiene la llave privada, RSA de 2048 bits, y publica la pública como **JWKS** en `GET /api/usuarios/jwks`.
  - El API Gateway y los procesos la obtienen de `http://identidad:8080/api/usuarios/jwks`, dentro de la red de Compose. La primera vez la piden cuando reciben un token, y la guardan en caché. Si llega un `kid` desconocido, la vuelven a pedir.
  - **Origen de la llave privada:**
    - Si `mdd.usuarios.llave-privada` apunta a un archivo PEM, Usuarios la carga de ahí. Es el caso del VPS en B00-H08, con un secreto del entorno.
    - Si no, Usuarios genera una llave efímera al arrancar y lo avisa en el registro. Es el caso de local, la CI y las pruebas.
- **Por qué:**
  - La llave privada nunca entra al repositorio ni al `compose.yaml` (principio IX, FR-002), y el «un solo comando» de B00-H02 sigue funcionando sin generar archivos antes.
  - Gitleaks rechaza los `.pem` fuera de la carpeta de la CA de prueba, así que las llaves de las pruebas también se generan en memoria.
  - JWKS es el mecanismo estándar del servidor de recursos de Spring Security.
- **Consecuencia aceptada:** con la llave efímera, reiniciar el proceso identidad invalida los tokens emitidos. En local es aceptable, porque basta con volver a iniciar sesión, y en el VPS la llave es persistente.
- **Alternativas consideradas:**
  - un par de llaves en archivos, generado por un contenedor de inicio y repartido por volúmenes: más piezas en el compose;
  - la llave pública en la configuración de cada proceso: hay que repartirla y rotarla a mano.

## 3. Doble validación: API Gateway y cada proceso

- **Decisión:**
  - El API Gateway valida el token de toda petición a `/api/**` antes de reenviarla, y reenvía el encabezado `Authorization` sin cambios.
  - Cada proceso vuelve a validar firma, algoritmo, emisor y vencimiento (FR-003, FR-004).
  - Los dos usan el servidor de recursos de Spring Security: la versión reactiva en el API Gateway y la servlet en los procesos.
- **Validaciones:**
  - algoritmo **solo RS256**, así que un token con `alg: none`, HS256 u otro se rechaza;
  - firma con la llave del JWKS;
  - `iss` igual a `mdd-usuarios`;
  - `exp`, con la tolerancia de 60 s por defecto de Spring Security;
  - los roles salen del *claim* `roles` y se convierten en autoridades `ROLE_<rol>`.
- **Rutas sin token:**
  - API Gateway: `POST /api/usuarios/sesion` (inicio de sesión, D-37) y `/actuator/health/**`;
  - proceso identidad: además, `GET /api/usuarios/jwks`, que piden los demás procesos sin token;
  - demás procesos: solo `/actuator/health/**`.
- **Por qué:** el caso límite «petición que llega directo a un proceso» exige que el proceso no confíe en el API Gateway. Validar con la llave pública en caché no cuesta una llamada de red por petición.

## 4. El 401: siempre igual y sin pistas

- **Decisión:**
  - Toda respuesta 401 por un token ausente o inválido lleva estado 401, `Content-Type: application/json` y `WWW-Authenticate: Bearer`, sin `error` ni `error_description`. El cuerpo es:

    ```json
    {"titulo": "Sesión no válida", "descripcion": "Inicia sesión de nuevo para continuar.", "codigo": "SEGURIDAD_NO_AUTENTICADO"}
    ```

  - Lo escribe un punto de entrada de autenticación propio: uno servlet, compartido por los 4 procesos (§6), y uno reactivo en el API Gateway.
- **Por qué:**
  - Por defecto, Spring Security responde con `WWW-Authenticate: Bearer error="invalid_token", error_description="Jwt expired at …"`, que dice qué falló. Eso va contra el escenario «Token alterado o vencido» y SC-002.
  - El formato del cuerpo es el del principio XIII (D-38).
- **Registro:** la causa del rechazo se registra en nivel DEBUG, sin el token, para poder diagnosticar sin exponerla (FR-009).

## 5. Catálogo de códigos inicial (D-38)

| Código | Estado | Dónde | Cuándo |
|---|---|---|---|
| `SEGURIDAD_NO_AUTENTICADO` | 401 | API Gateway y los 4 procesos | Sin token, o con un token alterado, vencido, mal formado, firmado con otra llave o con otro algoritmo |
| `USUARIOS_CREDENCIALES_INVALIDAS` | 401 | Usuarios, inicio de sesión | Usuario inexistente, inactivo, contraseña incorrecta o datos vacíos. La respuesta es la misma en todos los casos. |

- El prefijo `SEGURIDAD_` es transversal: la autenticación no es de un solo servicio. Se registra en el catálogo como código común, igual que el código genérico de error técnico que creará B00-H04.
- El catálogo vive en `contratos/comun/errores.md`, junto al esquema `Error` (`contratos/comun/errores.yaml`). B00-H04 lo completa y no renombra estos códigos (D-38).
- **Datos vacíos en el inicio de sesión:** responden como credenciales inválidas (401), no como error de entrada (400). Los errores de entrada con `detalles` son de B00-H04, que activará la validación de los contratos.

## 6. Dónde vive la seguridad de los procesos

- **Decisión:** un módulo nuevo, `backend/plataforma/seguridad`, con:
  - la configuración servlet del servidor de recursos, como autoconfiguración de Spring Boot;
  - el decodificador RS256 con JWKS y validación del emisor;
  - el punto de entrada 401 de §4;
  - la lista de rutas sin token, que cada proceso amplía con una propiedad (`mdd.seguridad.rutas-publicas`).

  `mdd.aplicacion` no lo agrega solo: lo agrega el `build.gradle` de cada proceso, y la plantilla de proceso lo trae.
- **El API Gateway** tiene su propia configuración reactiva en `backend/gateway`, porque WebFlux no comparte las clases de la versión servlet.
- **Por qué:** son 4 procesos con la misma configuración (unas 80 líneas). Copiarla 4 veces haría que se desalineen, y esa es la razón que el principio XI acepta para una pieza compartida. No es una abstracción especulativa: hay 4 usos desde el primer día.
- **Alternativas consideradas:** la configuración dentro de la plantilla de proceso, copiada 4 veces; una sola configuración para servlet y reactivo, que no es posible sin duplicar igual.

## 7. Inicio de sesión mínimo en Usuarios (D-37)

Usuarios es un servicio de un subdominio genérico (constitución §4), así que su DDD es liviano (principio II). Se organiza por capas así:

| Capa | Pieza |
|---|---|
| Dominio | `Usuario`: nombre de usuario, hash de la contraseña, activo y roles, con la regla de que solo un usuario activo puede iniciar sesión. Java puro. |
| Puertos de entrada | `IniciarSesion` (caso de uso) |
| Aplicación | `IniciarSesionServicio`. Si el usuario no existe, calcula igual un hash, para que el tiempo de respuesta no revele qué usuarios existen. Si el usuario no existe, está inactivo o la contraseña no coincide, lanza `CredencialesInvalidas`. |
| Puertos de salida | `RepositorioUsuarios`, `VerificadorDeClaves` y `EmisorDeTokens`. El reloj es un `java.time.Clock` inyectado. |
| Adaptadores | `SesionControlador`, que implementa la interfaz generada del contrato; `RepositorioUsuariosJpa`; `VerificadorArgon2`; `EmisorJwt` y `JwksControlador` |
| Infraestructura | `LlavesDeFirma` (§2), `Argon2PasswordEncoder` con los valores recomendados de Spring Security, `UsuariosFlywayConfig` y la carga del usuario de prueba (§8) |

- **Persistencia:** Spring Data JPA en el adaptador, como fija la constitución (sección 3); es el primer servicio con entidades.
  - `V2__usuarios.sql` crea `usuarios.usuario` (`id` UUID, `nombre_usuario` único, `clave_hash` y `activo`) y `usuarios.usuario_rol` (`usuario_id` y `rol`).
  - Hibernate en modo `validate`: Flyway es el único que cambia el esquema.
- **Roles:** por ahora viven en `usuarios.usuario_rol`. F05-H02 (Roles) decidirá de dónde salen; esta historia solo los transporta en el token (spec, Assumptions). El principio V se cumple, porque ningún otro servicio lee esa tabla.
- **Argon2id:** `Argon2PasswordEncoder` necesita BouncyCastle (`org.bouncycastle:bcprov-jdk18on:1.86`), que no está en el BOM de Spring Boot. La versión se fija en `gradle.properties`.

## 8. El usuario de prueba

- **Decisión:**
  - Se crea al arrancar el proceso identidad, solo si `mdd.usuarios.usuario-prueba.habilitado=true`. Lo hace un `ApplicationRunner` de la infraestructura de Usuarios, que lo inserta si no existe.
  - La contraseña llega en `MDD_USUARIO_PRUEBA_CLAVE` y se guarda con Argon2id.
  - **Datos:**
    - nombre `autor.prueba`;
    - roles `DOCENTE` y `COORDINADOR_CARRERA`, los del escenario «Varios roles» de F05-H02;
    - contraseña ficticia `cambiar-en-local-usuario-prueba` en `.env.ejemplo`.
- **Por qué:**
  - Una migración de Flyway con el usuario lo crearía también en el VPS, con una contraseña conocida.
  - Una ubicación de migraciones condicional rompe la validación de Flyway si después se desactiva.
  - Así, el hash nunca está en el repositorio y el usuario solo existe donde se habilita.
- **En las pruebas:** las pruebas de integración crean su propio usuario con una contraseña generada en la prueba.

## 9. «Sesión actual» en cada proceso (D-39)

| Proceso | Servicio | Endpoint |
|---|---|---|
| identidad | `usuarios` | `GET /api/usuarios/sesion` |
| datos | `cargas` | `GET /api/cargas/sesion` |
| distributivo | `generacion` | `GET /api/generacion/sesion` |
| salida | `dashboard` | `GET /api/dashboard/sesion` |

- Responden `200 {"usuario": "autor.prueba", "roles": ["COORDINADOR_CARRERA", "DOCENTE"]}`, con los roles ordenados.
- El controlador vive en los adaptadores de entrada de su servicio y lee el JWT ya validado del `SecurityContext`. No hay caso de uso, porque no hay lógica.
- Se eligen `cargas` y `dashboard` (soporte) y `generacion`, porque distributivo no tiene servicios de soporte. Agregar un controlador en el adaptador no toca el dominio de ninguno.
- **Por qué:** recorre el API Gateway y los 4 procesos con el mismo contrato, y cada historia de esos servicios puede reutilizar o retirar el endpoint.

## 10. Rutas del API Gateway

`spring.cloud.gateway.server.webflux.routes` en el `application.yaml` del API Gateway, con un predicado `Path` por proceso:

| Rutas | Destino |
|---|---|
| `/api/usuarios/**`, `/api/roles/**`, `/api/notificaciones/**` | `http://identidad:8080` |
| `/api/cargas/**`, `/api/docentes/**`, `/api/academico/**`, `/api/normativa/**` | `http://datos:8080` |
| `/api/generacion/**`, `/api/validacionocs/**`, `/api/firma/**` | `http://distributivo:8080` |
| `/api/reporting/**`, `/api/dashboard/**` | `http://salida:8080` |

- Los destinos se pueden cambiar con variables, para B00-H08 y para las pruebas.
- Sin filtros de reescritura: los procesos reciben la misma ruta `/api/<servicio>/…`.

## 11. Contratos OpenAPI y generación (principio IV)

- **Organización:**

  ```text
  contratos/
  ├── comun/
  │   ├── errores.yaml      # esquema Error y DetalleError (principio XIII)
  │   ├── errores.md        # catálogo de códigos (§5)
  │   └── seguridad.yaml    # SesionActual, respuesta NoAutenticado y esquema bearer
  ├── usuarios/openapi.yaml
  ├── cargas/openapi.yaml
  ├── generacion/openapi.yaml
  └── dashboard/openapi.yaml
  ```

  Todos los archivos en OpenAPI 3.0.3. Los contratos de servicio referencian `../comun/*.yaml`.
- **Generación:** un plugin de convención nuevo, `mdd.contrato`, que aplican los servicios con contrato:
  - usa el plugin `org.openapi.generator` **7.25.0** (la 7.26.0 tiene una semana) con el generador `spring` y `interfaceOnly`, `useSpringBoot4`, `useJackson3`, `skipDefaultInterface`, `useTags`, `openApiNullable=false`, `useBeanValidation=false`, `documentationProvider=none` y `annotationLibrary=none`;
  - las interfaces y los modelos van a `ec.mdd.<servicio>.adaptadores.entrada.api`, generados en `build/generated/openapi`;
  - Spotless, SpotBugs (`config/spotbugs/excluir.xml` ya excluye `build/generated`) y JaCoCo no analizan el código generado.
- **Pruebas de contrato:** con `com.atlassian.oai:openapi-request-validator-core` **3.0.0**, antes llamado swagger-request-validator.
  - Su módulo para MockMvc usa `javax.servlet` y no funciona con Spring 7, así que se usa solo el núcleo, con un adaptador de unas 30 líneas que convierte el `MvcResult` en `SimpleRequest` y `SimpleResponse`.
  - Vive en `plataforma/seguridad` (fixtures de prueba, `testFixtures`) para que lo usen los 4 servicios.
  - El núcleo usa Jackson 2, que convive con Jackson 3 porque los paquetes son distintos y solo está en las pruebas.
- **Contexto de construcción de las imágenes:** hoy es `backend/`, y `contratos/` está en la raíz. El contexto pasa a la raíz del repositorio, con un `.dockerignore` raíz que solo deja pasar `backend/` y `contratos/`, sin salidas de compilación. `java.Dockerfile` copia los dos y compila en `/src/backend`. Esto cambia un detalle de ADR-013 y se registra en ADR-015.
- **Alternativas consideradas:**
  - escribir los controladores sin generación, que va contra el principio IV;
  - un solo contrato para todo el sistema, que va contra un contrato por módulo;
  - `contratos/` dentro de `backend/`, que la SPA de B00-H06 también necesita leer.

## 12. Pruebas

| Nivel | Qué |
|---|---|
| Dominio | `Usuario`: un usuario inactivo no puede iniciar sesión. |
| Aplicación (Mockito en los puertos) | `IniciarSesionServicio`: credenciales correctas emiten un token; usuario inexistente, inactivo o contraseña incorrecta lanzan `CredencialesInvalidas`; con un usuario inexistente igual se calcula un hash. |
| Adaptadores | `VerificadorArgon2` y `EmisorJwt` (firma RS256, *claims*, `kid` y vencimiento con un `Clock` fijo), unitarias. `RepositorioUsuariosJpa`, de integración con PostgreSQL. |
| Contrato | Por servicio con contrato: 200 y 401 de «sesión actual»; 200 y 401 del inicio de sesión; 200 del JWKS. Con el validador de §11. |
| Seguridad de los procesos (`plataforma/seguridad`) | Con un par de llaves generado en la prueba y un JWKS servido por la prueba: sin token, token alterado, vencido, mal formado, de otra llave, `alg: none` o HS256 y emisor distinto dan 401 con el mismo cuerpo y `WWW-Authenticate: Bearer` sin detalles; un token válido llega con sus roles. |
| API Gateway | Las mismas causas con `WebTestClient`; las rutas públicas pasan sin token; un token válido se reenvía con el encabezado `Authorization`. |
| De punta a punta | `deploy/probar_autenticacion.sh` sobre el perfil levantado, con `curl` y el usuario de prueba: los escenarios 1 a 3 (§13). Un token vencido de punta a punta no se prueba, porque habría que esperar la vigencia; lo cubren las pruebas de seguridad con un reloj fijo. |

## 13. Escenarios de punta a punta y R-19

- **`deploy/probar_autenticacion.sh`** comprueba, contra el perfil levantado y a través del API Gateway (`localhost:8080`):
  1. `GET /api/cargas/sesion` sin token responde 401 con el cuerpo de §4;
  2. el inicio de sesión del usuario de prueba devuelve un token;
  3. la «sesión actual» de los 4 procesos responde 200 con `autor.prueba` y sus 2 roles;
  4. el token con un carácter cambiado en la firma responde 401 con el mismo cuerpo que el paso 1;
  5. un proceso llamado directo, sin token, también responde 401 (`docker compose exec`);
  6. una contraseña incorrecta responde 401 con `USUARIOS_CREDENCIALES_INVALIDAS`.

  Termina con 0 si todo pasa. Lo usan la validación local y `perfil-consolidado.yml`.
- **`deploy/medir_memoria.sh --con-sesion`:** antes del calentamiento inicia sesión con el usuario de prueba, y el calentamiento suma 20 llamadas a la «sesión actual» de cada proceso. La tabla dice «después de iniciar sesión» y el formato no cambia (D-34). `perfil-consolidado.yml` ejecuta los dos scripts.
- **Memoria:** identidad suma Hibernate y Spring Security, y los demás procesos y el API Gateway suman Spring Security. Se espera un aumento moderado sobre los 931,6 MiB de B00-H02. El umbral sigue en 8 GiB, con alerta en 7 GiB (D-36). Los límites de 512 MiB por proceso se revisan si identidad se acerca al suyo.

## 14. ADR aprobados (constitución §6.4)

- **ADR-014, «Autenticación con JWT RS256 emitido por Usuarios»:** §1 a §4 y §6. Formato del token; llave privada solo en Usuarios, efímera en local y desde un archivo en el VPS; JWKS; doble validación; 401 uniforme sin detalles; `plataforma/seguridad`. Afecta a todas las historias con endpoints, a F05-H01 y F05-H02 y a B00-H08 (llave del VPS).
- **ADR-015, «Contratos OpenAPI en `contratos/` y generación por servicio»:** §11. Organización de `contratos/` con `comun/`; plugin `mdd.contrato` con openapi-generator 7.25.0; pruebas de contrato con el núcleo de openapi-request-validator; contexto de construcción de las imágenes en la raíz del repositorio (modifica ADR-013 en ese punto). Afecta a todas las historias con API, a B00-H04 y a B00-H06.

## 15. Lo que queda fuera

- La pantalla de inicio de sesión, la redirección y los usuarios reales de la demo (F05-H01); la matriz de permisos y la autorización por rol (F05-H02, D-16).
- La renovación del token, el cierre de sesión y la revocación.
- El catálogo completo, el `@RestControllerAdvice` y los errores de entrada con `detalles` (B00-H04).
- La llave privada persistente del VPS y su secreto (B00-H08).
- HTTPS: lo pone Caddy en el VPS (B00-H08); en local, el perfil es HTTP.
