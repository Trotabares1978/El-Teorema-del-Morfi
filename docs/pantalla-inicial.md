# Pantalla inicial — especificación visual

## Concepto

La pantalla representa el recorrido de Matías. El camino nace en el horizonte del Río de la Plata y llega hasta el espectador. Los lugares que aparecen a sus costados representan experiencias que Matías ya recorrió. Matías aparece último, delante de todo, invitando a continuar.

## Fondo

- Cielo y Río de la Plata.
- Sol sobre el horizonte.
- Sin edificios urbanos reconocibles en la costa opuesta.
- Ambiente de Ensenada: costa, vegetación, instalaciones portuarias y construcciones bajas.
- Un pequeño muelle de madera aparece donde el camino llega al agua; debe verse claramente como muelle, no como una continuación de tierra.

## Animación por capas

### Fase 1 — dibujo del mundo
- blanco → cielo
- cielo → sol
- sol → río/horizonte
- río → terrenos
- terrenos → camino
- el camino crece desde el horizonte hacia primer plano

### Fase 2 — aparecen los lugares
Los edificios y objetos no se dibujan: aparecen mediante fundido/movimiento muy sutil.

Orden previsto:
1. Astillero + barco
2. Pizzería
3. Clínica
4. Escuela

### Fase 3 — protagonista
Matías aparece último, en primer plano.

### Fase 4 — identidad
- EL TEOREMA DEL MORFI
- Más que un libro
- ENTRAR

## Criterio técnico

La escena se construirá por capas independientes para poder animarlas dentro de Android. No se dependerá de un único video pre-renderizado.

La ilustración completa actual sirve como referencia de composición y estilo. Para la implementación habrá que preparar versiones separadas de fondo, camino, lugares, Matías, textos y botón.
