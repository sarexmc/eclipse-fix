# DEDsafio Eclipse (Forge 1.20.1)

Mod para eventos tipo DEDsafio: eclipse con luna oscura y corona, oscuridad total, linterna con durabilidad y Herobrine.

## Comandos (necesitan nivel de operador)
| Comando | Qué hace |
|---|---|
| `/eclipse start [segundos]` | Aparece la luna y se acerca al sol (por defecto 120 s). El cielo se va poniendo gris y al llegar al eclipse queda oscurísimo. Pone la hora en mediodía y congela el ciclo día/noche mientras dura. |
| `/eclipse stop [segundos]` | Vuelve la luz poco a poco (por defecto 60 s) y devuelve la regla del ciclo de día. |
| `/eclipse estado` | Muestra el progreso. |
| `/eclipse bunker add <nombre> <radio>` | Marca un búnker (cubo) centrado en ti. |
| `/eclipse bunker addbox <nombre> <desde> <hasta>` | Marca un búnker con dos esquinas. |
| `/eclipse bunker remove <nombre>` / `list` | Quita / lista búnkeres. |
| `/eclipse herobrine spawn` / `clear` | Prueba: invoca / elimina Herobrines. |

## Reglas
- **Mirar el eclipse** (sin nada que tape el cielo) te prende fuego y te daña.
- **Linterna** (`dedsafio_eclipse:linterna`): clic derecho para encender: sale una cuenta regresiva de 2 segundos (2.0 → 0.0) con un contorno blanco que da la vuelta y al terminar se enciende. Otro clic la apaga al instante (o cancela el encendido). Gasta 1 de durabilidad por segundo encendida (1200 = 20 min). Se repara con redstone en el yunque. Receta: vidrio, 3 lingotes de hierro y antorcha de redstone.
- **Herobrine** aparece con la oscuridad total, te mira quieto. Si lo alumbras con la linterna (~0,5 s) desaparece; si pasan **5 s sin alumbrarlo** te persigue y te golpea. No aparece dentro de un búnker y desaparece si tú o él están en uno, o cuando termina el eclipse.

## Compilar
- Con GitHub: sube este repo y mira la pestaña **Actions**; el `.jar` sale en *Artifacts*.
- Local: Java 17 + Gradle 8.1.1 (`gradle build`), jar en `build/libs/`.

## Instalar
El `.jar` va en `mods/` **del servidor y de cada jugador** (Forge 1.20.1).

## Notas técnicas
- El eclipse se forma casi en el cenit (arriba): la luna entra por la línea vertical y termina tapando el sol, quedando un círculo negro con un aro de luz. Sin estrellas ni nubes.
- El cielo se dibuja con geometría (sin texturas), por eso se ve nítido a cualquier resolución.
- La linterna es un modelo 3D (`models/item/linterna*.json`). Si en tu mano se ve girada, ajusta los bloques `display` del JSON (o ábrela en Blockbench).
- El mod incluye `data/minecraft/dimension_type/overworld.json` para enganchar el cielo del eclipse al Overworld.
- Todos los jugadores ven las linternas encendidas de los demás: cada linterna abre un charco de luz en la oscuridad donde apunta su haz.
- La oscuridad y el haz de la linterna son una capa dibujada en el mundo (no iluminación real de bloques); tu mano y el HUD no se oscurecen.
