# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Vecino (usuario principal del curso IHM, PA3):** persona adulta de 35 a 60 años que vive en una zona alta o asentamiento humano (AA.HH.) de Chiclayo o Monsefú, donde el agua no llega por red y depende de cisternas de EPSEL. Usa un celular de gama media o baja con datos móviles limitados y tiene poca experiencia digital. Su tarea es saber **cuándo llega el agua a su casa** para estar presente con baldes o tanque, y seguir la cisterna mientras se acerca.
- **Administrador de EPSEL:** programa rutas, asigna cisternas y conductores, monitorea en vivo y atiende incidencias desde el panel web (ya implementado).
- **Público:** cualquier ciudadano que consulta el panel de transparencia sin iniciar sesión.
- **Conductor:** usa la app móvil (fuera del alcance de este diseño web).

## Product Purpose

AguaYa organiza el abastecimiento de agua potable con camiones cisterna para EPSEL S.A.: planifica rutas por capacidad, verifica cada entrega con el GPS del conductor y le informa al vecino cuándo llega su atención. Para el vecino, el éxito es enterarse a tiempo y no perder la entrega. Para EPSEL, el éxito es cumplir las rutas y poder demostrarlo.

## Positioning

Cada entrega se verifica por geocerca: una parada solo cuenta como atendida si el GPS del conductor estuvo a 50 m o menos de la casa. Por eso lo que el vecino ve (y lo que muestra el panel público) es evidencia y no una promesa, frente a las denuncias de cisternas desviadas.

## Operating Context

- El vecino consulta desde el celular, a menudo al aire libre o en casa con poca señal, y muchas veces mientras atiende otras tareas.
- Las direcciones en asentamientos son informales ("Mz. B Lt. 12, AA.HH. X"); lo que vale es el pin en el mapa.
- Las rutas se programan por sector y por turno (mañana o tarde). La cisterna sale de la planta, carga agua y reparte por paradas ordenadas, y primero atiende las instituciones críticas.
- Es un proyecto académico (UTP Chiclayo): curso IHM, entrega PA3 (implementación accesible y validación con usuarios). La demo se hace navegando con el teclado (Tab/Enter) y se audita con WAVE o axe.

## Capabilities and Constraints

- Para el vecino: próxima atención (ruta en curso, o la siguiente y la última, con su propia parada), posición de la cisterna en vivo mientras la ruta está EN_CURSO, reporte de incidencias (NO_LLEGO, LLEGO_TARDE, YA_PASO, VOLUMEN_INSUFICIENTE, OTRO), calificación de 1 a 5 estrellas de una parada atendida, y registro con OTP por SMS.
- Panel público: indicadores agregados (cumplimiento, litros distribuidos), estado por sector y cronograma. Nunca muestra nombres, DNI, teléfonos ni coordenadas de viviendas.
- Stack: React + Vite (`web/`), Leaflet con OpenStreetMap y backend REST en Spring Boot. El panel del Admin y `/transparencia` ya existen.
- Sin pagos ni facturación, y sin SMS para avisos (solo para el OTP).
- Terminología: sector, parada, ruta, turno (mañana/tarde), cisterna (con placa), punto de entrega, incidencia.

## Brand Commitments

- **AguaYa** es la marca principal. Hay una propuesta de logo en `docs/marca/propuesta-logo-aguaya.png`: una gota azul en degradado (de cian a azul) con una señal de conexión y un camión cisterna en blanco, y el texto "Agua Ya!" en azul marino, en sans redondeada y pesada.
- **EPSEL S.A.** aparece como respaldo discreto ("Servicio de EPSEL S.A.") con su logo en tamaño pequeño, en el pie o en el inicio de sesión. Referencia en `docs/marca/referencia-epsel.png`.
- Los datos (sectores, placas, cisternas) son referenciales, no oficiales de EPSEL.

## Evidence on Hand

- El backend está implementado y tiene datos semilla referenciales (sectores reales de Chiclayo y Monsefú, placas ficticias).
- No hay testimonios, métricas reales de EPSEL ni resultados SUS todavía; no se deben inventar.
- El prototipo previo del PA2 está en Figma Sites (https://garb-squat-69121133.figma.site). El equipo lo considera de baja calidad: sirve solo para comparar PA2 contra PA3.

## Product Principles

1. **Primero la respuesta:** la pregunta del vecino es "¿cuándo llega?" y la respuesta va antes que cualquier otra cosa.
2. **Verificado, no prometido:** se muestra lo que el sistema puede demostrar (GPS, geocerca) y se distingue claramente lo programado de lo confirmado.
3. **Legible para quien no es experto:** lenguaje simple y local, pocas opciones por pantalla y textos que explican el estado sin depender del color.
4. **Liviano:** pantallas que cargan bien con datos móviles limitados en celulares modestos.

## Accessibility & Inclusion

- WCAG 2.1 AA como mínimo, demostrable en el código: contraste ≥ 4.5:1, navegación completa por teclado, foco visible, ARIA en íconos sin texto, objetivos táctiles ≥ 44×44 px, errores con texto (no solo color), etiquetas visibles con `autocomplete`, enlace "Ir al contenido principal", HTML semántico y cuerpo de texto ≥ 14 px (de preferencia 16 px).
- Usuarios con poca experiencia digital y, posiblemente, baja visión: debe funcionar con zoom al 200 % y con lector de pantalla (TalkBack).
