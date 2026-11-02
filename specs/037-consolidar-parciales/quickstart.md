# Quickstart: Consolidar los parciales con el informe sustentado

## Automático

```bash
deploy/probar_consolidado.sh
```

Con el perfil levantado y la base vacía, sobre los datos de prueba:

1. El consolidado coincide con una suma aparte, carrera por carrera (SC-001), y abre en menos de 5 s.
2. Consolidar deja quién y cuándo, y se repite.
3. Cambiar una propuesta lo deja desactualizado, y volver a consolidar lo arregla; justificar y adjuntar no.
4. Un PDF de exactamente 10 MB se adjunta en menos de 10 s, también por el puerto de la SPA, y se descarga idéntico.
5. Un texto, una imagen y un archivo vacío llamados `.pdf`, y uno de más de 10 MB, se rechazan.
6. Reemplazar conserva el anterior.
7. Un archivo cambiado en el disco responde el error del catálogo.
8. El informe sobrevive a reiniciar el proceso.
9. Permisos: un coordinador recibe 403; el Vicerrectorado lee y descarga.
10. Con `datos` detenido, el error del catálogo en menos de 10 s.

`deploy/equipo/probar_equipo.sh` comprueba además que en la demo se puede escribir en el volumen.

## A mano, en la demo

Entra con `consolidacion.demo` y abre **Consolidado del periodo**, periodo 003.

1. Pulsa **Consolidar**: ve SOCIALES, la única carrera con oferta, con su estado, y las inconsistencias abiertas.
2. Adjunta un PDF y descárgalo.
3. Con `coordinacion.demo`, cambia una propuesta. Con `consolidacion.demo`, mira el aviso de desactualizado.
4. Con `vicerrectorado.demo`, descarga el informe, sin poder consolidar.
