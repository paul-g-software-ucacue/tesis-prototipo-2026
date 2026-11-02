# Research: Consolidar los parciales con el informe sustentado

## 1. Dónde se guardan los archivos (FR-016, RNF-POR-01)

- **Decision:** un volumen con nombre, `archivos`, montado solo en `distributivo`, en `/var/mdd/archivos`.
- **Rationale:** cumple RNF-POR-01 y sobrevive a las actualizaciones. Un volumen con nombre no toca carpetas del usuario y no necesita etiquetas de SELinux.
- **Dueño:** el proceso corre como 10001, y un volumen nuevo copia dueño y permisos de la imagen: `java.Dockerfile` crea la carpeta. Vale para todas las imágenes Java; la usa solo `distributivo`.
- **Alternatives:** en la base, contradice RNF-POR-01. Un servicio de objetos, como MinIO, agrega un contenedor a un equipo de 4 GB.

## 2. El puerto

- **Decision:** `AlmacenDeArchivos { String guardar(byte[]); byte[] leer(String clave); }`, con `AlmacenEnDisco`.
- **La clave:** un UUID que genera el almacén. El nombre recibido no se usa para la ruta (FR-017).
- **Guardar:** archivo temporal en la misma carpeta y movimiento atómico: un archivo a medias nunca tiene clave.
- **Reutilizar:** cuando llegue Firma se decide si el puerto sube a la plataforma. Hacerlo ahora sería diseñar para un uso que no existe.

## 3. El orden al adjuntar (FR-022)

- **Decision:** primero el archivo, fuera de la transacción, y después la fila y el registro, en una transacción.
- **Rationale:** si falla el archivo, no hay fila. Si falla la fila, queda un archivo huérfano, sin efecto para nadie.

## 4. Validar el PDF (FR-015)

- **Decision:** `ArchivoPdf.de(nombre, bytes)` exige no estar vacío, hasta 10 MB, que empiece por `%PDF-` y tenga `%%EOF` en sus últimos 1024 bytes.
- **Rationale:** comprueba el contenido sin una biblioteca nueva. Abrirlo de verdad es «leer el contenido del PDF», fuera de alcance.
- **Nombre mostrado:** sin ruta, solo caracteres imprimibles, hasta 150. `Content-Disposition` lleva un nombre fijo: `informe-sustentado-<periodo>.pdf`.

## 5. Los límites

| Dónde | Hoy | Queda |
|---|---|---|
| `frontend/nginx.conf` | 10 MB | 11 MB |
| `apps/distributivo`, multipart | 1 MB, por omisión | 11 MB |
| Dominio | — | 10 MB |
| SPA | — | 10 MB, antes de subir |

Entre 10 y 11 MB responde el código de la historia. Por encima, `COMUN_CONTENIDO_DEMASIADO_GRANDE`.

## 6. Desactualizado (FR-012, FR-013)

- **Decision:** `max(momento)` de `registro_cambio` con `PROPUESTA_GUARDADA`, `PROPUESTA_QUITADA`, `ACTIVIDAD_GUARDADA` y `ACTIVIDAD_QUITADA` de ese periodo, comparado con la última consolidación.
- **Rationale:** el registro también ve lo quitado, que ya no tiene fila. Esas cuatro acciones llevan `periodo` en su detalle. Justificar y adjuntar no están en la lista.

## 7. El consolidado (FR-007 a FR-011)

- **Decision:** `ConsolidadoDelPeriodo.armar(...)`, dominio puro, con `avance` de la versión y `CargaHoraria` por carrera. Las inconsistencias vienen de `ConsultarPanel`, sin volver a evaluar.
- **Horas por carrera:** clase de las propuestas de esa carrera y actividades de ese parcial, sin contar retirados.
- **La carrera:** Generación solo tiene el código. La SPA pone el nombre, como en el panel.

## 8. Permisos

Los de D-87: leer, `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`; escribir, `CONSOLIDACION:ESCRITURA`.

## 9. Descargar

- **Decision:** bytes con `application/pdf`, después de comprobar la huella. El archivo pasa una vez por memoria: son 10 MB como máximo y el proceso tiene 512 MB.
- **SPA:** con el token, como las plantillas: `blob` y un enlace temporal.

## 10. Las pruebas del despliegue

`probar_equipo.sh` levanta la demo con `compose.demo.yml`: comprobará que el proceso puede escribir en el volumen, porque ahí se ve un error de permisos que ninguna prueba unitaria ve.
