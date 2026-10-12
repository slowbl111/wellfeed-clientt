# Wellfeed (Fabric client mod, Minecraft Java 1.21.11)

A client mod with a click GUI and these modules:

| Module      | What it does                                                       | Default key |
|-------------|--------------------------------------------------------------------|-------------|
| Keystrokes  | Smooth cream WASD / mouse / space display with CPS (always on)     | -           |
| ArmorHUD    | Worn armor with durability bar and percent (always on)             | -           |
| PotionHUD   | Active effects with level and time left (always on)                | -           |
| ESP         | Player names + distance in meters, boxes, glow through walls       | H           |
| Fullbright  | Everything fully lit                                               | B           |
| Fly         | Velocity flight: WASD, Space up, Shift down, adjustable speed      | G           |
| AutoTotem   | Keeps a totem in the offhand from hotbar or inventory              | J           |

**Right Shift** opens the click GUI. Left click toggles a module, right click
expands its settings. Every key above can be rebound in
Options > Controls > Wellfeed.

## Build

1. Install JDK 21.
2. Generate the Gradle wrapper once (needs Gradle installed): `gradle wrapper`
   - or copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the Fabric example mod.
3. `./gradlew build` -> jar in `build/libs/`
4. `./gradlew runClient` to test in a dev client.

## Layout

- `WellfeedClient.java` - entrypoint, keybinds, tick + HUD dispatch
- `module/` - `Module`, `Setting`, `ModuleManager`
- `modules/` - one class per module
- `gui/ClickGuiScreen.java` - click GUI
- `mixin/MinecraftClientMixin.java` - ESP glow outline
- `mixin/SimpleOptionMixin.java` - Fullbright (overrides gamma while on)
- `util/Render.java` - cream palette and small draw helpers

## Adding a module

Extend `Module`, add a `public static final ... INSTANCE`, add settings with
`add(Setting.bool(...))` / `add(Setting.num(...))`, override `onTick` and/or
`onHud`, then register it in `ModuleManager.init()`. It appears in the GUI
automatically.

## Notes

- Versions in `gradle.properties` were current on 2026-10-12.
- Not compile-tested. If a Yarn name differs in 1.21.11, the compiler error
  points to the line. Likeliest spots: `KeyBinding.Category`, `getTickProgress`,
  `drawBorder`, `getCameraPosVec`, `HudRenderCallback` (Fabric API marks it
  deprecated in favour of `HudElementRegistry`), `getWindow().getHandle()`.
- Fly, AutoTotem and ESP give you an advantage over other players. Public
  servers almost always ban them and many detect them. Use on singleplayer or
  servers where everyone has agreed.
