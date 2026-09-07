# Curved180

**Experimental 360-degree panoramic FOV for Minecraft Java 26.2**, designed around ultrawide curved displays and the Iris/Sodium rendering stack.

Curved180 replaces Minecraft's normal world projection with a cylindrical panorama. It extends the existing FOV slider to **30–360 horizontal degrees**, supports Zoomify and normal FOV effects, and blends upright panoramic movement with a narrower view when looking steeply up or down.

The historical mod ID and name are retained even though coverage now reaches 360 degrees. This is a client-side rendering experiment, not an official Minecraft, Iris, Sodium, or Fabulously Optimized project.

**Current release:** `0.4.2-experimental+26.2` · **License:** [GPL v2 only](LICENSE)

[Download releases](https://github.com/Grammercy/curved180/releases) · [Report a problem](https://github.com/Grammercy/curved180/issues)

## Status: read before installing

The mod works by rendering the world multiple times per displayed frame. It can substantially reduce FPS and increase GPU memory use. Shader seams, temporal artifacts, and compatibility problems remain possible. The shader-state fixes are candidate mitigations, not a guarantee of seamless rendering.

The original target was a 3440×1440, 34-inch, 1000R ultrawide monitor. The projection follows the actual window aspect ratio; it does not depend on that exact monitor. It is **not** a physical optical calibration: viewing distance and panel measurements are not modeled.

## Requirements

| Component | Requirement / development version |
| --- | --- |
| Minecraft Java | **26.2** |
| Java | **25 or newer**; built with Java 25 |
| Fabric Loader | >=0.19.3 |
| Fabric API | >=0.152.1 in metadata; built with **0.158.0+26.2** |
| Iris | **1.11.2+mc26.2**, pinned in mod metadata |
| Sodium | **0.9.1+mc26.2**, pinned in mod metadata |
| Graphics backend | OpenGL |
| Optional zoom mod | Zoomify **2.16.1+26.2** was inspected for integration |
| Shader pack used in development | Complementary Reimagined **r5.9** |

The development environment used Fabulously Optimized **14.0.0-beta.7 for 26.2**. Other modpacks may work with the required dependencies, but Simply Optimized and arbitrary mod combinations have not been verified. Iris remains a required dependency even if shaders are switched off. VulkanMod, other Minecraft releases, and other Iris/Sodium versions are not supported by this release.

## Install with Prism Launcher

1. Close Minecraft completely. A running game can lock the mod JAR on Windows.
2. Download `curved180-0.4.2-experimental+26.2.jar` from Releases. Do not install the sources JAR or source ZIP.
3. In Prism, edit your Minecraft **26.2 Fabric** instance and open its Mods page or `minecraft/mods` folder.
4. Remove previous Curved180 JARs from `mods`, keeping a backup outside that folder if desired.
5. Add the new JAR and ensure the dependencies above are installed.
6. Launch the instance. Open **Options → FOV (Horizontal)**.

Only one Curved180 version should be enabled at a time. No server-side installation is required. Server rules about client modifications still apply.

To uninstall, close Minecraft and remove its JAR. Minecraft may reset an out-of-range saved FOV when loading without the mod. Restoring an old JAR is the rollback procedure; no worlds or resource packs are modified by the mod.

## Controls and behavior

### Horizontal FOV

The normal Minecraft FOV slider now represents **horizontal**, rather than vertical, degrees. Its range is 30–360. Saved numeric settings are preserved, but their visual meaning changes. There is no separate settings screen or F8 toggle; old `curved180.properties` files are ignored.

At the horizon, the unpitched cylindrical projection uses the window's aspect ratio. Its vertical field is capped at 80 degrees. At 3440×1440 and 180 horizontal degrees, that corresponds to approximately 66.7 vertical degrees. The pitch blend changes the elevation mapping as described below.

### Zoom and FOV effects

The mod reads the completed camera FOV calculation after vanilla and compatible mod adjustments. Speed, sprint, flight, death/fluid FOV changes, and Zoomify's return-value adjustment feed into the panorama. Minecraft's FOV Effects setting still influences the vanilla calculation. Zoom can reduce the effective FOV below the slider's 30-degree minimum.

**FOV effects may exceed 360 degrees.** For example, a 360-degree setting multiplied by 1.1 produces 396 degrees at the horizon. Directions repeat across the screen because the extra 36 degrees overlaps the full circle. This is intentional; it does not reveal additional unique scenery. Very large angles can be disorienting and are not extensively validated.

Compatibility is designed around modifiers of `Camera.calculateFov`. Mods that replace camera matrices elsewhere may conflict. Spyglass use temporarily follows the ordinary camera path. Zoomify's hand-FOV preference affects the separately rendered hand layer.

### Upright panorama and vertical looking

Wide views normally become awkward when a rigid camera rotation swings the rear half in the opposite direction. Curved180 blends an upright panorama with a narrower camera-relative view:

| Absolute pitch | Behavior at a 360-degree base FOV |
| --- | --- |
| 0–20 degrees | Full 360-degree coverage; upright panorama |
| Above 20 degrees | Smooth horizontal narrowing begins |
| 25–75 degrees | Upright projection gradually blends into camera-relative viewing |
| 85–90 degrees | Horizontal coverage reaches 100 degrees; camera-relative viewing |

Looking back toward the horizon restores the full adjusted FOV. Upward and downward transitions are symmetric. If zoom already produces an FOV below 100 degrees, the transition does not widen it. Upright influence also fades out between 100 and 60 degrees of base FOV so narrow zoom remains conventional.

The thresholds are currently constants in `ProjectionPlan`, not user settings. The slider displays the base setting, not the continuously changing effective coverage.

### Hand, HUD, and debug crosshair

The hand renders once, after world compositing, with ordinary perspective and vanilla item lighting. Shader-pack-specific hand lighting is not applied. The HUD stays outside the panorama. The F3 three-axis direction indicator is deferred until after compositing so it is not stretched or duplicated with the camera views.

World camera bobbing, hurt tilt, and nausea/portal projection distortion are suppressed during captures. Their behavior is separate from speed and zoom FOV adjustments.

## Performance and visual limitations

- At effective FOVs up to 60 degrees, the renderer uses one adaptive camera capture.
- Above 60 degrees, it uses **six overlapping camera directions**, including upward and downward views. Each capture currently uses a full-window-sized texture and repeats world extraction/rendering and Iris processing.
- At 3440×1440, six captures total about 29.7 million color pixels before the shader pack's additional passes, shadows, depth buffers, temporal buffers, and final compositing.
- This release does not include FSR, frame generation, dynamic resolution, shared shadow rendering, or an optimized capture-resolution scheme. It makes no 165 FPS claim.
- More powerful shaders and larger render distances can multiply the cost. A simpler pack, lower shader quality, lower resolution, or lower render distance may help; gains depend on the bottleneck.
- Horizontal structures can curve in a cylindrical panorama. Full 360-degree coverage, uniform motion, and low distortion at every elevation cannot all be preserved simultaneously.
- Screen-space reflections, bloom, clouds, light shafts, temporal anti-aliasing, and other view-dependent effects may disagree between captures. Overlap blending cannot fully correct those disagreements.
- Shaderless rendering has been reported to remove the large vertical bands seen with shaders. Bands that fade after movement suggest temporal/adaptation differences, but individual causes require diagnosis.
- Dimension changes, resource reloads, long sessions, unusual aspect ratios, third-person modes, and unrelated camera/culling mods have not been exhaustively tested.

The mod retains separate Iris pipelines and image histories for each capture direction. It prevents extra-view pipeline creation from resetting Iris's shared animation clock, and shares a specific allowlist of view-independent Complementary float uniforms from the center view. It does **not** share camera matrices or TAA image history. It does **not** change shader-pack settings. Complementary's Regular light-shaft mode was proposed as a mitigation for per-view Scene Aware adaptation; that settings change has not been applied or verified by this release.

## Build from source

The Gradle Wrapper uses **Gradle 9.5.0** and the build uses **Fabric Loom 1.15.4**. Install JDK 25 and set `JAVA_HOME` to it. The first build needs network access to resolve Minecraft and declared dependencies.

Windows:

```powershell
.\gradlew.bat assemble -x test
```

Linux/macOS:

```sh
chmod +x gradlew
./gradlew assemble -x test
```

The installable JAR is written to `build/libs/`. The `-sources.jar` is for developers. To explicitly run tests, use `gradlew.bat test` or `./gradlew test`; `build` also runs tests.

`gradlew.bat runClient` starts an isolated development client under `run/`. To select an existing local test world, use `gradlew.bat runClient "-PsmokeWorld=New World"`. The repository does not include that world, the full modpack, shaders, or their settings. A copied ModernFix development environment previously required `mixin.devenv=false` because of a developer-only hook mismatch; this is not a bundled setting or a general installation requirement.

## What was validated

- The 0.4.0 build passed six automated tests for horizon mapping, ray normalization and coverage across pitch/aspect/FOV combinations, center aim, continuous symmetric narrowing, blend thresholds, and shared environmental values.
- Its isolated Iris/Complementary client loaded a world with the cube-capture compositor.
- **0.4.1 removed the effects cap without running tests or an in-game validation, at the owner's request.**
- **0.4.2 is the GPL-2.0-only publication/package update with the same rendering behavior. It was assembled without tests or launching Minecraft.**

Tests are included for future work, but their presence is not evidence that the current release was tested. Visual comfort during mouse movement and complete removal of shader bands are not confirmed.

## Implementation guide

| File | Responsibility |
| --- | --- |
| `ProjectionPlan.java` | Effective coverage, pitch narrowing, upright weighting, capture FOV, and a CPU reference ray calculation |
| `CameraMixin.java` | Reads adjusted FOV/pitch, applies square perspective capture, culling focal lengths, and capture orientations |
| `GameRendererMixin.java` | Coordinates world captures, camera-state restoration, post-Iris compositing, normal hand rendering, and deferred F3 indicator |
| `CylinderCompositor.java` | Manages six OpenGL textures and fullscreen compositing while saving/restoring touched GL state |
| `cylinder.frag` | Builds the blended ray, chooses overlapping capture faces, and samples the panorama |
| `OptionsMixin.java` | Replaces the original FOV option with range 30–360 while preserving its saved-value codec |
| `PipelineManagerMixin.java` | Uses separate Iris cache keys for capture views and avoids extra-view clock resets |
| `SharedFrameUniforms.java`, `FloatCachedUniformMixin.java` | Shares selected Complementary environmental values within one displayed frame |
| `HandRendererMixin.java`, `HandRendererAccess.java` | Suppresses duplicated Iris hand passes and supports the separate hand layer |

For an unpitched frame, horizontal angle is `(u - 0.5) * horizontalRadians`, and the camera-local cylinder ray is proportional to `(sin(angle), (2v - 1) * verticalTangent, -cos(angle))`. Upright mode changes the world-relative elevation without crossing a pole, transforms that ray into the original camera basis, and blends normalized directions. The center ray remains the aiming direction. The six capture faces are front, left, right, rear, up, and down; a capture tangent of 1.2 provides overlap for face blending.

The renderer depends on Minecraft and Iris internals through required mixins. Updating version strings alone is not a port. Review method descriptors, projection/depth conventions, extraction order, shader state, capture face orientation, resource lifetime, and hand/HUD ordering when upgrading dependencies.

## Reporting issues and contributing

Include the mod version, Minecraft/Fabric/Iris/Sodium versions, shader pack/version/settings, FOV, approximate pitch, resolution, GPU, and whether the problem also happens with shaders disabled. For bands, say whether they fade when stationary. Include a short reproduction and a screenshot if useful. Review logs for private information before posting them.

Useful next work includes smaller capture targets, reducing repeated extraction/shadow work, better temporal-state handling across view changes, configurable pitch transitions, and controlled visual regression scenes. Keep compatibility claims tied to actual testing. Explain validation in pull requests, including anything intentionally not run.

## License and attribution

Copyright (C) 2026 Curved180 contributors.

The mod's own code and documentation in this publication are licensed under **GNU General Public License version 2 only**, SPDX `GPL-2.0-only`; see [LICENSE](LICENSE). This software is provided without warranty. Earlier private experimental artifacts carried MIT notices; this publication does not retroactively change the terms attached to those copies.

The Gradle Wrapper is separate Apache-2.0 build tooling. Other dependencies retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Minecraft binaries, third-party mod/shader binaries, worlds, launch credentials, and game logs are not distributed in this repository or the release source archive.

This project was developed iteratively with AI assistance. Minecraft and Iris source were inspected for compatibility. QuakeProMax was consulted for architecture research; its implementation was not copied.
