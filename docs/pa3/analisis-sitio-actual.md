# Análisis del sitio actual de EPSEL y bases del rediseño

**Curso:** Interacción Hombre Máquina (UTP), PA3.
**Objeto:** sitio público de EPSEL S.A. (`www.epsel.com.pe/sue/`) y su seguimiento de cisternas (DANA, `eps.center`).
**Fecha de la evaluación:** 4 de noviembre de 2026.

El rediseño es una **propuesta académica**. No es el sitio oficial de EPSEL ni está afiliado a la empresa.

---

## 1. Método

| Técnica | Qué mide | Evidencia |
|---|---|---|
| **Análisis automático con axe-core 4.10** | Fallas WCAG 2.0/2.1 A y AA en 7 páginas | [`linea-base/axe/`](linea-base/axe/) |
| **WAVE** (lo pasa el equipo) | Errores, contraste y alertas | `linea-base/wave/` |
| **Pruebas automatizadas propias** | Recorrido con teclado (foco visible, nombre), tamaño de controles, texto pequeño y vista de celular | [`linea-base/auditoria/`](linea-base/auditoria/) |
| **Peso de página** | Datos que descarga un celular | [`linea-base/README.md`](linea-base/README.md#peso-de-la-página) |
| **Evaluación heurística** | Las 10 heurísticas de Nielsen, con severidad de 0 a 4 | Sección 4 de este documento |
| **Inventario de arquitectura de información** | Menú, rótulos y caminos hacia las tareas del vecino | Secciones 2 y 5 |

**Perfil de usuario de referencia:** vecino o vecina de 35 a 60 años de un asentamiento humano de Chiclayo. Usa un celular de gama baja con datos prepago y tiene poca experiencia digital. También consideramos a personas con baja visión, con daltonismo, que usan solo el teclado o lector de pantalla, o mayores con temblor en las manos.

**Tareas principales del vecino** (lo que viene a hacer al sitio):

1. Saber si hoy hay corte o si pasa la cisterna por su zona, y a qué hora.
2. Pagar su recibo o saber dónde pagarlo.
3. Hacer un reclamo.
4. Encontrar una oficina o un teléfono de atención.
5. Consultar información pública (transparencia).

---

## 2. Inventario del sitio actual

### 2.1 Lo que se ve al entrar (portada)

En orden de aparición:

1. **Aviso emergente (modal)** "Central de atención virtual" que tapa la página. Trae la imagen "Ruta en tiempo real · Agua a la vista" y el botón **"Ingresa aquí"**, que es el **único acceso** al seguimiento de cisternas.
2. **Cabecera con foto** de la planta y el título "Empresa Prestadora de Servicios de Saneamiento de Agua Potable y Alcantarillado de Lambayeque S.A.", más tres botones: *Transparencia*, *Intranet* y *Tu gerente te escucha #TeEscucha*.
3. **Barra azul con el logo y seis menús** (2.2).
4. **Carrusel de noticias** que avanza solo.
5. **"Nuestros servicios":** Agua potable, Alcantarillado y Laboratorio.
6. **"Consultas y trámite online":** Oficina virtual, Trámite documentario, Aplicaciones web, Procesos de adquisición, Directivas de denuncias y Denuncias o sugerencias.
7. **Bloque de OTASS** (otra entidad del Estado).
8. **"Entidades relacionadas":** SUNASS, OTASS y gob.pe.
9. **"Centros de atención":** se ve como un bloque grande en blanco.
10. **Carruseles de Comunicados, Orientación y Boletines.** Los comunicados son imágenes con texto; los horarios de abastecimiento también están dentro de imágenes.
11. **Pie azul** con lema, botón *Iniciar sesión* y 15 enlaces.
12. **Cinco cuadritos de colores fijos a la izquierda** (redes sociales), sin texto ni ícono.

### 2.2 Menú principal: 6 menús y 35 opciones (29 sin contar las que repiten el título del menú)

| Menú | Opciones |
|---|---|
| **Nosotros** | Nosotros · Historia · Misión y Visión · Nuestro Compromiso · Valores Empresariales · Alta Dirección · Organización · Área de Cobertura · Información General |
| **Orientación al Cliente** | Orientación al Cliente · Centros de Atención · Centro de Recaudación · Requisitos para Trámite del Servicio · Requisitos para Realizar Reclamo · Procedimiento del Reclamo |
| **Ciclo Operacional** | Ciclo Operacional · Servicio de Alcantarillado · Servicio de Agua Potable · Potabilización del Agua |
| **Gobernanza y Gobernabilidad** | Gobernanza y Gobernabilidad · Sostenibilidad Financiera · Desempeño Global del PMO · Fortalecimiento Institucional · Clima Laboral · Transparencia de Gestión · Calidad de Atención al Cliente · Gestión Social e Institucional |
| **VMA** | VMA · Nosotros VMA · ¿Por qué el control VMA? · Normatividad · ¿Cómo se aplica la Normatividad? |
| **Integridad** | Integridad · Información de integridad · Denuncia actos de corrupción |

Además hay botones arriba (Transparencia, Intranet, #TeEscucha), enlaces en el pie y el portal de transparencia con 5 tarjetas. Contando todo, **hay más de 60 enlaces de navegación**, y varios destinos se repiten en tres lugares.

**Ninguna de las 5 tareas principales del vecino tiene un camino directo:**

| Tarea del vecino | Dónde está hoy | Problema |
|---|---|---|
| ¿Pasa la cisterna? / ¿Hay corte? | Aviso emergente de la portada (cisterna) y carrusel de comunicados en imágenes (cortes) | Si cierra el aviso, ya no hay cómo volver. Los cortes no se pueden buscar ni leer con lector de pantalla |
| Pagar el recibo | *Orientación al Cliente › Centro de Recaudación* | "Recaudación" no es la palabra del vecino |
| Hacer un reclamo | *Orientación al Cliente*, repartido en dos páginas (requisitos y procedimiento) | El rótulo del menú no dice "reclamo" |
| Oficina o teléfono | *Orientación al Cliente › Centros de Atención* | La sección de la portada se ve en blanco |
| Transparencia | Botón de cabecera, menú *Gobernanza*, pie y portal con 5 tarjetas | Tres caminos distintos. "Pública" y "Estándar" no se distinguen. Indicadores da error del servidor |

---

## 3. Resultados automáticos (resumen)

El detalle completo está en [`linea-base/README.md`](linea-base/README.md).

| Página | Reglas WCAG violadas | Elementos con falla | Críticas |
|---|---:|---:|---:|
| Portada | 9 | 100 | 1 |
| Portal de transparencia | 7 | 48 | 1 |
| Rendición de cuentas | 8 | 46 | 2 |
| Indicadores de gestión | 7 | 46 | 1 |
| Servicio de agua potable | 7 | 73 | 1 |
| Centros de atención | 7 | 65 | 1 |
| Seguimiento de cisternas (DANA) | 8 | 77 | 3 |

**Lo más grave que encontramos:**

- **Peso:** la portada descarga **unos 85 MB** y tarda entre 37 y 45 s, por un campo oculto de ASP.NET (`__VIEWSTATE`) de 47 MB.
- **DANA impide el zoom.** Ningún elemento muestra el foco del teclado, y 47 de 114 textos miden menos de 14 px (hasta 9,8 px).
- **El sitio no declara su idioma** en ninguna página de epsel.com.pe, así que el lector de pantalla lo lee con pronunciación inglesa.
- **Faltan textos alternativos:** las imágenes (incluidos los comunicados con horarios) no tienen texto alternativo y de 24 a 53 elementos por página tienen contraste insuficiente.
- **Controles chicos:** entre el 60 % y el 80 % de los controles miden menos de 44×44 px.
- **No hay enlace "saltar al contenido"** en ninguna página.

---

## 4. Evaluación heurística (Nielsen)

Severidad: **0** no es problema · **1** cosmético · **2** menor · **3** mayor · **4** catastrófico, hay que corregirlo sí o sí.

| # | Hallazgo | Heurística | Sev. | Dónde |
|---|---|---|:-:|---|
| H1 | **Error del servidor con la traza de la pila a la vista.** Muestra rutas internas y la versión de .NET, y el usuario no sabe qué hacer | 9. Ayudar a reconocer y recuperarse de errores | 4 | Indicadores de gestión |
| H2 | **Unos 85 MB por visita:** en celular con datos prepago la página casi no carga | 7. Flexibilidad y eficiencia | 4 | Portada |
| H3 | **El seguimiento de cisternas solo se abre desde un aviso emergente.** Al cerrarlo no hay otro camino, y está en otro dominio (`eps.center`) sin marca EPSEL clara | 6. Reconocer antes que recordar · 4. Consistencia | 4 | Portada / DANA |
| H4 | **DANA no responde la pregunta del vecino:** lista 14 cisternas con el mismo horario y no dice cuál le toca ni cuánto falta | 2. Coincidencia con el mundo real · 8. Diseño minimalista | 3 | DANA |
| H5 | **La imagen promocional muestra "llegada en 8 min"**, pero la plataforma no da tiempo de llegada: promete algo que no cumple | 1. Visibilidad del estado · 2. Mundo real | 3 | Aviso de la portada |
| H6 | **Menú organizado por el organigrama**, con jerga: "Gobernanza y Gobernabilidad", "Ciclo Operacional", "VMA", "PMO", "Recaudación" | 2. Coincidencia con el mundo real | 3 | Menú |
| H7 | **35 opciones de menú y más de 60 enlaces**, con destinos repetidos en cabecera, menú, pie y tarjetas | 8. Diseño estético y minimalista | 3 | Todas |
| H8 | **Horarios de corte y de reparto publicados como imagen** dentro de carruseles que avanzan solos | 1. Visibilidad del estado · 6. Reconocer antes que recordar | 3 | Portada |
| H9 | **Cinco cuadritos de colores sin texto ni ícono** que tapan el contenido | 4. Consistencia y estándares · 6. Reconocer | 3 | Todas |
| H10 | **"Intranet" e "Iniciar sesión" destacados al público**, junto a lo que sí sirve al vecino | 8. Minimalista · 5. Prevención de errores | 2 | Cabecera y pie |
| H11 | **Rendición de cuentas:** grupo "SIN NOMBRE", "Sin Periodo" en todas las filas, fechas de 2022 en el año 2020 y un disquete sin texto para descargar | 1. Visibilidad del estado · 4. Consistencia | 2 | Rendición |
| H12 | **"Transparencia Pública" y "Transparencia Estándar"** como opciones hermanas sin explicación | 2. Mundo real · 10. Ayuda y documentación | 2 | Portal de transparencia |
| H13 | **DANA:** el selector "¿Desea registrar de forma anónima?" parece un campo de texto, el botón "Reportar" rosado pálido parece desactivado, y hay datos sin explicar ("SIN ACTIVIDAD · 0.00 km/h", un contador de "13s") | 4. Consistencia · 1. Visibilidad del estado | 2 | DANA |
| H14 | **Texto justificado, gris claro y con títulos en mayúsculas**: cuesta leerlo | 8. Minimalista | 2 | Páginas de contenido |
| H15 | **Errores de ortografía:** "Estandar", "Area", "GESTION", "Como se aplica", "Copyright © 2020 Intranet" | 4. Consistencia y estándares | 1 | Varias |

**Total:** 3 problemas catastróficos, 6 mayores, 5 menores y 1 cosmético.

---

## 5. Lenguaje: del rótulo institucional a la palabra del vecino

| Hoy dice | Qué es | Proponemos |
|---|---|---|
| Orientación al Cliente | Atención, pagos y reclamos | **Atención y reclamos** / **Pagos y trámites** |
| Centro de Recaudación | Dónde pagar el recibo | **Dónde pagar tu recibo** |
| Requisitos para Trámite del Servicio | Nueva conexión, cambio de titular, etc. | **Trámites: nueva conexión y otros** |
| Requisitos + Procedimiento del Reclamo | Cómo reclamar (dos páginas) | **Cómo hacer un reclamo** (una sola página, en pasos) |
| Ciclo Operacional | Cómo llega el agua | **De dónde viene tu agua** |
| Potabilización del Agua | Tratamiento en las plantas | **Cómo limpiamos el agua** |
| Área de Cobertura | Distritos atendidos | **Dónde damos servicio** |
| Gobernanza y Gobernabilidad | Gestión de la empresa | **Transparencia › Cómo se gestiona EPSEL** |
| Desempeño Global del PMO | Cumplimiento del plan de mejora | **Metas y resultados** |
| VMA | Valores Máximos Admisibles (desagüe de negocios e industrias) | **Para empresas: desagüe no doméstico (VMA)** |
| Tu gerente te escucha #TeEscucha | WhatsApp de atención | **Escríbenos por WhatsApp** |
| Intranet / Iniciar Sesión | Acceso del personal | **Acceso del personal** (solo en el pie) |
| Transparencia Pública / Estándar | Portal del Estado y documentos propios | **Información pública** (una entrada, con subsecciones explicadas) |
| Plataforma de monitoreo de cisternas | Seguimiento de la cisterna | **¿Pasa hoy la cisterna?** |

**Reglas de escritura para el rediseño:**
- tuteo, como hace EPSEL en "Tu gerente te escucha";
- verbos de acción en los botones ("Ver", "Pagar", "Reclamar");
- frases de 15 palabras como máximo en las ayudas;
- siglas solo con su explicación al lado;
- mayúscula solo al inicio, nunca títulos en mayúsculas;
- texto alineado a la izquierda, nunca justificado.

---

## 6. Bases del rediseño

### 6.1 Principios

1. **Primero lo que el vecino viene a hacer.** Las tareas del vecino van antes que la información institucional.
2. **Una sola forma de llegar a cada cosa.** Nada de destinos repetidos en cabecera, menú, pie y tarjetas.
3. **Todo es texto real.** Horarios, cortes y comunicados se publican como texto, no como imagen. Ningún contenido avanza solo.
4. **Liviano:** menos de 1 MB por página en la primera carga.
5. **Accesible desde el inicio:**
   - WCAG 2.1 AA, y además objetivos táctiles de 44×44 px;
   - foco siempre visible;
   - zoom permitido;
   - `lang="es"`;
   - enlace para saltar al contenido.
6. **Honesto:** no prometer tiempos de llegada que el sistema no calcula. Decir "faltan 3 paradas antes de la tuya" y no "llega en 8 min".

### 6.2 Menú propuesto: 5 menús + 1 acción destacada

```
[Logo EPSEL]  Agua en tu zona ▾   Pagos y trámites ▾   Atención y reclamos ▾   Información pública ▾   Nosotros ▾   [ ¿Pasa hoy la cisterna? ]
```

| Menú | Opciones (máximo 5 cada uno) |
|---|---|
| **Agua en tu zona** | Cortes y avisos de hoy · ¿Pasa hoy la cisterna? · Horarios de abastecimiento · De dónde viene tu agua · Cómo limpiamos el agua |
| **Pagos y trámites** | Dónde pagar tu recibo · Oficina virtual · Trámites: nueva conexión y otros · Para empresas: desagüe no doméstico (VMA) |
| **Atención y reclamos** | Cómo hacer un reclamo · Oficinas y horarios · Escríbenos por WhatsApp · Denuncias y sugerencias |
| **Información pública** | Metas y resultados · Rendición de cuentas · Pedir información pública · Cómo se gestiona EPSEL · Integridad y denuncias de corrupción |
| **Nosotros** | Quiénes somos · Directivos y organización · Dónde damos servicio · Noticias |

**El menú pasa de 35 opciones a 22, cada destino en un solo lugar.**

- **Pie:** teléfono y WhatsApp de atención, dirección, entidades relacionadas (SUNASS, OTASS), **Acceso del personal** y "Propuesta académica, no es el sitio oficial".
- **Se eliminan:**
  - el aviso emergente;
  - los cuadritos flotantes; las redes sociales pasan al pie, con nombre;
  - los carruseles automáticos;
  - los botones de la cabecera que repiten el menú.

**En celular:** un botón **"Menú"** con ícono y texto (no solo tres rayas), de 48 px de alto. Los menús se abren como lista desplegable (acordeón), y **"¿Pasa hoy la cisterna?"** queda fijo y visible siempre.

**Accesibilidad del menú:**
- `<nav aria-label="Menú principal">` con botones que usan `aria-expanded`;
- se abre con Enter o Espacio y se cierra con Esc, devolviendo el foco al botón;
- nada se abre solo con pasar el mouse;
- foco visible en todos los elementos.

### 6.3 Portada propuesta (orden)

1. **Saltar al contenido** (visible al recibir foco).
2. **Cabecera:** el menú de 6.2.
3. **Respuesta principal:**
   - el título "¿Hay agua hoy en tu zona?";
   - un buscador por distrito o sector, con etiqueta visible;
   - dos botones grandes: **"¿Pasa hoy la cisterna?"** y **"Cortes y avisos de hoy"**.
4. **Avisos de hoy** como texto: zona, motivo y desde/hasta qué hora, con estado por forma, texto y color.
5. **Lo que más se busca:** cuatro accesos (pagar, reclamar, oficinas, trámites) con ícono **y** texto.
6. **Cómo vamos:** tres indicadores en lenguaje simple (por ejemplo, % de entregas de cisterna verificadas por GPS), con enlace a Información pública.
7. **De dónde viene tu agua:** el recorrido Tinajones → Boró → plantas → tu casa, como diagrama accesible con descripción en texto.
8. **Pie.**

### 6.4 Cómo validamos el menú antes de construirlo

**Prueba de árbol (tree test)** con 3 a 5 vecinos del perfil, en papel o en una página simple con solo los rótulos. Se mide si encuentran la opción correcta y al primer intento, con el menú actual y con el propuesto.

1. "Quieres saber si hoy cortan el agua en tu zona."
2. "Quieres pagar tu recibo y no sabes dónde."
3. "Te llegó un recibo muy alto y quieres reclamar."
4. "Quieres saber si la cisterna ya está en camino."
5. "Quieres ver cuánto dinero gastó EPSEL este año."

Es barata, entra en el informe como evidencia de diseño centrado en el usuario y complementa la prueba SUS que haremos sobre las pantallas terminadas.
