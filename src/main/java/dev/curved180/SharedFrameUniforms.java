// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Only view-independent Complementary values are shared; matrices and image history are not. */
public final class SharedFrameUniforms {
    private static final Set<String> ENVIRONMENT = Set.of(
        "isEyeInCave", "inDry", "inRainy", "inSnowy", "starter", "frameTimeSmooth",
        "eyeBrightnessM", "eyeBrightnessM2", "rainFactor", "inNetherWastes",
        "inCrimsonForest", "inWarpedForest", "inBasaltDeltas", "inSoulValley",
        "inPaleGarden", "inSulfurCaves", "maxBlindnessDarkness", "endFlashIntensityM");
    private final Map<String, Float> center = new HashMap<>();

    public void beginFrame() { center.clear(); }
    public float resolve(String name, float value, int view) {
        if (!ENVIRONMENT.contains(name)) return value;
        if (view == 1) { center.put(name, value); return value; }
        return center.getOrDefault(name, value);
    }
}

