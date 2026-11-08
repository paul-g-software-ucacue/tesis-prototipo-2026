# Contrato: el estado del flujo en la SPA

Un componente común `EstadoDelFlujo` (`frontend/src/pantallas/comun/EstadoDelFlujo.tsx`) que consulta `consultarFlujo` y muestra:
- la etapa actual con su nombre, y la versión;
- cada parcial: «Enviado por … el …», «Pendiente» o «Enviado, falta confirmar» (con «Reenviar» para su coordinador, research §2);
- un botón por cada acción de `acciones` (la API decide qué ve cada usuario).

| Pantalla | Quién | Qué ve |
|---|---|---|
| Oferta del periodo | Coordinador de carrera | Su parcial y «Enviar parcial». Antes de enviar, una confirmación: «Después de enviarlo, no podrás cambiar las propuestas ni las actividades de tu carrera.» Después, la oferta y las actividades de su carrera quedan en solo lectura |
| Consolidado del periodo | Responsable de consolidación, Vicerrector | La etapa y qué carreras enviaron |
| Proyectos | Gestor de unidad | «Avanzar» solo cuando el distributivo está en BORRADOR_UNIDADES |

Los errores se muestran con `AvisoDeError`, como en las demás pantallas.

**Pruebas:** Vitest del componente (pendiente y enviado, la confirmación, el botón solo con la acción, el 409 de ya enviado) y de cada pantalla; Playwright `e2e/flujo-envio.spec.ts`: el coordinador envía, la oferta queda en solo lectura, y axe-core sin fallas.
