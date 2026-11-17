# Contrato: la campana de la SPA

F16-H01 (#67). Lo que ve y hace el usuario (US2, FR-006 a FR-010).

## La campana (`navegacion/Campana.tsx`, en el `Toolbar` de `Marco.tsx`)

- Es un `IconButton` con un `Badge` del número sin leer. No muestra el número si es 0.
- Su `aria-label` es «Notificaciones, N sin leer», o «Notificaciones» si no hay ninguna sin leer.
- Consulta `GET …/bandeja/sin-leer`:
  - al montar;
  - cada 30 s (SC-004);
  - cuando la ventana vuelve a tener el foco.
- Deja de consultar al cerrar la sesión.
- Un 401 lo maneja el cliente común (D-50). Cualquier otro error no muestra nada: la campana sigue con el último número.

## La lista (un `Popover` anclado a la campana)

- Al abrirse, consulta `GET …/bandeja` (50).
- Título: «Notificaciones».
- **Cada notificación** es un elemento de lista que se activa con el clic o con Enter. Muestra:
  - el título, en negrita y con un punto si no está leída;
  - el detalle;
  - «{autorNombre} · {dd/mm/aa hh:mm}» y el periodo.
- **Abrir una notificación:**
  1. hace `POST …/{id}/lectura`;
  2. cierra la lista;
  3. navega a `enlace` con `?periodo=` de la notificación. «Oferta del periodo», «Consolidado del periodo» y «Resolución del OCS» abren ese periodo si existe; si no, el de por omisión (`pantallas/periodos/periodoElegido.ts`). Elegir otro periodo en el selector lo deja en la dirección;
  4. baja el número.

  Si la lectura falla, navega igual: el número se corrige en la siguiente consulta.
- **Sin notificaciones:** «No tienes notificaciones».
- **Error al cargar:** «No se pudieron cargar las notificaciones.» y el botón «Reintentar».
- **Teclado:**
  - Escape cierra la lista y devuelve el foco a la campana;
  - las flechas recorren la lista.

## Pruebas

- **Vitest (`Campana.test.tsx`):**
  - el número y su `aria-label`;
  - la consulta a los 30 s con temporizadores falsos;
  - la lista ordenada, con las leídas y las no leídas;
  - abrir: marca, navega con el periodo y baja el número;
  - la lista vacía, el error y el reintento.
- **Vitest (`periodoElegido.test.tsx` y `Consolidado.test.tsx`):** el periodo de la dirección, el de por omisión si no existe, y elegir otro lo deja en la dirección.
- **Vitest (`enlaces.test.ts`):** para cada etapa de la configuración por defecto,
  - su enlace de [eventos.md](eventos.md) es una ruta de `rutas.tsx`;
  - el rol de la etapa tiene el permiso que pide la ruta, según `sesion/funcionalidades.ts` y la matriz D-16 con Roles V9;
  - con COORDINADOR_CARRERA, también el enlace de la devolución.
- **Playwright (`e2e/notificaciones.spec.ts`):**
  - con la API simulada, el Vicerrector ve 1 sin leer;
  - abre la notificación con el teclado y llega a «Consolidado del periodo»;
  - el número desaparece;
  - axe-core (WCAG 2.1 AA) sin fallas con la lista cerrada, abierta y vacía (SC-006).
