# Surface brief: AguaYa web (vecino + login + transparencia)

Mode: Operate (vecino, login); Read (transparencia). Mobile-first web (390 px), transparencia también 1440 px.
Audiencia: vecino adulto 35–60 de AA.HH. (Cerropón, Chiclayo), gama baja, poca experiencia digital.
Tarea principal (GOMS / prueba PA3): saber cuándo llega el agua y seguir la cisterna.
Restricciones: WCAG 2.1 AA demostrable (11 criterios del PA3), sin ETA inventada (el backend da pendientesAntes, paradasAtendidas/totalParadas, orden), sin datos personales en transparencia.

## Direction contract

THESIS: Una app de barrio amable y precisa: una idea por tarjeta, y el único color saturado es lo que se puede tocar. Rechaza el «delivery con mapa a pantalla completa» y el dashboard gris.

OWN-WORLD: Fondo arena clara tintado, tarjetas blancas de radio amplio (20 px), azul AguaYa #0A5BB5 solo para acciones y selección, marino #0E2A5C para títulos, degradado cian→azul del logo reservado a la gota de progreso. Nunito (títulos, eco del logotipo redondeado) + Atkinson Hyperlegible (cuerpo, diseñada para baja visión). Estados por forma + texto + color.

STORY: El vecino abre, lee en una línea si hoy le toca y cuánto falta, ve la cisterna en el mapa si quiere, y reporta o califica sin buscar.

FIRST VIEWPORT: Barra superior (logo, menú). Saludo + sector. Tarjeta principal: «Hoy · turno mañana» + chip «En camino», titular «Faltan 3 paradas antes de la tuya», a la derecha la gota que se llena (signature), debajo la tira de 24 segmentos (atendida llena, antes de ti punteada, tu casa con borde naranja y etiqueta). Tarjeta cisterna (placa AYA-101, 12 000 L). Botón primario «Ver la cisterna en el mapa» (56 px). Barra inferior al pulgar: Inicio, Mapa, Reportar, Mi cuenta (ícono + texto).

FORM: Challenger «warm consumer app surface» (digital-design-canon-warm-consumer-app-surface), elegido por el usuario en la primera mano. Raises: estado por forma, no solo color (emission-line rail); misma tira a la misma escala en todos los estados (botanical folio); placa de identidad de la cisterna (character catalog); paradas críticas con marcador mayor en el mapa (star atlas). Signature: la gota del logo se llena con el avance de la ruta hasta tu parada. seed: d5fd0144e2967391

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
