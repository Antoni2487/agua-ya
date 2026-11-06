# PA3: prueba con usuarios (protocolo, registro y SUS)

**Qué pide la rúbrica:**
- **Mínimo 2 personas reales.** Al menos 1 con el perfil del PA1: un vecino de una zona alta que depende de la cisterna.
- **Se recomienda una persona en contexto de accesibilidad.** Puede ser situacional, por ejemplo usar el sistema con una sola mano, solo con teclado o con TalkBack.
- **Por persona:** el formulario SUS y al menos una evidencia (foto, grabación o notas).

## Tareas

La **tarea principal** debe ser **la misma tarea del GOMS del PA1**. Cópiala tal cual de ese informe. Si es la de la cisterna, queda así:

> **T1 (web).** "Usted vive en P.J. Atusparias. Averigüe si esta semana pasa la cisterna por su zona, qué día y qué placa tiene el camión."
>
> **T2 (app).** "Entre a la app con el DNI 30000004 y la clave 1234. Diga cuántas paradas faltan antes de su casa y califique la última entrega."

**Respuesta correcta.** En la web está en **¿Pasa hoy la cisterna?** (día y placa) y en la app, en **Inicio** (paradas que faltan) y en el botón **Calificar**.

Prepara la ruta de Atusparias en el panel del admin antes de cada sesión ([guion-exposicion.md](guion-exposicion.md#lista-del-día)).

## Protocolo (15–25 min por persona)

| Fase | Qué hace el grupo | Qué se anota |
|---|---|---|
| 1. Bienvenida (2 min) | Leer el guion de abajo y pedir el consentimiento | Consentimiento (sí/no) |
| 2. Tarea principal (5–10 min) | Leer la tarea y **no ayudar**. Tomar el tiempo desde que termina de leer hasta que responde | Tiempo, errores, si completó |
| 3. Exploración libre (3–5 min) | "Mire lo que quiera, siga pensando en voz alta" | Qué toca, qué ignora, dónde se detiene |
| 4. Entrevista (5 min) | Las 3 preguntas de abajo | Frases textuales |
| 5. SUS (2 min) | Entregar el formulario de abajo | Las 10 respuestas |

**Guion de bienvenida.** Se lee tal cual:
> "Gracias por ayudarnos. Vamos a evaluar el sistema, no a usted: si algo no sale, es culpa del diseño. Le pedimos que piense en voz alta: diga lo que busca, lo que espera y lo que le confunde. No podremos ayudarle durante la tarea. ¿Nos permite tomar notas, una foto de sus manos y la pantalla (sin su cara) y usar los resultados sin su nombre en un trabajo de la universidad?"

**Preguntas de la entrevista:**
1. ¿Qué fue lo más difícil?
2. Si vio el prototipo de Figma del PA2: ¿qué notó distinto ahora?
3. ¿Confiaría en este sistema para saber cuándo llega su agua? ¿Por qué?

### Variantes de accesibilidad (elegir al menos una)

| Tipo | Cómo se hace | Qué verifica el grupo |
|---|---|---|
| Motora | T1 **solo con teclado** (Tab, Enter, Esc) o con una sola mano en el celular | Que todo se alcance sin mouse |
| Visual | Zoom al 200 % y brillo bajo, o T2 con **TalkBack** activado | Alt, etiquetas y contraste |
| Cognitiva | Persona que nunca vio el sistema y sin explicación previa | Cuántos pasos necesita y si entiende los errores |

## Hoja de registro (una por persona)

**Participante:** P__  ·  **Perfil:** __________  ·  **Edad aprox.:** __  ·  **Dispositivo:** __________  ·  **Variante:** __________  ·  **Fecha:** ___

| Tarea | ¿Completó? (sí/no/con ayuda) | Tiempo (s) | Errores (nº y cuáles) | Observaciones |
|---|---|---|---|---|
| T1 web | | | | |
| T2 app | | | | |

**Frases textuales:**
- "…"
- "…"

**Barreras de accesibilidad encontradas y qué se hizo:** __________

## Cuestionario SUS

Marque del 1 (totalmente en desacuerdo) al 5 (totalmente de acuerdo).

| # | Afirmación | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| 1 | Creo que me gustaría usar este sistema con frecuencia. | | | | | |
| 2 | Encontré el sistema innecesariamente complejo. | | | | | |
| 3 | Pensé que el sistema era fácil de usar. | | | | | |
| 4 | Creo que necesitaría ayuda de una persona técnica para usar este sistema. | | | | | |
| 5 | Las funciones del sistema estaban bien integradas. | | | | | |
| 6 | Pensé que había demasiadas inconsistencias en el sistema. | | | | | |
| 7 | Imagino que la mayoría de las personas aprendería a usar este sistema muy rápido. | | | | | |
| 8 | Encontré el sistema muy difícil de usar. | | | | | |
| 9 | Me sentí muy seguro(a) usando el sistema. | | | | | |
| 10 | Necesité aprender muchas cosas antes de poder usar el sistema. | | | | | |

**Cómo se calcula:**
1. En las preguntas **impares** (1, 3, 5, 7, 9) se resta 1 a la respuesta.
2. En las **pares** (2, 4, 6, 8, 10) se resta la respuesta a 5.
3. Se suman los 10 valores y la suma se multiplica por **2,5**.

El resultado va de 0 a 100. **≥ 70 es aceptable** y **≥ 85 es excelente**.

> Ejemplo: respuestas 4, 2, 5, 1, 4, 2, 5, 1, 4, 2. Impares: 3 + 4 + 3 + 4 + 3 = 17. Pares: 3 + 4 + 3 + 4 + 3 = 17. (17 + 17) × 2,5 = **85**.

## Tabla comparativa PA2 vs PA3 (informe, sección 6)

Los valores del PA2 salen de su informe (prueba con el prototipo de Figma).

| Métrica | PA2 (Figma) | PA3 (código) | Observación |
|---|---|---|---|
| Efectividad (%) = personas que completaron ÷ total | | | |
| Eficiencia = tiempo promedio de T1 (s) | | | |
| Satisfacción SUS (promedio) | | | |
| Errores detectados (total) | | | |

**Para la diapositiva 05:** una foto de la prueba, el SUS de cada persona y su promedio, y esta tabla.
