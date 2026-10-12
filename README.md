# Wellfeed v4 (Fabric client mod, Minecraft Java 1.21.11)

A client mod with a click GUI and these modules:

| Module      | What it does                                                                 | Default key |
|-------------|------------------------------------------------------------------------------|-------------|
| Keystrokes  | Smooth cream WASD / mouse / space display with CPS (always on)               | -           |
| ArmorHUD    | Worn armor with durability bar and percent (always on)                       | -           |
| PotionHUD   | Active effects with level and time left (always on)                          | -           |
| ESP         | **3D** player boxes, fill, names, distance, health bar, tracers, glow        | H           |
| Fullbright  | Everything fully lit                                                         | Z           |
| **Freecam** | Camera leaves your body and flies where you look                             | N           |
| **Cinematic** | Drone cam: the camera flies around you on its own while you play           | B           |
| Fly         | Velocity flight: WASD, Space up, Shift down, adjustable speed                | G           |
| AutoTotem   | Keeps a totem in the offhand from hotbar or inventory                        | J           |

**Right Shift** opens the new v4 menu: category tabs, animated switches, draggable sliders, keybind chips.
Left click toggles a module, right click expands its settings, drag sliders to change numbers. Every key above can be rebound in Options > Controls > Wellfeed.

## What's new in v4

**New menu**
- Centered panel that scales in, with category tabs (HUD, Render, Camera, Movement, Combat), animated
  on/off switches, real draggable sliders, each module's keybind shown as a chip, a slowly shifting
  accent line and drifting cream dust in the background. Remembers your last tab.

**ESP, rebuilt**
- Real 3D wireframe: all 8 corners of the player's box are projected through the camera
  and clipped at the near plane, so boxes stay correct (and don't vanish) when you stand next to someone.
- Everything is eased per frame: players fade in, fade out near max distance, the health bar slides,
  colors blend smoothly red -> yellow -> green by distance.
- Name tags scale with distance and sit on a small pill. Optional translucent fill and tracers.
- Works through Freecam and Cinematic (your own body gets a box too while the camera is detached).

**Freecam (N)**
- WASD flies along your look direction, Space/Shift up and down, sprint key = boost.
- Velocity is eased and integrated per frame, so it is smooth at any FPS.
- Your real player stands still and can't attack or place blocks while it's on. Fly is paused too.
- Leaving the world or dying shuts it down cleanly.

**Cinematic (B), the surprise**
- A self-flying drone camera. Five shots: slow orbit, rising spiral, figure-eight fly-by,
  low hero orbit and high crane. It crossfades between them every ~11 s and the opening shot is random.
- Eased follow, eased look-at, and a faint handheld sway. It pulls in front of walls instead of clipping through them.
- You keep playing normally the whole time. Settings: Speed, and Shot (0 = auto, 1-5 = lock a shot).
- Turning on Freecam turns Cinematic off and the other way round.

## Build

1. Install JDK 21.
2. Generate the Gradle wrapper once (needs Gradle installed): `gradle wrapper`
   - or copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the Fabric example mod.
3. `./gradlew build` -> `build/libs/wellfeed-4.0.0.jar`
4. `./gradlew runClient` to test in a dev client.

## Layout

- `WellfeedClient.java` - entrypoint, keybinds, pre-tick / tick / HUD dispatch
- `module/` - `Module`, `Setting`, `ModuleManager`
- `modules/` - one class per module
- `gui/ClickGuiScreen.java` - the v4 menu
- `mixin/CameraMixin.java` - per-frame camera hook for Freecam / Cinematic, hides the floating hand
- `mixin/MinecraftClientMixin.java` - ESP glow outline
- `mixin/SimpleOptionMixin.java` - Fullbright (overrides gamma while on)
- `util/CameraRig.java` - the detached camera (client-only armor stand as camera entity)
- `util/Render.java` - cream palette, fade, rounded rect, anti-stair-step rotated `line()`

## Adding a module

Extend `Module`, add a `public static final ... INSTANCE`, add settings with
`add(Setting.bool(...))` / `add(Setting.num(...))`, override `onPreTick`, `onTick` and/or
`onHud`, then register it in `ModuleManager.init()`. It appears in the GUI automatically.

## Notes

- v4 was **not compile-tested** (the build servers weren't reachable where it was written; only a
  syntax check was run). If a Yarn name differs in 1.21.11, the compiler error points to the line.
  New in v4, likeliest spots:
  - `Entity.refreshPositionAndAngles(double, double, double, float, float)` (CameraRig)
  - `new RaycastContext(..., Entity)` (CameraRig, cinematic wall pull-in)
  - `ClientPlayerEntity.input` + `new Input()` (CameraRig, Freecam)
  - `DrawContext.getMatrices()` returning `org.joml.Matrix3x2fStack` with `pushMatrix/popMatrix/translate/rotate/scale` (Render.line, ESP name tag)
  - `MinecraftClient.getRenderTickCounter().getTickProgress(false)`, `Entity.getEyePos()`
  - Still from v3: `KeyBinding.Category`, `getCameraPosVec`, `HudRenderCallback`, `getWindow().getHandle()`.
- `CameraMixin` uses `require = 0`, so if `Camera.update` / `isThirdPerson` are renamed the mod still loads;
  the camera then updates at 20 Hz from the tick instead of per frame (choppier, still works).
- Fly, AutoTotem, ESP and Freecam give you an advantage over other players. Public servers almost
  always ban them and many detect them. Use on singleplayer or servers where everyone has agreed.
  Cinematic is camera-only and is fine anywhere.
