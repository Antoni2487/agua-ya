# PA3: guion de la exposición (12–15 min)

**Idea central:** el vecino de una zona alta no sabe si hoy pasa la cisterna. El sitio actual de EPSEL no se lo dice y tampoco es accesible. Lo resolvimos en dos partes: la web de EPSEL rediseñada (accesible, con la información de las cisternas) y la app AguaYa (vecino y conductor), que sale del Figma del PA2.

**Orden de la historia:** antes, con sus fallas → después, con la web rediseñada → la app AguaYa → el flujo completo en vivo → resultados.

Todo corre en local (ver la tabla de [README](../../README.md#puesta-en-marcha)). Antes de entrar a la sala, revisa la [lista del día](#lista-del-día).

---

## Diapositivas (máximo 10)

Solo palabras clave e imágenes. El texto va en lo que se dice, no en la diapositiva.

| N.º | Título | En pantalla | Quién | Tiempo |
|---|---|---|---|---|
| 01 | Portada | AguaYa · nombres del equipo · IHM · PA3 | Integrante 1 | 0:20 |
| 02 | Del Figma al código | Dos o tres pantallas del Figma del PA2 al lado de la app real (Inicio, Mapa, Calificar) | Integrante 1 | 1:00 |
| 03 | Accesibilidad implementada | Los 5 criterios principales, cada uno con su captura de código (ver [criterios-wcag.md](criterios-wcag.md)) | Integrante 2 | 1:30 |
| 04 | Reporte axe / WAVE | Antes: portada de EPSEL, 9 reglas violadas y 100 elementos. Después: 0 en las 7 páginas | Integrante 2 | 1:00 |
| 05 | Prueba con usuarios | Foto de la prueba · SUS de cada persona · tabla PA2 vs PA3 | Integrante 3 | 1:30 |
| 06 | Top 3 hallazgos | 3 barreras reales y cómo se resolvieron (ver abajo) | Integrante 3 | 1:00 |
| 07 | Demo en vivo | (se cambia al navegador y al celular) | Integrantes 2 y 4 | 6:00 |
| 08 | Ciclo DCU | Problema → PA1 → PA2 → PA3 en un diagrama | Integrante 4 | 0:45 |
| 09 | Reflexión final | Una frase | Integrante 4 | 0:20 |

Total ≈ 13:30. Quedan 1:30 para preguntas o imprevistos.

### Top 3 hallazgos (diapositiva 06)

Elegir tres que puedan mostrarse en vivo:

1. **Lo urgente no estaba en el menú.** En la prueba de árbol, "¿Pasa hoy la cisterna?" y "¿Cortan el agua hoy?" no tenían camino en el menú actual. En el rediseño, son el primer botón de la portada y el primer menú.
2. **El sitio no se podía usar con teclado ni con lector de pantalla.** Había imágenes sin `alt`, menús que solo se abrían con el mouse y la página no tenía idioma. Ahora hay "Ir al contenido principal", un menú que se abre con Enter o Espacio y se cierra con Esc, y foco amarillo visible.
3. **En el celular los botones eran muy chicos y la página pesaba 85 MB.** Antes, 34 de 54 controles medían menos de 44 px. Ahora ninguno: miden 48 px o más y la página pesa 0,8 MB, algo clave con datos móviles limitados.

(Alternativa para la app: el estado de la cisterna no se dice solo con color. "Faltan 3 paradas" y "Verificada por GPS" van en texto, y la gota de avance es decorativa.)

### Frase final (diapositiva 09)

> "Ahora el vecino de una zona alta sabe, desde su celular y sin ayuda, cuándo llega su cisterna."

---

## Demo en vivo (diapositiva 07, unos 6 min)

Dos pantallas: la **laptop** (navegador) y el **celular** proyectado (scrcpy, o la cámara de la laptop apuntando al celular).

### Parte A. Antes: el sitio actual (1 min)

1. Abrir `www.epsel.com.pe/sue/`. Presionar **Tab** varias veces: el foco no se ve y no hay "Ir al contenido".
2. Intentar abrir un menú con **Enter**: no abre.
3. Mostrar el reporte de axe del sitio actual: [linea-base/axe/](linea-base/axe/), con su captura.

> Si no hay internet, usar las capturas de [linea-base/axe/](linea-base/axe/).

### Parte B. Después: la web rediseñada, solo con teclado (2 min)

En `http://localhost:5500`, **sin tocar el mouse**:

1. **Tab** → aparece "Ir al contenido principal" (criterio 9). **Enter** → el foco salta al contenido.
2. Volver arriba. Con **Tab** llegar al menú; **Enter** lo abre; **Tab** recorre las opciones; **Esc** lo cierra y el foco vuelve al botón (criterios 3 y 4).
3. En el buscador escribir `xx` y **Enter** → error en texto, no solo en rojo (criterio 7). Escribir `atusparias` → aparece el resultado y se anuncia.
4. Ir a **¿Pasa hoy la cisterna?**: se ve la semana con la placa de la cisterna. Si el simulador ya corre, aparece "Repartiendo ahora".
5. Abrir axe DevTools en esa página → **0 problemas** (deliverable 4).
6. Opcional: zoom al 200 % → no aparece barra horizontal.

### Parte C. AguaYa: el flujo completo (3 min)

Antes de exponer, deja la ruta de hoy programada (ver la [lista del día](#lista-del-día)).

1. **Admin (laptop):** en `http://localhost:5173` entrar con `10000001` / `Admin2026!`. Mostrar la **Planificación de capacidad** ("¿alcanzan las cisternas?") y luego **Rutas**, con la ruta de hoy de P.J. Atusparias y su cisterna y conductor.
2. **Vecino (celular):** entrar con `30000004` / `1234`. Inicio dice "Te toca hoy" o "Faltan N paradas" y muestra la placa.
3. **Arrancar el simulador** en una terminal: `node scripts/simular-ruta.mjs --api http://localhost:8080`.
4. Mostrar a la vez:
   - el **Mapa en vivo** del admin, con el camión moviéndose;
   - la pestaña **Mapa** del vecino;
   - en la web rediseñada, "Repartiendo ahora".
5. Cuando el camión llega a la casa del vecino, la parada queda **Atendida · Verificada por GPS**. El vecino toca **Calificar** y da 5 estrellas.
6. El vecino **reporta** "Llegó tarde" → en el panel del admin aparece la alerta en **Incidencias** al instante.
7. **Accesibilidad en la app (30 s):** activar **TalkBack** y deslizar por el Inicio. Lee "Faltan 3 paradas antes de la tuya" y "Botón Calificar". Las estrellas se leen como "4 de 5: Buena".

---

## Preguntas probables y quién responde

Según la rúbrica, la docente puede pedir que se explique cualquier línea o que se modifique algo en vivo. Practicar estos cambios (cada uno toma menos de un minuto):

| Pregunta o pedido | Dónde se hace |
|---|---|
| "Cambia el color del foco" | `rediseno-epsel/estilos.css`, variable `--foco` |
| "Agrega un `aria-label` a este botón" | El `<button>` en el `.html` correspondiente |
| "Muestra que el error no es solo color" | `sitio.js`, función del buscador: escribe el texto en el `<p class="campo-error">` y pone `aria-invalid` |
| "¿Por qué 48 px y no 44?" | 44 es el mínimo de WCAG 2.5.5 y Android recomienda 48 dp. Usamos 48 en la web y 56 en los botones de la app (`mobile/src/tema.ts`, `medida.toque`) |
| "¿Cómo sabe la app que el camión llegó?" | Geocerca: la última posición del conductor debe estar a 50 m o menos de la parada y tener 2 minutos o menos de antigüedad (`backend/.../monitoreo`) |
| "¿Por qué una clave de 4 dígitos?" | Es como Yape o Plin, simple para el vecino, y se bloquea 15 min tras 5 intentos fallidos. El SMS cuesta, así que solo se usa para registrarse |

La explicación de cada parte del código está en [guia-del-codigo.md](guia-del-codigo.md).

---

## Lista del día

**La noche anterior**
- [ ] `docker compose up --build`, para que el backend tenga la última versión.
- [ ] Capturas de axe y WAVE guardadas (por si falla internet).
- [ ] Video demo de 3 min grabado (deliverable 3): es el mismo recorrido de las partes B y C.

**30 min antes**
- [ ] Laptop y celular en la **misma red wifi**; el wifi de la universidad puede bloquear la conexión entre equipos. Si pasa, usa el hotspot del celular.
- [ ] Levantar 1–4 de la tabla de puesta en marcha.
- [ ] En el panel del admin, programar la **ruta de hoy** para P.J. Atusparias y asignarle una cisterna y el conductor `20000001`.
- [ ] En la app, dejar abierta la sesión del vecino `30000004`.
- [ ] Ensayar el simulador con **otro sector y otro conductor** (`--dni 20000002`): una ruta completada ya no se puede borrar ni repetir el mismo día. Si una ruta queda EN_CURSO tras un Ctrl+C, el simulador la retoma.
- [ ] Brillo al máximo y zoom del navegador al 125 % para que se lea desde atrás.
