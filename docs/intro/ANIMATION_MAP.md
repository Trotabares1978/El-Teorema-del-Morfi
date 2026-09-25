# Mapa de construcción de la intro

Este documento define los estados visuales que deben respetarse al animar la ilustración original de **El Teorema del Morfi**.

## Regla principal

La animación es **acumulativa**: cada estado conserva todo lo anterior y agrega solamente la nueva capa. No se deben revelar edificios, camino, Matías, título ni botón mediante barridos, diagonales, rectángulos o fragmentos internos.

## Estados

### 01 — Fondo
Solo:
- cielo
- sol
- mar
- horizonte
- vegetación/campo

No contiene:
- camino
- Astillero
- Pizzería
- Clínica
- Escuela
- Matías
- título/subtítulo
- botón Entrar

### 02 — Camino
Se agrega el camino completo sobre el fondo.
El camino debe aparecer como **una sola pieza continua**, conservando el fondo debajo.

### 03 — Lugares
Se agregan simultáneamente, completos y en sus posiciones originales:
- Astillero
- Pizzería
- Clínica
- Escuela

No debe aparecer todavía Matías, título, subtítulo ni Entrar.

### 04 — Matías
Se agrega **Matías completo**, con su silueta natural y en su posición original.
No se debe construir por partes del cuerpo.

### 05 — Título
Se agregan:
- EL TEOREMA DEL MORFI
- Más que un libro

Ambos permanecen encima de todo lo anterior.

### 06 — Entrar
Último elemento: botón **ENTRAR** completo.
Nada debe aparecer después del botón.

## Implementación

La referencia visual son los estados del storyboard, pero la imagen final que debe verse en pantalla sigue siendo la **ilustración original** del proyecto.

La implementación recomendada es trabajar con capas/máscaras independientes y acumulativas:

`background -> road -> places -> matias -> title/subtitle -> enter`

Cada capa debe poder hacerse visible completa mediante una transición suave (fade/materialización orgánica), sin recorrer su interior con `PathMeasure`.

## Criterio de aceptación

Al detener la animación en cualquier estado, el resultado debe parecer una ilustración que se está completando por capas, no una imagen que está siendo descubierta mediante recortes geométricos.
