# Quickstart: Cargar la configuración del flujo y del árbol de firmas

## Automático

```bash
deploy/probar_flujo.sh
```

Con el perfil levantado y la base vacía, sobre los datos de prueba:

1. Sin archivo rige la configuración por defecto.
2. La plantilla se descarga.
3. Sin Responsable designado se rechaza.
4. Con el Responsable designado, la plantilla se carga sin cambios (SC-001).
5. Otra carga crea una versión nueva.
6. Un archivo sin EN_OCS, una firma en una etapa inactiva y un rol sin usuario se rechazan, diciendo cada problema.
7. Un archivo con varios problemas los muestra todos (SC-002).
8. Anclas, alias, etiquetas, claves repetidas, vacío y más de 1 MB se rechazan.
9. Ningún rechazo cambia lo vigente.
10. Un periodo que no existe responde 404.
11. Permisos: sin FLUJO_FIRMAS, 403; sin token, 401.
12. Con `datos` o con `identidad` detenidos, el error del catálogo en menos de 10 s, sin guardar.
13. Con los procesos de vuelta, se carga.
14. Fuera de Planificación se ve y no se carga.
15. Cada carga queda con su texto, quién y cuándo.
16. El contenido de los archivos no va a los registros.

## A mano, en la demo

Entra con `admin.demo` y abre **Flujo y firmas**, periodo 003.

1. Ve que rige la configuración por defecto.
2. Descarga la plantilla, cambia un plazo y cárgala.
3. Quita `EN_OCS` del archivo y cárgalo: dice qué falta, y lo vigente no cambia.
