# Research: Cargar la configuración del flujo y del árbol de firmas

## 1. El archivo (FR-007, FR-008)

- **Decision:** `flujo-por-defecto.yaml`, un recurso del servicio, es a la vez la plantilla y la configuración por defecto.
- **Rationale:** un solo archivo no se puede desincronizar. Una prueba carga la plantilla por el mismo camino que un archivo subido.

```yaml
etapas:
  EN_REVISION_VRA:
    activa: true
    nombre: En revisión del Vicerrectorado
    rol: VICERRECTOR
    plazo_dias: 5
    admite_devolucion: true
firmas:
  - orden: 1
    etiqueta: Elaborado
    rol: RESPONSABLE_CONSOLIDACION
    etapa: CONGELADO_EN_FIRMA
    documento: CONSOLIDADO
```

## 2. Leer sin riesgo (FR-009 a FR-011)

- **Decision:** primero se recorren los eventos del lector: un ancla, un alias o una etiqueta explícita rechazan el archivo. Después se arma solo el árbol de nodos: ningún paso construye objetos, y cada valor es el texto escrito. Es más estricto que lo planeado, que construía con `SafeConstructor`.
- **Rationale:** rechazar los alias evita su explosión, y un archivo de configuración no los necesita. Sin construir objetos no hay tipos que interpretar. Con los nodos, una clave repetida se dice en la lista de problemas, en vez de cortar la lectura.
- **Límites:** 1 MB y 8 niveles de profundidad.
- **Texto:** UTF-8 estricto. Se quita la marca de orden, porque los editores de Windows la agregan.
- **Mensajes:** una línea del lector nunca llega al usuario: se traduce a «No se pudo leer el archivo», con «Revisa la línea N.» si se conoce.

## 3. Todos los problemas (FR-015)

- **Decision:** `ConfiguracionFlujo.desde(borrador)` devuelve la configuración o la lista de problemas. No lanza al primero.
- **El lector** arma un `Borrador` tolerante: lo que no entiende queda como problema. Después, los firmantes se agregan a la misma lista.
- **Contrato:** 400 `VALIDACIONOCS_CONFIGURACION_INVALIDA`, con un detalle por problema.
- **Límite de la lista:** hasta 50 problemas y «Hay N problemas más.». Un archivo de 1 MB puede tener miles, y con los primeros basta para corregir.
- **Firmantes:** se revisan sobre lo que dice el archivo, no sobre la configuración, para que salgan junto con los demás problemas.

## 4. Qué se guarda (FR-017, FR-018)

- **Decision:** el texto subido, tal cual, en una fila por carga, con quién y cuándo. La vigente es la última; su número es su posición.
- **Rationale:** el archivo es la fuente: conserva los comentarios y se puede volver a descargar. Tablas de etapas y firmas repetirían lo mismo.
- **Registro:** esa tabla es el registro. La bitácora general es F17.
- **Dos a la vez:** dos filas, y vale la última.

## 5. El estado del periodo

- **Decision:** ruta nueva `/interno/academico/periodos/{codigo}/estado`.
- **Alternatives:** `/interno/cargas/.../preparacion` dice abierto y cerrado, pero no Planificación.

## 6. Los firmantes (FR-013, FR-014)

- **Decision:** ruta nueva `/interno/roles/firmantes?periodo=`: los roles con al menos un usuario, y si hay Responsable designado.
- **Proceso:** Roles vive en `identidad`. `distributivo` agrega `mdd.procesos.identidad` y el mismo `ConsultasEntreProcesos`.
- **Roles válidos:** el dominio conoce los 10 de D-63, para decir «ese rol no existe» sin llamar a nadie.

## 7. Los valores por defecto

| Etapa | Activa | Rol | Días | Devolución |
|---|---|---|---|---|
| BORRADOR_CARRERA | sí | COORDINADOR_CARRERA | 15 | no |
| BORRADOR_UNIDADES | no | GESTOR_UNIDAD | 10 | no |
| EN_CONSOLIDACION | sí | RESPONSABLE_CONSOLIDACION | 5 | sí |
| EN_REVISION_VRA | sí | VICERRECTOR | 5 | sí |
| CONGELADO_EN_FIRMA | sí | RESPONSABLE_CONSOLIDACION | 5 | sí |
| EN_OCS | sí | SECRETARIA_GENERAL | 10 | sí |
| REMITIDO_ZONAL | no | VICERRECTOR | 10 | no |
| APROBADO | sí | SECRETARIA_GENERAL | 3 | no |
| PUBLICADO | sí | SECRETARIA_GENERAL | — | no |

Los roles de EN_OCS, APROBADO y PUBLICADO salen de la matriz D-16: Secretaría General escribe la resolución y aprueba la publicación. CONGELADO_EN_FIRMA parte con F1, del Responsable.

## 8. Permisos

Leer: `FLUJO_FIRMAS:LECTURA`. Cargar: `FLUJO_FIRMAS:ESCRITURA`. Hoy, solo el Administrador.

## 9. Lo nuevo en `validacionocs`

Es su primer contrato: agrega `mdd.contrato`, `ErroresValidacionocs`, `generar:api` en `frontend/package.json` y las pruebas de permisos y de catálogo, con el mismo patrón de Generación. El API Gateway ya enruta `/api/validacionocs/**`.
