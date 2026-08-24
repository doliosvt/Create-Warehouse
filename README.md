# Create: Warehouse

An early-stage Create addon for warehouse logistics and decoration on Minecraft 1.21.1.

The current build includes a functional crate packager and crate package entity, plus registered placeholder blocks for the remaining existing models and textures. Placeholder blocks are intentionally decorative until their mechanics are implemented.

## Gantry elevator

The gantry elevator controller turns an ordinary horizontal Create gantry-shaft line into a station-controlled horizontal lift:

1. Put the controller directly against one end of a continuous horizontal gantry-shaft line. Its facing side is the output; feed rotation into the opposite side.
2. Use a standard Create gantry carriage and attach the structure that should move.
3. Place gantry elevator contacts in the cross-sections where the carriage should stop (up to eight blocks from the shaft) and give the desired contact a redstone pulse.
4. The controller chooses the required shaft direction, stops the carriage at that contact, and emits a short redstone pulse from the reached contact.

The gantry elevator controls block should be attached to the structure carried by the first, horizontal carriage. Press the panel to open its address field and enter a destination such as `12A`: the numeric part selects horizontal contact 12 and the letter selects vertical level A. Levels continue as A, B, ... Z, AA, AB, and each level is two blocks higher than the previous one. Confirm the address to start the automatic horizontal-then-vertical trip. The panel remains usable while the gantry is moving and after it disassembles at a stop.

For two-axis travel, mount the first carriage on the side of the horizontal shaft and configure its kinetic rotation axis vertically. Connect a vertical gantry shaft directly above or below that carriage and place the second carriage on the vertical shaft. On reaching the horizontal contact, the controller powers and locks the horizontal shaft, transfers rotation through the first carriage, and drives the vertical carriage to the selected level. Vertical contacts are not required: level 1 is two blocks above the bottom of the vertical shaft, level 2 is four blocks above it, and each following level adds another two blocks. The maximum level is limited by the vertical shaft length. The controls panel belongs on the first carriage's frame, not on the vertically moving platform.

Only one controller should be attached to a shaft line. The line can be up to 256 shaft blocks long.

## Development

Use Java 21 and run:

```powershell
.\gradlew.bat build
```
